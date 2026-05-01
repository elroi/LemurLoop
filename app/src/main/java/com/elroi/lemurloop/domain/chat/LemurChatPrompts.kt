package com.elroi.lemurloop.domain.chat

object LemurChatPrompts {

    const val JSON_MARKER = "<<<JSON>>>"

    fun buildStreamingPrompt(
        assistantName: String,
        historyTurns: List<Pair<String, String>>,
        userMessage: String,
        repairHint: String?
    ): String {
        val historyText = historyTurns.takeLast(MAX_HISTORY_TURNS).joinToString("\n") { (role, text) ->
            "${role.uppercase()}: $text"
        }
        val repair = repairHint?.let { "\nFix the previous response: $it\n" } ?: ""
        return """
You are "$assistantName", the conversational assistant in the LemurLoop alarm clock app.

Help the user configure ONE alarm: local time (hour 0-23, minute 0-59), optional label, repeating weekdays or one-time.
Weekdays use ISO day-of-week: Monday=1 through Sunday=7. Use an empty array [] for days_of_week only when the user clearly wants a one-time (non-repeating) alarm.

Output format (required):
1) Write your conversational reply to the user in plain language (no JSON in this part).
2) On its own line, output exactly: $JSON_MARKER
3) Next line onward: a single JSON object (optionally wrapped in a markdown ```json fence).

JSON shape:
{"message":"<same friendly reply text as section 1>","propose_alarm":null}
Or when you can propose concrete settings:
{"message":"<friendly reply>","propose_alarm":{"hour":7,"minute":30,"label":"","days_of_week":[1,2,3,4,5]}}

If you still need details from the user, set propose_alarm to null.

$repair
Prior conversation:
$historyText

USER: $userMessage
""".trimIndent()
    }

    fun buildRepairPrompt(
        assistantName: String,
        rawBrokenOutput: String,
        validationHint: String
    ): String = """
You are "$assistantName" for LemurLoop. Your previous reply could not be parsed or validated.

Previous output:
$rawBrokenOutput

Validation issue:
$validationHint

Reply in plain language, then a line $JSON_MARKER then JSON only:
{"message":"<short apology + clarify>","propose_alarm":null}
Use valid propose_alarm only if you can supply hour 0-23, minute 0-59, days_of_week in 1..7.
""".trimIndent()

    private const val MAX_HISTORY_TURNS = 8
}
