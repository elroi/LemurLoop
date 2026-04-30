package com.elroi.lemurloop.domain.buddy

import com.elroi.lemurloop.domain.manager.AccountabilityManager
import com.elroi.lemurloop.domain.manager.SettingsManager
import com.elroi.lemurloop.domain.model.Alarm
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalTime

class AlarmBuddyLifecycleNotifierTest {

    private val confirmedPhone = "+15551234567"

    @Test
    fun `onAlarmSaved new enabled with notify on set calls sendAlarmLifecycleSetMessage`() = runTest {
        val am = mockk<AccountabilityManager>(relaxed = true)
        val sm = mockk<SettingsManager>()
        every { sm.confirmedBuddyNumbersFlow } returns flowOf(setOf(confirmedPhone))
        val notifier = AlarmBuddyLifecycleNotifier(am, sm)
        val alarm = Alarm(
            time = LocalTime.of(7, 0),
            buddyPhoneNumber = confirmedPhone,
            notifyBuddyOnSet = true,
            isEnabled = true,
            userName = "Alex"
        )
        notifier.onAlarmSaved(null, alarm)
        verify(exactly = 1) { am.sendAlarmLifecycleSetMessage(confirmedPhone, alarm) }
    }

    @Test
    fun `onAlarmSaved new unconfirmed buddy does not send`() = runTest {
        val am = mockk<AccountabilityManager>(relaxed = true)
        val sm = mockk<SettingsManager>()
        every { sm.confirmedBuddyNumbersFlow } returns flowOf(emptySet())
        val notifier = AlarmBuddyLifecycleNotifier(am, sm)
        val alarm = Alarm(
            time = LocalTime.of(7, 0),
            buddyPhoneNumber = confirmedPhone,
            notifyBuddyOnSet = true,
            isEnabled = true
        )
        notifier.onAlarmSaved(null, alarm)
        verify(exactly = 0) { am.sendAlarmLifecycleSetMessage(any(), any()) }
    }

    @Test
    fun `onAlarmSaved edit material change with flag sends changed`() = runTest {
        val am = mockk<AccountabilityManager>(relaxed = true)
        val sm = mockk<SettingsManager>()
        every { sm.confirmedBuddyNumbersFlow } returns flowOf(setOf(confirmedPhone))
        val notifier = AlarmBuddyLifecycleNotifier(am, sm)
        val prev = Alarm(time = LocalTime.of(7, 0), buddyPhoneNumber = confirmedPhone)
        val next = prev.copy(time = LocalTime.of(8, 0), notifyBuddyOnChangeOrDismiss = true)
        notifier.onAlarmSaved(prev, next)
        verify(exactly = 1) { am.sendAlarmLifecycleScheduleChangedMessage(confirmedPhone, next) }
        verify(exactly = 0) { am.sendAlarmLifecycleSetMessage(any(), any()) }
    }

    @Test
    fun `onAlarmDismissed sends when flag and confirmed`() = runTest {
        val am = mockk<AccountabilityManager>(relaxed = true)
        val sm = mockk<SettingsManager>()
        every { sm.confirmedBuddyNumbersFlow } returns flowOf(setOf(confirmedPhone))
        val notifier = AlarmBuddyLifecycleNotifier(am, sm)
        val alarm = Alarm(
            time = LocalTime.of(7, 0),
            buddyPhoneNumber = confirmedPhone,
            notifyBuddyOnChangeOrDismiss = true,
            label = "Gym"
        )
        notifier.onAlarmDismissed(alarm)
        verify(exactly = 1) { am.sendAlarmLifecycleDismissedMessage(confirmedPhone, alarm) }
    }

    @Test
    fun `onAlarmDismissed no flag no send`() = runTest {
        val am = mockk<AccountabilityManager>(relaxed = true)
        val sm = mockk<SettingsManager>()
        every { sm.confirmedBuddyNumbersFlow } returns flowOf(setOf(confirmedPhone))
        val notifier = AlarmBuddyLifecycleNotifier(am, sm)
        val alarm = Alarm(
            time = LocalTime.of(7, 0),
            buddyPhoneNumber = confirmedPhone,
            notifyBuddyOnChangeOrDismiss = false
        )
        notifier.onAlarmDismissed(alarm)
        verify(exactly = 0) { am.sendAlarmLifecycleDismissedMessage(any(), any()) }
    }
}
