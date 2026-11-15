package com.elroi.lemurloop.data.repository

import com.elroi.lemurloop.data.local.AppDatabase
import com.elroi.lemurloop.domain.manager.SettingsManager
import com.elroi.lemurloop.domain.repository.AppDataRepository
import javax.inject.Inject

class AppDataRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val settingsManager: SettingsManager,
    private val timerDataWiper: TimerDataWiper
) : AppDataRepository {

    override suspend fun wipeAll() {
        timerDataWiper.wipe()
        appDatabase.alarmDao().deleteAllAlarms()
        appDatabase.sleepRecordDao().deleteAllRecords()
        settingsManager.clearAll()
    }
}

