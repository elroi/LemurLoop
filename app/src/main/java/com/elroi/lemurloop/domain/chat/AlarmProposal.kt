package com.elroi.lemurloop.domain.chat

import com.elroi.lemurloop.domain.manager.AlarmDefaults
import com.elroi.lemurloop.domain.model.Alarm
import java.time.LocalTime
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

data class AlarmProposalPartial(
    val hour: Int,
    val minute: Int,
    val label: String?,
    val daysOfWeek: List<Int>
)

data class ParsedLemurChatResponse(
    val message: String,
    val proposal: AlarmProposalPartial?
)

object AlarmProposalValidator {

    fun validate(p: AlarmProposalPartial): Boolean {
        if (p.hour !in 0..23 || p.minute !in 0..59) return false
        if (p.daysOfWeek.any { it !in 1..7 }) return false
        return true
    }
}

object AlarmDraftFactory {

    fun mergeProposal(defaults: AlarmDefaults, previous: Alarm?, proposal: AlarmProposalPartial): Alarm {
        require(AlarmProposalValidator.validate(proposal)) { "invalid proposal" }
        val time = LocalTime.of(proposal.hour, proposal.minute).withSecond(0).withNano(0)
        val label = proposal.label?.trim()?.takeIf { it.isNotBlank() }
        val daysSorted = proposal.daysOfWeek.distinct().sorted()
        return if (previous != null) {
            previous.copy(
                time = time,
                label = label ?: previous.label,
                daysOfWeek = daysSorted
            )
        } else {
            Alarm(
                id = UUID.randomUUID().toString(),
                time = time,
                label = label,
                daysOfWeek = daysSorted,
                isEnabled = false,
                mathDifficulty = defaults.mathDifficulty,
                mathProblemCount = defaults.mathProblemCount,
                mathGraduallyIncreaseDifficulty = defaults.mathGraduallyIncreaseDifficulty,
                isBriefingEnabled = defaults.isBriefingEnabled,
                isTtsEnabled = defaults.isTtsEnabled,
                isSoundEnabled = defaults.isSoundEnabled,
                isVibrate = defaults.isVibrate,
                isGentleWake = defaults.isGentleWake,
                crescendoDurationMinutes = defaults.crescendoDurationMinutes,
                isSnoozeEnabled = defaults.isSnoozeEnabled,
                snoozeDurationMinutes = defaults.snoozeDurationMinutes,
                isSmoothFadeOut = defaults.isSmoothFadeOut,
                isEvasiveSnooze = defaults.isEvasiveSnooze,
                evasiveSnoozesBeforeMoving = defaults.evasiveSnoozesBeforeMoving,
                smileToDismiss = defaults.smileToDismiss,
                smileFallbackMethod = defaults.smileFallbackMethod,
                vibrationPattern = defaults.vibrationPattern,
                vibrationCrescendoStartGapSeconds = defaults.vibrationCrescendoStartGapSeconds,
                soundUri = defaults.defaultSoundUri,
                briefingTimeoutSeconds = defaults.briefingTimeoutSeconds,
                isSmartWakeupEnabled = defaults.isSmartWakeupEnabled,
                wakeupCheckDelayMinutes = defaults.wakeupCheckDelayMinutes,
                wakeupCheckTimeoutSeconds = defaults.wakeupCheckTimeoutSeconds
            )
        }
    }
}

object LemurChatParser {

    /** Text suitable for streaming UI before JSON marker / tail is complete. */
    fun visibleStreamingPrefix(raw: String): String {
        val idx = raw.indexOf(LemurChatPrompts.JSON_MARKER)
        return if (idx >= 0) raw.substring(0, idx).trim() else raw.trim()
    }

    fun parseModelOutput(raw: String): ParsedLemurChatResponse {
        val afterMarker = raw.substringAfter(LemurChatPrompts.JSON_MARKER, "").ifBlank { raw }
        val jsonStr = extractJsonObjectString(afterMarker.trim()) ?: extractJsonObjectString(raw.trim())
        if (jsonStr == null) {
            val fallback = visibleStreamingPrefix(raw).ifBlank { raw.trim() }
            return ParsedLemurChatResponse(fallback, null)
        }
        return try {
            val tokener = JSONTokener(jsonStr.trim())
            val obj = JSONObject(tokener)
            val message = obj.optString("message", "").trim().ifBlank { visibleStreamingPrefix(raw) }
            val proposal = runCatching {
                val proposalObj = obj.optJSONObject("propose_alarm") ?: return@runCatching null
                if (proposalObj.length() == 0) return@runCatching null
                parseProposalOrNull(proposalObj)
            }.getOrNull()
            ParsedLemurChatResponse(message, proposal)
        } catch (_: Exception) {
            ParsedLemurChatResponse(visibleStreamingPrefix(raw).ifBlank { raw.trim() }, null)
        }
    }

    private fun extractJsonObjectString(s: String): String? {
        val t = s.trim().removePrefix("\uFEFF").trim()
        val braceAt = t.indexOf('{')
        if (braceAt >= 0) {
            return t.substring(braceAt)
        }
        val fence = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE).find(t)
        val inner = fence?.groupValues?.getOrNull(1)?.trim()?.removePrefix("\uFEFF")?.trim() ?: return null
        val innerBrace = inner.indexOf('{')
        return if (innerBrace >= 0) inner.substring(innerBrace) else null
    }

    private fun parseProposalOrNull(obj: JSONObject): AlarmProposalPartial? {
        val hour = obj.optInt("hour", -1)
        val minute = obj.optInt("minute", -1)
        val label = obj.optString("label", "").trim().takeIf { it.isNotEmpty() }
        val daysArray: JSONArray = when {
            obj.has("days_of_week") -> obj.optJSONArray("days_of_week") ?: JSONArray()
            obj.has("daysOfWeek") -> obj.optJSONArray("daysOfWeek") ?: JSONArray()
            else -> JSONArray()
        }
        val days = buildList {
            for (i in 0 until daysArray.length()) {
                add(daysArray.optInt(i))
            }
        }
        val proposal = AlarmProposalPartial(hour, minute, label, days)
        return if (AlarmProposalValidator.validate(proposal)) proposal else null
    }
}
