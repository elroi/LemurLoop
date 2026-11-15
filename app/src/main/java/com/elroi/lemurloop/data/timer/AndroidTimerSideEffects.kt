package com.elroi.lemurloop.data.timer

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.elroi.lemurloop.domain.timer.TimerRingControls
import com.elroi.lemurloop.domain.timer.TimerRingStop
import com.elroi.lemurloop.domain.timer.TimerTrackGrantReleaser
import com.elroi.lemurloop.service.TimerRingService

class AndroidTimerRingStop(
    private val context: Context
) : TimerRingStop {
    override fun stop() {
        context.stopService(
            Intent().setClassName(context, SERVICE_CLASS_NAME)
        )
    }

    companion object {
        const val SERVICE_CLASS_NAME = "com.elroi.lemurloop.service.TimerRingService"
    }
}

class AndroidTimerRingControls(
    private val context: Context
) : TimerRingControls {
    override fun dismiss() {
        context.startService(
            Intent(context, TimerRingService::class.java).setAction(TimerRingService.ACTION_DISMISS)
        )
    }
}

class AndroidTimerTrackGrantReleaser(
    private val context: Context
) : TimerTrackGrantReleaser {
    override fun release(uri: String?) {
        if (uri.isNullOrBlank()) return
        try {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // The grant was already released or was never held.
        }
    }
}
