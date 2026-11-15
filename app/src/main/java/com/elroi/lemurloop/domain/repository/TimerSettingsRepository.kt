package com.elroi.lemurloop.domain.repository

import com.elroi.lemurloop.domain.timer.TimerSettings
import kotlinx.coroutines.flow.Flow

interface TimerSettingsRepository {
    suspend fun get(): TimerSettings
    fun observe(): Flow<TimerSettings>
    suspend fun update(settings: TimerSettings)
    suspend fun clear()
}
