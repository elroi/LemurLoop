package com.elroi.lemurloop.domain.dismissal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Stage 1, slice 1 of the voice challenge (see docs/voice-challenge-plan.md).
 * Pure English spoken-number -> Int parsing. The ASR layer hands recognized text
 * (words and/or digits) to this; it must map it to the integer the user said, or
 * null when it cannot, so the challenge loop can retry.
 */
class SpokenNumberParserTest {

    private val parser = SpokenNumberParser()

    @Test
    fun `bare digit string parses`() {
        assertEquals(12, parser.parse("12"))
        assertEquals(0, parser.parse("0"))
        assertEquals(105, parser.parse("105"))
    }

    @Test
    fun `single number words parse`() {
        assertEquals(0, parser.parse("zero"))
        assertEquals(5, parser.parse("five"))
        assertEquals(12, parser.parse("twelve"))
        assertEquals(30, parser.parse("thirty"))
    }

    @Test
    fun `compound tens parse with space or hyphen`() {
        assertEquals(21, parser.parse("twenty one"))
        assertEquals(21, parser.parse("twenty-one"))
        assertEquals(99, parser.parse("ninety nine"))
    }

    @Test
    fun `hundreds parse with and without 'and' and leading article`() {
        assertEquals(105, parser.parse("one hundred five"))
        assertEquals(105, parser.parse("a hundred and five"))
        assertEquals(100, parser.parse("hundred"))
        assertEquals(150, parser.parse("one hundred and fifty"))
    }

    @Test
    fun `case and surrounding whitespace are ignored`() {
        assertEquals(12, parser.parse("  Twelve  "))
        assertEquals(42, parser.parse("Forty Two"))
    }

    @Test
    fun `unparseable input returns null`() {
        assertNull(parser.parse(""))
        assertNull(parser.parse("   "))
        assertNull(parser.parse("banana"))
        assertNull(parser.parse("twelve apples"))
    }
}
