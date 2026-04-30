package com.elroi.lemurloop.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuddyPhoneMatchingTest {

    @Test
    fun `confirmed set matches when suffix overlaps`() {
        assertTrue(isBuddyPhoneInConfirmedSet("+15551234567", setOf("5551234567")))
    }

    @Test
    fun `no match when unrelated`() {
        assertFalse(isBuddyPhoneInConfirmedSet("+111", setOf("+222")))
    }
}
