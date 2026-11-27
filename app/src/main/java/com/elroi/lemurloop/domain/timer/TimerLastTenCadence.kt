package com.elroi.lemurloop.domain.timer

/**
 * Timing for the spoken "10 … 1" countdown.
 *
 * Every digit is due at a fixed wall-clock time measured from the timer's end, never relative
 * to when the previous digit finished speaking. Speech length varies per digit and per engine,
 * so chaining on completion drifts, then bunches up and drops digits.
 *
 * The onset lead is how long the TTS engine takes between `speak()` and audio starting. The
 * service measures it and asks for each digit that much early, so the sound lands on the second.
 */
object TimerLastTenCadence {
    const val MAX_ONSET_LEAD_MILLIS = 400L

    /** Used for the first digit, before any latency has been measured. */
    const val INITIAL_ONSET_LEAD_MILLIS = 150L

    fun speakAtEpochMillis(digit: Int, endsAtEpochMillis: Long, onsetLeadMillis: Long): Long =
        endsAtEpochMillis - digit * 1_000L - onsetLeadMillis

    /** Smooths the measured onset latency so one slow utterance doesn't jerk the next. */
    fun nextOnsetLeadMillis(previousLeadMillis: Long?, observedLatencyMillis: Long): Long {
        val observed = observedLatencyMillis.coerceIn(0L, MAX_ONSET_LEAD_MILLIS)
        val previous = previousLeadMillis ?: return observed
        return ((previous + observed) / 2L).coerceIn(0L, MAX_ONSET_LEAD_MILLIS)
    }
}
