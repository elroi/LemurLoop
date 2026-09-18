package com.elroi.lemurloop.domain.manager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * generateContentStreaming used to build a com.google.ai.client.generativeai.GenerativeModel
 * (SDK 0.9.0) and call generateContentStream() on it. That SDK predates newer model names
 * such as gemini-2.5-flash and returns no chunks for them, even though the same model works
 * fine through the raw HTTP path (generateWithKey / getWorkingModelConfig already use it).
 * The fix routes streaming through the raw HTTP streamGenerateContent SSE endpoint instead,
 * parsed line-by-line by parseSseTextDelta. These tests pin that parser's contract.
 */
class GeminiManagerTest {

    @Test
    fun parseSseTextDelta_validDataLine_returnsText() {
        val line = """data: {"candidates":[{"content":{"parts":[{"text":"Hello"}]}}]}"""

        assertEquals("Hello", GeminiManager.parseSseTextDelta(line))
    }

    @Test
    fun parseSseTextDelta_nonDataLine_returnsNull() {
        assertNull(GeminiManager.parseSseTextDelta("event: message"))
        assertNull(GeminiManager.parseSseTextDelta(""))
    }

    @Test
    fun parseSseTextDelta_doneSentinel_returnsNull() {
        assertNull(GeminiManager.parseSseTextDelta("data: [DONE]"))
    }

    @Test
    fun parseSseTextDelta_malformedJson_returnsNullInsteadOfThrowing() {
        assertNull(GeminiManager.parseSseTextDelta("data: {not json"))
    }

    @Test
    fun parseSseTextDelta_emptyCandidates_returnsNull() {
        val line = """data: {"candidates":[]}"""

        assertNull(GeminiManager.parseSseTextDelta(line))
    }

    @Test
    fun parseSseTextDelta_emptyPartsText_returnsNull() {
        val line = """data: {"candidates":[{"content":{"parts":[{"text":""}]}}]}"""

        assertNull(GeminiManager.parseSseTextDelta(line))
    }
}
