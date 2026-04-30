package com.elroi.lemurloop.util

/**
 * Matches stored buddy numbers and opt-in flow (digits and + only), allowing
 * partial matches when country codes differ.
 */
fun normalizePhoneDigitsPlus(phone: String): String =
    phone.replace(Regex("[^\\d+]"), "")

fun isBuddyPhoneInConfirmedSet(phone: String, confirmed: Set<String>): Boolean {
    val normalizedPhone = normalizePhoneDigitsPlus(phone)
    if (normalizedPhone.isBlank()) return false
    return confirmed.any { stored ->
        val n = normalizePhoneDigitsPlus(stored)
        n.endsWith(normalizedPhone) || normalizedPhone.endsWith(n)
    }
}
