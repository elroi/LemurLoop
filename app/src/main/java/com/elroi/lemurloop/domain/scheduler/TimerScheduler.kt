package com.elroi.lemurloop.domain.scheduler

interface TimerScheduler {
    fun schedule(timerId: Long, triggerAtEpochMillis: Long)
    fun cancel(timerId: Long)
}
