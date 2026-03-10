package com.elroi.lemurloop.domain.manager

import com.elroi.lemurloop.domain.model.Alarm
import com.elroi.lemurloop.domain.repository.AlarmRepository
import com.elroi.lemurloop.domain.scheduler.AlarmScheduler
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import javax.inject.Inject

class DemoAlarmSeeder @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {

    /**
     * Seeds a small, rich set of demo alarms.
     *
     * This is additive: it never deletes existing alarms, and it skips inserting
     * demo alarms that match an existing alarm with the same label, time, and daysOfWeek.
     *
     * @return the number of demo alarms that were inserted.
     */
    suspend fun seedDemoAlarms(): Int {
        val existing = alarmRepository.getAllAlarms().first()
        val existingKeys = existing.map { it.demoKey() }.toMutableSet()

        val demoAlarms = buildDemoAlarms()

        var inserted = 0
        for (demo in demoAlarms) {
            val key = demo.demoKey()
            if (existingKeys.add(key)) {
                alarmRepository.insertAlarm(demo)
                alarmScheduler.schedule(demo)
                inserted++
            }
        }
        return inserted
    }

    private fun buildDemoAlarms(): List<Alarm> {
        return listOf(
            // Classic weekday alarm showcasing full briefing + gentle wake
            Alarm(
                time = LocalTime.of(7, 0),
                label = "Weekday Wake-Up",
                daysOfWeek = listOf(1, 2, 3, 4, 5), // Mon–Fri
                isGentleWake = true,
                crescendoDurationMinutes = 5,
                isSmoothFadeOut = true,
                isBriefingEnabled = true,
                isTtsEnabled = true,
                briefingTimeoutSeconds = 45,
                snoozeDurationMinutes = 5,
                isSoundEnabled = true,
                isSnoozeEnabled = true
            ),
            // Early gym mission with math and evasive snooze
            Alarm(
                time = LocalTime.of(6, 0),
                label = "Gym Mission",
                daysOfWeek = listOf(1, 3, 5), // Mon, Wed, Fri
                mathDifficulty = 2, // Medium
                mathProblemCount = 3,
                mathGraduallyIncreaseDifficulty = true,
                isEvasiveSnooze = true,
                evasiveSnoozesBeforeMoving = 1,
                snoozeDurationMinutes = 3,
                isSoundEnabled = true,
                isSnoozeEnabled = true
            ),
            // Weekend sleep-in with extra-gentle wakeup, no briefing
            Alarm(
                time = LocalTime.of(9, 30),
                label = "Weekend Sleep-In",
                daysOfWeek = listOf(6, 7), // Sat, Sun
                isGentleWake = true,
                crescendoDurationMinutes = 12,
                isSmoothFadeOut = true,
                isBriefingEnabled = false,
                isTtsEnabled = false,
                snoozeDurationMinutes = 15,
                isSoundEnabled = true,
                isSnoozeEnabled = true
            ),
            // Smart wake-up accountability style alarm with spoken briefing only (no on-screen TTS)
            Alarm(
                time = LocalTime.of(7, 30),
                label = "Smart Wake-Up Check",
                daysOfWeek = listOf(1, 2, 3, 4, 5),
                isSmartWakeupEnabled = true,
                wakeupCheckDelayMinutes = 8,
                wakeupCheckTimeoutSeconds = 45,
                isBriefingEnabled = true,
                isTtsEnabled = false,
                briefingTimeoutSeconds = 30,
                snoozeDurationMinutes = 3,
                isSoundEnabled = true,
                isSnoozeEnabled = true
            ),
            // Face challenge alarm, vibration-focused with snooze disabled
            Alarm(
                time = LocalTime.of(8, 15),
                label = "Face Game Challenge",
                daysOfWeek = listOf(2, 4), // Tue, Thu
                isVibrate = true,
                isSoundEnabled = false,
                vibrationPattern = "HEARTBEAT",
                vibrationCrescendoStartGapSeconds = 10,
                smileToDismiss = true,
                smileFallbackMethod = "MATH",
                mathDifficulty = 1,
                mathProblemCount = 2,
                isSnoozeEnabled = false,
                snoozeDurationMinutes = 0
            )
        )
    }

    private fun Alarm.demoKey(): DemoKey =
        DemoKey(
            hour = time.hour,
            minute = time.minute,
            label = label.orEmpty(),
            daysOfWeek = daysOfWeek.sorted()
        )

    private data class DemoKey(
        val hour: Int,
        val minute: Int,
        val label: String,
        val daysOfWeek: List<Int>
    )
}

