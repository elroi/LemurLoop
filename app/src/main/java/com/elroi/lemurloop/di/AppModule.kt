package com.elroi.lemurloop.di

import android.app.Application
import androidx.room.Room
import com.elroi.lemurloop.data.local.AppDatabase
import com.elroi.lemurloop.data.local.dao.AlarmDao
import com.elroi.lemurloop.data.local.dao.DiagnosticLogDao
import com.elroi.lemurloop.data.repository.AlarmRepositoryImpl
import com.elroi.lemurloop.data.repository.AppDataRepositoryImpl
import com.elroi.lemurloop.data.repository.DiagnosticLogRepositoryImpl
import com.elroi.lemurloop.data.repository.timerSettingsDataStore
import com.elroi.lemurloop.domain.repository.AlarmRepository
import com.elroi.lemurloop.domain.repository.AppDataRepository
import com.elroi.lemurloop.domain.repository.DiagnosticLogRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "alarm_pal_db"
        )
            .addMigrations(
                com.elroi.lemurloop.data.local.MIGRATION_6_7,
                com.elroi.lemurloop.data.local.MIGRATION_7_8,
                com.elroi.lemurloop.data.local.MIGRATION_8_9,
                com.elroi.lemurloop.data.local.MIGRATION_9_10,
                com.elroi.lemurloop.data.local.MIGRATION_10_11,
                com.elroi.lemurloop.data.local.MIGRATION_11_12,
                com.elroi.lemurloop.data.local.MIGRATION_12_13,
                com.elroi.lemurloop.data.local.MIGRATION_13_14,
                com.elroi.lemurloop.data.local.MIGRATION_14_15,
                com.elroi.lemurloop.data.local.MIGRATION_15_16,
                com.elroi.lemurloop.data.local.MIGRATION_16_17,
                com.elroi.lemurloop.data.local.MIGRATION_17_18,
                com.elroi.lemurloop.data.local.MIGRATION_18_19,
                com.elroi.lemurloop.data.local.MIGRATION_19_20,
                com.elroi.lemurloop.data.local.MIGRATION_20_21,
                com.elroi.lemurloop.data.local.MIGRATION_21_22,
                com.elroi.lemurloop.data.local.MIGRATION_22_23,
                com.elroi.lemurloop.data.local.MIGRATION_23_24,
                com.elroi.lemurloop.data.local.MIGRATION_24_25,
                com.elroi.lemurloop.data.local.MIGRATION_25_26
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideAlarmDao(db: AppDatabase): AlarmDao {
        return db.alarmDao()
    }

    @Provides
    @Singleton
    fun provideDiagnosticLogDao(db: AppDatabase): DiagnosticLogDao {
        return db.diagnosticLogDao()
    }

    @Provides
    @Singleton
    fun provideAlarmRepository(dao: AlarmDao): AlarmRepository {
        return AlarmRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideDiagnosticLogRepository(
        diagnosticLogDao: DiagnosticLogDao
    ): DiagnosticLogRepository {
        return DiagnosticLogRepositoryImpl(diagnosticLogDao)
    }

    @Provides
    @Singleton
    fun provideTimerDao(db: AppDatabase): com.elroi.lemurloop.data.local.dao.TimerDao {
        return db.timerDao()
    }

    @Provides
    @Singleton
    fun provideTimerRepository(
        dao: com.elroi.lemurloop.data.local.dao.TimerDao
    ): com.elroi.lemurloop.domain.repository.TimerRepository {
        return com.elroi.lemurloop.data.repository.TimerRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideTimerSettingsRepository(
        app: Application
    ): com.elroi.lemurloop.domain.repository.TimerSettingsRepository {
        return com.elroi.lemurloop.data.repository.TimerSettingsRepositoryImpl(
            app.timerSettingsDataStore
        )
    }

    @Provides
    @Singleton
    fun provideTimerScheduler(
        app: Application
    ): com.elroi.lemurloop.domain.scheduler.TimerScheduler {
        return com.elroi.lemurloop.data.scheduler.AndroidTimerScheduler(app)
    }

    @Provides
    @Singleton
    fun provideTimerBootReconciler(
        repository: com.elroi.lemurloop.domain.repository.TimerRepository,
        scheduler: com.elroi.lemurloop.domain.scheduler.TimerScheduler,
        notifier: com.elroi.lemurloop.domain.timer.TimerFinishedNotifier
    ): com.elroi.lemurloop.domain.timer.TimerBootReconciler {
        return com.elroi.lemurloop.domain.timer.TimerBootReconciler(
            repository = repository,
            scheduler = scheduler,
            notifier = notifier,
            ringRunning = { com.elroi.lemurloop.domain.timer.TimerRingSession.isRunning() }
        )
    }

    @Provides
    @Singleton
    fun provideTimerFinishedNotifier(
        app: Application
    ): com.elroi.lemurloop.domain.timer.TimerFinishedNotifier {
        return com.elroi.lemurloop.data.timer.AndroidTimerFinishedNotifier(app)
    }

    @Provides
    @Singleton
    fun provideTimerRingStop(
        app: Application
    ): com.elroi.lemurloop.domain.timer.TimerRingStop {
        return com.elroi.lemurloop.data.timer.AndroidTimerRingStop(app)
    }

    @Provides
    @Singleton
    fun provideTimerRingControls(
        app: Application
    ): com.elroi.lemurloop.domain.timer.TimerRingControls {
        return com.elroi.lemurloop.data.timer.AndroidTimerRingControls(app)
    }

    @Provides
    @Singleton
    fun provideTimerTrackGrantReleaser(
        app: Application
    ): com.elroi.lemurloop.domain.timer.TimerTrackGrantReleaser {
        return com.elroi.lemurloop.data.timer.AndroidTimerTrackGrantReleaser(app)
    }

    @Provides
    @Singleton
    fun provideTimerDataWiper(
        timerRepository: com.elroi.lemurloop.domain.repository.TimerRepository,
        timerSettingsRepository: com.elroi.lemurloop.domain.repository.TimerSettingsRepository,
        timerScheduler: com.elroi.lemurloop.domain.scheduler.TimerScheduler,
        timerRingStop: com.elroi.lemurloop.domain.timer.TimerRingStop,
        timerTrackGrantReleaser: com.elroi.lemurloop.domain.timer.TimerTrackGrantReleaser
    ): com.elroi.lemurloop.data.repository.TimerDataWiper {
        return com.elroi.lemurloop.data.repository.TimerDataWiper(
            timerRepository = timerRepository,
            timerSettingsRepository = timerSettingsRepository,
            timerScheduler = timerScheduler,
            timerRingStop = timerRingStop,
            timerTrackGrantReleaser = timerTrackGrantReleaser
        )
    }

    @Provides
    @Singleton
    fun provideAppDataRepository(
        appDatabase: AppDatabase,
        settingsManager: com.elroi.lemurloop.domain.manager.SettingsManager,
        timerDataWiper: com.elroi.lemurloop.data.repository.TimerDataWiper
    ): AppDataRepository {
        return AppDataRepositoryImpl(appDatabase, settingsManager, timerDataWiper)
    }

    @Provides
    @Singleton
    fun provideAlarmScheduler(
        app: Application,
        briefingGenerator: com.elroi.lemurloop.domain.generator.BriefingGenerator
    ): com.elroi.lemurloop.domain.scheduler.AlarmScheduler {
        return com.elroi.lemurloop.data.scheduler.AndroidAlarmScheduler(app, briefingGenerator)
    }

    @Provides
    @Singleton
    fun provideDemoAlarmSeeder(
        alarmRepository: AlarmRepository,
        alarmScheduler: com.elroi.lemurloop.domain.scheduler.AlarmScheduler
    ): com.elroi.lemurloop.domain.manager.DemoAlarmSeeder {
        return com.elroi.lemurloop.domain.manager.DemoAlarmSeeder(alarmRepository, alarmScheduler)
    }

    @Provides
    @Singleton
    fun provideTtsEngine(
        manager: com.elroi.lemurloop.domain.manager.TextToSpeechManager
    ): com.elroi.lemurloop.domain.manager.TtsEngine = manager

    @Provides
    @Singleton
    fun provideGoogleCloudTtsEngine(
        app: Application,
        settingsManager: com.elroi.lemurloop.domain.manager.SettingsManager
    ): com.elroi.lemurloop.data.local.GoogleCloudTtsEngine {
        return com.elroi.lemurloop.data.local.GoogleCloudTtsEngine(app, settingsManager)
    }
}
