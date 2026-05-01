package com.elroi.lemurloop.domain.creation

import com.elroi.lemurloop.domain.model.Alarm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nav-scoped alarm creation draft shared across Chat, Wizard, and Detailed flows (in-memory).
 * Chat transcripts stay in the chat ViewModel; only the alarm draft lives here.
 */
@Singleton
class AlarmCreationSessionStore @Inject constructor() {
    private val _draft = MutableStateFlow<Alarm?>(null)
    val draft: StateFlow<Alarm?> = _draft.asStateFlow()

    /** After first persist from chat (inactive or active). Used to replace the same row on later saves. */
    private val _persistedAlarmId = MutableStateFlow<String?>(null)
    val persistedAlarmId: StateFlow<String?> = _persistedAlarmId.asStateFlow()

    fun updateDraft(alarm: Alarm) {
        _draft.value = alarm
    }

    fun markPersisted(alarmId: String) {
        _persistedAlarmId.value = alarmId
    }

    /** Called when starting a new alarm from the FAB hub (before picking Chat / Wizard / Detailed). */
    fun resetSession() {
        _draft.value = null
        _persistedAlarmId.value = null
    }

    fun clearDraftOnly() {
        _draft.value = null
    }
}
