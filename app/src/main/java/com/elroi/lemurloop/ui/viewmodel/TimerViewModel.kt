package com.elroi.lemurloop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elroi.lemurloop.domain.repository.TimerRepository
import com.elroi.lemurloop.domain.scheduler.TimerScheduler
import com.elroi.lemurloop.domain.timer.CountdownTimer
import com.elroi.lemurloop.domain.timer.TimerCommandResult
import com.elroi.lemurloop.domain.timer.TimerListController
import com.elroi.lemurloop.domain.timer.TimerOrdering
import com.elroi.lemurloop.domain.timer.TimerRingControls
import com.elroi.lemurloop.domain.timer.TimerRingStop
import com.elroi.lemurloop.domain.timer.TimerVoiceCueKind
import com.elroi.lemurloop.domain.timer.TimerVoiceCueCoordinatorPort
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimerListUiState(
    val timers: List<CountdownTimer> = emptyList(),
    val nowEpochMillis: Long = 0L,
    val ready: Boolean = false,
    val limitReached: Boolean = false,
    val undoVisible: Boolean = false
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val repository: TimerRepository,
    scheduler: TimerScheduler,
    ringStop: TimerRingStop,
    private val voiceCues: TimerVoiceCueCoordinatorPort,
    private val ringControls: TimerRingControls
) : ViewModel() {
    private val controller = TimerListController(
        repository = repository,
        scheduler = scheduler,
        ringStop = ringStop,
        voiceCues = voiceCues,
        now = { System.currentTimeMillis() }
    )
    private val timers = MutableStateFlow<List<CountdownTimer>>(emptyList())
    private val now = MutableStateFlow(System.currentTimeMillis())
    private val ready = MutableStateFlow(false)
    private val limitReached = MutableStateFlow(false)
    private val undoVisible = MutableStateFlow(false)

    val state = combine(timers, now, ready, limitReached, undoVisible) { items, time, isReady, limited, undo ->
        TimerListUiState(items, time, isReady, limited, undo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerListUiState())

    init {
        viewModelScope.launch {
            repository.observeAll().collect { items ->
                timers.value = TimerOrdering.newestFirst(items)
                ready.value = true
            }
        }
        viewModelScope.launch {
            while (true) {
                val time = System.currentTimeMillis()
                now.value = time
                undoVisible.value = controller.undoAvailable(time)
                delay(250)
            }
        }
    }

    fun start(durationMillis: Long, label: String) = run { controller.start(durationMillis, label) }
    fun pause(id: Long) = run { controller.pause(id) }
    fun play(id: Long) = run { controller.play(id) }
    fun addOneMinute(id: Long) = run { controller.addOneMinute(id) }
    fun refresh(id: Long) = run { controller.refresh(id) }
    fun rename(id: Long, label: String) = run { controller.rename(id, label) }
    fun duplicate(id: Long) = run { controller.duplicate(id) }
    fun delete(id: Long) = run { controller.delete(id) }
    fun undo() = run { controller.undo() }

    fun setVoiceCueStartOverride(id: Long, value: Boolean?) =
        run { controller.setVoiceCueOverride(id, TimerVoiceCueKind.Start, value) }

    fun setVoiceCueMidRemainingOverride(id: Long, value: Boolean?) =
        run { controller.setVoiceCueOverride(id, TimerVoiceCueKind.MidRemaining, value) }

    fun setVoiceCueLastTenOverride(id: Long, value: Boolean?) =
        run { controller.setVoiceCueOverride(id, TimerVoiceCueKind.EnterLastTen, value) }

    fun dismissRinging() {
        ringControls.dismiss()
    }

    fun clearLimitMessage() {
        limitReached.value = false
    }

    override fun onCleared() {
        controller.clearUndo()
        super.onCleared()
    }

    private fun run(block: suspend () -> TimerCommandResult) {
        viewModelScope.launch {
            limitReached.value = block() == TimerCommandResult.LimitReached
            undoVisible.value = controller.undoAvailable(System.currentTimeMillis())
        }
    }
}
