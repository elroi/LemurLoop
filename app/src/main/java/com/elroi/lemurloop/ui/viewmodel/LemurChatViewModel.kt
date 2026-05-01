package com.elroi.lemurloop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elroi.lemurloop.domain.buddy.AlarmBuddyLifecycleNotifier
import com.elroi.lemurloop.domain.chat.AlarmDraftFactory
import com.elroi.lemurloop.domain.chat.AlarmProposalPartial
import com.elroi.lemurloop.domain.chat.LemurChatParser
import com.elroi.lemurloop.domain.chat.LemurChatPrompts
import com.elroi.lemurloop.domain.creation.AlarmCreationSessionStore
import com.elroi.lemurloop.domain.manager.AlarmDefaults
import com.elroi.lemurloop.domain.manager.GeminiManager
import com.elroi.lemurloop.domain.manager.SettingsManager
import com.elroi.lemurloop.domain.model.Alarm
import com.elroi.lemurloop.domain.repository.AlarmRepository
import com.elroi.lemurloop.domain.scheduler.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LemurChatApiError { NO_API_KEY, NETWORK, UNKNOWN }

data class ChatMessageUi(
    val id: String,
    val isUser: Boolean,
    val text: String,
    /** While assistant message is streaming */
    val isStreaming: Boolean = false
)

data class LemurChatUiState(
    val messages: List<ChatMessageUi> = emptyList(),
    val draftAlarm: Alarm? = null,
    val persistedAlarmId: String? = null,
    val defaults: AlarmDefaults = AlarmDefaults(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val streamingAssistantId: String? = null,
    val geminiKeyPresent: Boolean = true,
    val apiErrorDialog: LemurChatApiError? = null
)

@HiltViewModel
class LemurChatViewModel @Inject constructor(
    private val geminiManager: GeminiManager,
    private val settingsManager: SettingsManager,
    private val creationSession: AlarmCreationSessionStore,
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
    private val buddyLifecycleNotifier: AlarmBuddyLifecycleNotifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(LemurChatUiState())
    val uiState: StateFlow<LemurChatUiState> = _uiState

    private val defaultsFlow = settingsManager.alarmDefaultsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlarmDefaults())

    private var throttleJob: Job? = null

    init {
        viewModelScope.launch {
            settingsManager.geminiApiKeyFlow.collect { key ->
                _uiState.update { it.copy(geminiKeyPresent = key.trim().isNotBlank()) }
            }
        }
        viewModelScope.launch {
            creationSession.draft.collect { draft ->
                _uiState.update { it.copy(draftAlarm = draft) }
            }
        }
        viewModelScope.launch {
            creationSession.persistedAlarmId.collect { id ->
                _uiState.update { it.copy(persistedAlarmId = id) }
            }
        }
        viewModelScope.launch {
            defaultsFlow.collect { d ->
                _uiState.update { it.copy(defaults = d) }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun dismissApiError() {
        _uiState.update { it.copy(apiErrorDialog = null) }
    }

    fun sendUserMessage(contextAssistantName: String, missingKeyMessage: String) {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isSending) return
        val keyOk = _uiState.value.geminiKeyPresent
        if (!keyOk) {
            val userMsg = ChatMessageUi(id = newId(), isUser = true, text = text)
            val assistantMsg = ChatMessageUi(id = newId(), isUser = false, text = missingKeyMessage)
            _uiState.update {
                it.copy(
                    messages = it.messages + userMsg + assistantMsg,
                    inputText = ""
                )
            }
            return
        }

        val priorHistory = buildPriorHistory()
        val userMsg = ChatMessageUi(id = newId(), isUser = true, text = text)
        val assistantId = newId()
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg + ChatMessageUi(
                    id = assistantId,
                    isUser = false,
                    text = "",
                    isStreaming = true
                ),
                inputText = "",
                isSending = true,
                streamingAssistantId = assistantId
            )
        }

        viewModelScope.launch {
            val defaults = defaultsFlow.first()
            val prompt = LemurChatPrompts.buildStreamingPrompt(
                assistantName = contextAssistantName,
                historyTurns = priorHistory,
                userMessage = userMsg.text,
                repairHint = null
            )

            try {
                var accumulated = ""
                var anyChunk = false
                geminiManager.generateContentStreaming(prompt).collect { fullSoFar ->
                    anyChunk = true
                    accumulated = fullSoFar
                    throttleUiUpdate(assistantId, accumulated)
                }
                throttleJob?.join()
                if (!anyChunk) {
                    replaceAssistantMessage(
                        assistantId,
                        "",
                        streaming = false
                    )
                    _uiState.update {
                        it.copy(isSending = false, streamingAssistantId = null, apiErrorDialog = LemurChatApiError.UNKNOWN)
                    }
                    return@launch
                }
                finalizeAssistantTurn(assistantId, accumulated, defaults, repairAttempt = 0)
            } catch (e: Exception) {
                android.util.Log.e("LemurChatViewModel", "Chat turn failed", e)
                removeStreamingPlaceholder(assistantId)
                _uiState.update {
                    it.copy(
                        isSending = false,
                        streamingAssistantId = null,
                        apiErrorDialog = LemurChatApiError.NETWORK
                    )
                }
            }
        }
    }

    private suspend fun finalizeAssistantTurn(
        assistantId: String,
        accumulated: String,
        defaults: AlarmDefaults,
        repairAttempt: Int
    ) {
        val needsRepair = repairAttempt == 0 &&
            !accumulated.contains(LemurChatPrompts.JSON_MARKER)

        if (needsRepair) {
            val repaired = runRepairTurn(accumulated, "Missing <<<JSON>>> section or invalid JSON tail.")
            if (repaired != null) {
                finalizeAssistantTurn(assistantId, repaired, defaults, repairAttempt = 1)
                return
            }
        }

        val parsed = LemurChatParser.parseModelOutput(accumulated)
        var proposal = parsed.proposal

        if (proposal != null) {
            proposal = normalizeProposalWithPrevious(proposal)
        }

        val cleanText = parsed.message.ifBlank { LemurChatParser.visibleStreamingPrefix(accumulated) }
        replaceAssistantMessage(assistantId, cleanText, streaming = false)

        if (proposal != null) {
            val merged = AlarmDraftFactory.mergeProposal(
                defaults,
                creationSession.draft.value,
                proposal
            )
            creationSession.updateDraft(merged)
        }

        _uiState.update { it.copy(isSending = false, streamingAssistantId = null) }
    }

    private fun normalizeProposalWithPrevious(incoming: AlarmProposalPartial): AlarmProposalPartial {
        val prev = creationSession.draft.value ?: return incoming
        val days = when {
            incoming.daysOfWeek.isNotEmpty() -> incoming.daysOfWeek
            else -> prev.daysOfWeek
        }
        val label = incoming.label ?: prev.label
        return AlarmProposalPartial(
            hour = incoming.hour,
            minute = incoming.minute,
            label = label,
            daysOfWeek = days
        )
    }

    private suspend fun runRepairTurn(raw: String, hint: String): String? {
        return try {
            val assistantName = "Lemur"
            val prompt = LemurChatPrompts.buildRepairPrompt(assistantName, raw, hint)
            geminiManager.generateContent(prompt)?.takeIf { !it.startsWith("ERROR:") }
        } catch (_: Exception) {
            null
        }
    }

    private fun throttleUiUpdate(assistantId: String, accumulated: String) {
        throttleJob?.cancel()
        throttleJob = viewModelScope.launch {
            delay(75)
            val visible = LemurChatParser.visibleStreamingPrefix(accumulated)
            replaceAssistantMessage(assistantId, visible, streaming = true)
        }
    }

    private fun replaceAssistantMessage(id: String, text: String, streaming: Boolean) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { m ->
                    if (m.id == id) m.copy(text = text, isStreaming = streaming) else m
                }
            )
        }
    }

    private fun removeStreamingPlaceholder(id: String) {
        _uiState.update { state ->
            state.copy(messages = state.messages.filterNot { it.id == id && it.isStreaming })
        }
    }

    private fun buildPriorHistory(): List<Pair<String, String>> {
        val msgs = _uiState.value.messages
        val turns = mutableListOf<Pair<String, String>>()
        var pendingUser: String? = null
        for (m in msgs) {
            if (m.isUser) {
                pendingUser = m.text
            } else if (pendingUser != null) {
                turns.add("user" to pendingUser!!)
                turns.add("assistant" to m.text)
                pendingUser = null
            }
        }
        return turns
    }

    fun persistAlarmInactive(): Boolean {
        val draft = _uiState.value.draftAlarm ?: return false
        viewModelScope.launch {
            val previous = alarmRepository.getAlarmById(draft.id)
            val toSave = draft.copy(isEnabled = false)
            alarmRepository.insertAlarm(toSave)
            buddyLifecycleNotifier.onAlarmSaved(previous, toSave)
            creationSession.markPersisted(toSave.id)
        }
        return true
    }

    fun persistAlarmActive(): Boolean {
        val draft = _uiState.value.draftAlarm ?: return false
        viewModelScope.launch {
            val previous = alarmRepository.getAlarmById(draft.id)
            val toSave = draft.copy(isEnabled = true)
            alarmRepository.insertAlarm(toSave)
            if (toSave.isEnabled) {
                alarmScheduler.schedule(toSave)
            }
            buddyLifecycleNotifier.onAlarmSaved(previous, toSave)
            creationSession.markPersisted(toSave.id)
        }
        return true
    }

    private fun newId() = java.util.UUID.randomUUID().toString()
}
