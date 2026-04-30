package com.elroi.lemurloop.domain.buddy

import com.elroi.lemurloop.domain.manager.AccountabilityManager
import com.elroi.lemurloop.domain.manager.SettingsManager
import com.elroi.lemurloop.domain.model.Alarm
import com.elroi.lemurloop.util.isBuddyPhoneInConfirmedSet
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends optional accountability SMS when alarms are saved or dismissed,
 * respecting per-alarm toggles and confirmed buddy opt-in.
 */
@Singleton
class AlarmBuddyLifecycleNotifier @Inject constructor(
    private val accountabilityManager: AccountabilityManager,
    private val settingsManager: SettingsManager
) {

    suspend fun onAlarmSaved(previous: Alarm?, saved: Alarm) {
        val phone = saved.buddyPhoneNumber?.trim()?.takeIf { it.isNotBlank() } ?: return
        val confirmed = settingsManager.confirmedBuddyNumbersFlow.first()
        if (!isBuddyPhoneInConfirmedSet(phone, confirmed)) return

        if (previous == null) {
            if (saved.notifyBuddyOnSet && saved.isEnabled) {
                accountabilityManager.sendAlarmLifecycleSetMessage(phone, saved)
            }
            return
        }

        if (saved.notifyBuddyOnChangeOrDismiss && hasMaterialBuddyRelevantScheduleChange(previous, saved)) {
            accountabilityManager.sendAlarmLifecycleScheduleChangedMessage(phone, saved)
        }
    }

    suspend fun onAlarmDismissed(alarm: Alarm) {
        if (!alarm.notifyBuddyOnChangeOrDismiss) return
        val phone = alarm.buddyPhoneNumber?.trim()?.takeIf { it.isNotBlank() } ?: return
        val confirmed = settingsManager.confirmedBuddyNumbersFlow.first()
        if (!isBuddyPhoneInConfirmedSet(phone, confirmed)) return
        accountabilityManager.sendAlarmLifecycleDismissedMessage(phone, alarm)
    }
}
