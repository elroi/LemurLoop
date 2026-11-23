package com.elroi.lemurloop.domain.scheduler

import com.elroi.lemurloop.domain.timer.TimerVoiceCueKind

interface TimerVoiceCueScheduler {
    fun schedule(timerId: Long, kind: TimerVoiceCueKind, triggerAtEpochMillis: Long)
    fun cancel(timerId: Long, kind: TimerVoiceCueKind)
    fun cancelAllForTimer(timerId: Long)
}
