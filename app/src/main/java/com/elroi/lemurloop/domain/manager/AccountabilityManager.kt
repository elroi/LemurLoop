package com.elroi.lemurloop.domain.manager

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import com.elroi.lemurloop.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountabilityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {

    /**
     * Sends an opt-in confirmation SMS to a newly assigned buddy.
     * The buddy must reply with the generated 4-digit code to be confirmed.
     */
    suspend fun sendBuddyOptInRequest(
        phoneNumber: String,
        buddyName: String?,
        userName: String?
    ) {
        if (phoneNumber.isBlank()) return

        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.SEND_SMS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("AccountabilityManager", "SEND_SMS permission not granted — opt-in SMS not sent")
            return
        }

        val code = generateBuddyCode()
        settingsManager.addPendingBuddyCode(code, phoneNumber)

        val name = buddyName?.trim()?.takeIf { it.isNotBlank() } ?: context.getString(R.string.buddy_invite_default_name)
        val user = userName?.trim()?.takeIf { it.isNotBlank() } ?: context.getString(R.string.buddy_invite_default_user)
        val message = context.getString(R.string.buddy_invite_sms, name, user, code)

        sendSms(phoneNumber, message)
    }

    fun sendBuddyConfirmationSuccess(phoneNumber: String) {
        val message = context.getString(R.string.buddy_confirm_sms)
        sendSms(phoneNumber, message)
    }

    private fun generateBuddyCode(): String {
        return (1000..9999).random().toString()
    }

    fun sendMissedAlarmMessage(
        phoneNumber: String,
        alarmLabel: String? = null,
        userName: String? = null,
        customMessage: String? = null
    ) {
        if (phoneNumber.isBlank()) return

        // Check permission
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.SEND_SMS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("AccountabilityManager", "SEND_SMS permission not granted — SMS not sent")
            return
        }

        val message = if (!customMessage.isNullOrBlank()) {
            customMessage.replace("{name}", userName ?: context.getString(R.string.buddy_missed_alarm_they))
        } else {
            val alarmDesc = if (!alarmLabel.isNullOrBlank()) "\"$alarmLabel\"" else context.getString(R.string.buddy_missed_alarm_an_alarm)
            val whoText = if (!userName.isNullOrBlank()) userName else context.getString(R.string.buddy_invite_default_user)
            context.getString(R.string.buddy_missed_alarm_sms, whoText, alarmDesc)
        }

        sendSms(phoneNumber, message)
    }

    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val subId = SubscriptionManager.getDefaultSmsSubscriptionId()
                context.getSystemService(SmsManager::class.java).createForSubscriptionId(subId)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val parts = smsManager.divideMessage(message)
            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }
            Log.d("AccountabilityManager", "SMS sent to $phoneNumber: $message")
        } catch (e: Exception) {
            Log.e("AccountabilityManager", "Failed to send SMS", e)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, context.getString(com.elroi.lemurloop.R.string.toast_sms_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }
}
