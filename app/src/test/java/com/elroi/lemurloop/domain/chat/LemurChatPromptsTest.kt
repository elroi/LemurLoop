package com.elroi.lemurloop.domain.chat

import org.junit.Assert.assertTrue
import org.junit.Test

class LemurChatPromptsTest {

    @Test
    fun appCapabilitiesSnippet_listsModelLimitsAndAdvancedPaths() {
        val s = LemurChatPrompts.appCapabilitiesSnippet()
        assertTrue(s.contains("propose_alarm", ignoreCase = true))
        assertTrue(s.contains("Math challenge", ignoreCase = true))
        assertTrue(s.contains("buddy/SMS", ignoreCase = true))
        assertTrue(s.contains("smile-to-dismiss", ignoreCase = true))
        assertTrue(s.contains("briefing", ignoreCase = true))
        assertTrue(s.contains("smart wakeup", ignoreCase = true))
        assertTrue(s.contains("wake-up check", ignoreCase = true))
    }

    @Test
    fun buildStreamingPrompt_embedsCapabilitiesSnippet() {
        val prompt = LemurChatPrompts.buildStreamingPrompt(
            assistantName = "Lemur",
            historyTurns = emptyList(),
            userMessage = "Hi",
            repairHint = null
        )
        assertTrue(prompt.contains(LemurChatPrompts.appCapabilitiesSnippet()))
    }

    @Test
    fun buildRepairPrompt_embedsCapabilitiesSnippet() {
        val prompt = LemurChatPrompts.buildRepairPrompt(
            assistantName = "Lemur",
            rawBrokenOutput = "broken",
            validationHint = "bad json"
        )
        assertTrue(prompt.contains(LemurChatPrompts.appCapabilitiesSnippet()))
    }
}
