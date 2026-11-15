package com.elroi.lemurloop.domain.dismissal

import android.speech.SpeechRecognizer
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Stage 1, slice 2 of the voice challenge (see docs/voice-challenge-plan.md).
 *
 * The plan's rule "any failure -> fallback" is too coarse (review finding #9). This
 * maps each Android SpeechRecognizer error code to one of two recoveries:
 *  - RETRY: the user may just try speaking again, within the attempt budget
 *           (no match, speech timeout, recognizer busy, transient audio glitch).
 *  - FALLBACK: speaking cannot succeed now, so show the number pad immediately
 *           (permissions, unsupported/unavailable language, network/server
 *           unreachable, client/fatal, and any unknown code).
 *
 * Not wasting a groggy user's spoken attempts on an unreachable backend is why
 * network/server errors fall back immediately rather than retrying.
 */
class RecognizerErrorClassifierTest {

    private val classifier = RecognizerErrorClassifier()

    @Test
    fun `transient speech errors are retryable`() {
        assertEquals(RecognizerRecovery.RETRY, classifier.classify(SpeechRecognizer.ERROR_NO_MATCH))
        assertEquals(RecognizerRecovery.RETRY, classifier.classify(SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
        assertEquals(RecognizerRecovery.RETRY, classifier.classify(SpeechRecognizer.ERROR_RECOGNIZER_BUSY))
        assertEquals(RecognizerRecovery.RETRY, classifier.classify(SpeechRecognizer.ERROR_AUDIO))
    }

    @Test
    fun `permission and language errors fall back immediately`() {
        assertEquals(RecognizerRecovery.FALLBACK, classifier.classify(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS))
        assertEquals(RecognizerRecovery.FALLBACK, classifier.classify(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED))
        assertEquals(RecognizerRecovery.FALLBACK, classifier.classify(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE))
    }

    @Test
    fun `network and server errors fall back immediately`() {
        assertEquals(RecognizerRecovery.FALLBACK, classifier.classify(SpeechRecognizer.ERROR_NETWORK))
        assertEquals(RecognizerRecovery.FALLBACK, classifier.classify(SpeechRecognizer.ERROR_NETWORK_TIMEOUT))
        assertEquals(RecognizerRecovery.FALLBACK, classifier.classify(SpeechRecognizer.ERROR_SERVER))
        assertEquals(RecognizerRecovery.FALLBACK, classifier.classify(SpeechRecognizer.ERROR_SERVER_DISCONNECTED))
    }

    @Test
    fun `client and unknown errors fall back`() {
        assertEquals(RecognizerRecovery.FALLBACK, classifier.classify(SpeechRecognizer.ERROR_CLIENT))
        assertEquals(RecognizerRecovery.FALLBACK, classifier.classify(999))
    }
}
