package com.elroi.lemurloop.domain.dismissal

import android.speech.SpeechRecognizer
import javax.inject.Inject

/** How the voice challenge should recover from a SpeechRecognizer error. */
enum class RecognizerRecovery {
    /** The user may try speaking again (within the attempt budget). */
    RETRY,

    /** Speaking cannot succeed now — show the typed number-pad fallback immediately. */
    FALLBACK,
}

/**
 * Maps Android SpeechRecognizer error codes to a recovery action.
 *
 * RETRY covers errors where the user simply speaking again could succeed. Everything
 * else — permissions, unsupported/unavailable language, unreachable network/server,
 * client/fatal, and any unrecognized code — falls back to the typed number pad, so a
 * groggy user never repeats themselves into a path that cannot work.
 */
class RecognizerErrorClassifier @Inject constructor() {

    fun classify(errorCode: Int): RecognizerRecovery = when (errorCode) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
        SpeechRecognizer.ERROR_AUDIO -> RecognizerRecovery.RETRY

        else -> RecognizerRecovery.FALLBACK
    }
}
