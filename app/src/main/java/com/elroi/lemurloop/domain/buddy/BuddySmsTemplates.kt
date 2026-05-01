package com.elroi.lemurloop.domain.buddy

/**
 * Substitutes placeholders in user-editable buddy SMS templates.
 * Supported: {name}, {label}, {time}, {repeat}, {event} (follow-up only; empty on set-SMS path if unused).
 */
fun String.applyBuddyLifecyclePlaceholders(
    name: String,
    label: String,
    time: String,
    repeat: String,
    event: String = ""
): String = this
    .replace("{name}", name)
    .replace("{label}", label)
    .replace("{time}", time)
    .replace("{repeat}", repeat)
    .replace("{event}", event)
