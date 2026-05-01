package com.elroi.lemurloop.domain.buddy

import org.junit.Assert.assertEquals
import org.junit.Test

class BuddySmsTemplatesTest {

    @Test
    fun applyBuddyLifecyclePlaceholders_replacesAllTags() {
        val template =
            "{name}: {label} @ {time} ({repeat}). More: {event}"
        assertEquals(
            "Alex: Workout @ 07:00 (Mon). More: dismissed now",
            template.applyBuddyLifecyclePlaceholders(
                name = "Alex",
                label = "Workout",
                time = "07:00",
                repeat = "Mon",
                event = "dismissed now"
            )
        )
    }

    @Test
    fun applyBuddyLifecyclePlaceholders_emptyEvent_whenOmitted() {
        val template = "{name}|{event}"
        assertEquals(
            "Sam|",
            template.applyBuddyLifecyclePlaceholders(
                name = "Sam",
                label = "",
                time = "",
                repeat = "",
                event = ""
            )
        )
    }
}
