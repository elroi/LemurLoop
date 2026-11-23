package com.elroi.lemurloop.ui.viewmodel

import com.elroi.lemurloop.domain.repository.TimerSettingsRepository
import com.elroi.lemurloop.domain.timer.TimerSettings
import com.elroi.lemurloop.domain.timer.TimerTrackGrantReleaser
import com.elroi.lemurloop.domain.timer.TimerVoiceCueCoordinatorPort
import com.elroi.lemurloop.domain.timer.TimerVoiceCueRebuildReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerSettingsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setVolume_nonCueWrite_doesNotRebuildVoiceCues() = runTest {
        val repository = MemorySettings()
        val voiceCues = RecordingVoiceCues()
        val viewModel = viewModel(repository, voiceCues)

        viewModel.setVolume(35)

        assertEquals(35, repository.current.volumePercent)
        assertEquals(0, voiceCues.globalChanges)
    }

    @Test
    fun setVoiceCueStartEnabled_voiceWrite_rebuildsVoiceCues() = runTest {
        val repository = MemorySettings()
        val voiceCues = RecordingVoiceCues()
        val viewModel = viewModel(repository, voiceCues)

        viewModel.setVoiceCueStartEnabled(false)

        assertEquals(false, repository.current.voiceCueStartEnabled)
        assertEquals(1, voiceCues.globalChanges)
    }

    private fun viewModel(
        repository: TimerSettingsRepository,
        voiceCues: TimerVoiceCueCoordinatorPort
    ) = TimerSettingsViewModel(
        repository = repository,
        grants = TimerTrackGrantReleaser { },
        voiceCues = voiceCues
    )

    private class MemorySettings : TimerSettingsRepository {
        private val flow = MutableStateFlow(TimerSettings())
        val current: TimerSettings get() = flow.value
        override suspend fun get(): TimerSettings = current
        override fun observe(): Flow<TimerSettings> = flow
        override suspend fun update(settings: TimerSettings) {
            flow.value = settings
        }
        override suspend fun clear() {
            flow.value = TimerSettings()
        }
    }

    private class RecordingVoiceCues : TimerVoiceCueCoordinatorPort {
        var globalChanges = 0
        override fun onTimerChanged(timerId: Long, reason: TimerVoiceCueRebuildReason) = Unit
        override fun onVoiceGlobalsChanged() {
            globalChanges += 1
        }
        override fun cancel(timerId: Long) = Unit
        override fun cancelAll() = Unit
    }
}
