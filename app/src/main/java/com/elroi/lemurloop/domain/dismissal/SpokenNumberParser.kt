package com.elroi.lemurloop.domain.dismissal

import javax.inject.Inject

/**
 * Converts English spoken-number text (as returned by Automatic Speech Recognition)
 * into the integer the user said, or null when it cannot be parsed.
 *
 * Stage 1 of the voice challenge is English-only by design (see
 * docs/voice-challenge-plan.md); a Hebrew parser is a later increment. Returning null
 * on anything unrecognized lets the challenge loop retry rather than accept a wrong
 * answer — deliberately conservative, since this gates a wake-up check.
 */
class SpokenNumberParser @Inject constructor() {

    private val units = mapOf(
        "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
        "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
        "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
        "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
        "eighteen" to 18, "nineteen" to 19,
    )

    private val tens = mapOf(
        "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
        "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90,
    )

    fun parse(spoken: String): Int? {
        val cleaned = spoken.trim().lowercase()
            .replace('-', ' ')
            .replace(",", " ")
        if (cleaned.isEmpty()) return null

        // Bare digits, e.g. "12" or "105".
        if (cleaned.matches(Regex("""^\d+$"""))) return cleaned.toIntOrNull()

        val tokens = cleaned.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null

        var total = 0      // accumulates completed scale groups (… thousand)
        var current = 0    // the group being built
        var sawNumber = false

        for (token in tokens) {
            when (token) {
                "and" -> continue            // filler: "a hundred and five"
                "a" -> {                     // leading article before a scale: "a hundred"
                    current += 1
                    sawNumber = true
                }
                "hundred" -> {
                    current = (if (current == 0) 1 else current) * 100
                    sawNumber = true
                }
                "thousand" -> {
                    total += (if (current == 0) 1 else current) * 1000
                    current = 0
                    sawNumber = true
                }
                in units -> {
                    current += units.getValue(token)
                    sawNumber = true
                }
                in tens -> {
                    current += tens.getValue(token)
                    sawNumber = true
                }
                else -> return null          // any unrecognized word fails the whole parse
            }
        }

        return if (sawNumber) total + current else null
    }
}
