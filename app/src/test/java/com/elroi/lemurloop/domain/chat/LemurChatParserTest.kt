package com.elroi.lemurloop.domain.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LemurChatParserTest {

    @Test
    fun parseModelOutput_plainJson_parsesProposal() {
        val raw = """{"message":"Hi","propose_alarm":{"hour":8,"minute":15,"days_of_week":[]}}"""
        val parsed = LemurChatParser.parseModelOutput(raw)
        assertEquals("Hi", parsed.message.trim())
        assertNotNull(parsed.proposal)
        assertEquals(8, parsed.proposal!!.hour)
        assertEquals(15, parsed.proposal!!.minute)
    }

    @Test
    fun parseModelOutput_markerAndJson_extractsMessageAndProposal() {
        val marker = LemurChatPrompts.JSON_MARKER
        val raw = "Sounds good\n$marker\n" +
            """{"message":"Sounds good","propose_alarm":{"hour":7,"minute":0,"days_of_week":[]}}"""

        val parsed = LemurChatParser.parseModelOutput(raw)
        assertEquals("Sounds good", parsed.message.trim())
        assertNotNull(parsed.proposal)
        assertEquals(7, parsed.proposal!!.hour)
        assertEquals(0, parsed.proposal!!.minute)
        assertEquals(emptyList<Int>(), parsed.proposal!!.daysOfWeek)
    }

    @Test
    fun visibleStreamingPrefix_hidesJsonTail() {
        val partial = "Hello\n<<<JSON>>>"
        assertEquals("Hello", LemurChatParser.visibleStreamingPrefix(partial))
    }

    @Test
    fun parseModelOutput_jsonFence_stillWorks() {
        val raw = """
            Hi there
            <<<JSON>>>
            ```json
            {"message":"Hi there","propose_alarm":null}
            ```
        """.trimIndent()
        val parsed = LemurChatParser.parseModelOutput(raw)
        assertEquals("Hi there", parsed.message)
        assertNull(parsed.proposal)
    }
}
