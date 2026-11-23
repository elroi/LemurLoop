package com.elroi.lemurloop.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.elroi.lemurloop.domain.timer.CountdownTimer
import com.elroi.lemurloop.domain.timer.TimerPhase

@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val originalDurationMillis: Long,
    val endsAtEpochMillis: Long?,
    val pausedRemainingMillis: Long?,
    val phase: String,
    val createdAtEpochMillis: Long,
    val voiceCueStartOverride: Boolean? = null,
    val voiceCueMidRemainingOverride: Boolean? = null,
    val voiceCueLastTenOverride: Boolean? = null
) {
    fun toDomain(): CountdownTimer {
        return CountdownTimer(
            id = id,
            label = label,
            originalDurationMillis = originalDurationMillis,
            endsAtEpochMillis = endsAtEpochMillis,
            pausedRemainingMillis = pausedRemainingMillis,
            phase = TimerPhase.valueOf(phase),
            createdAtEpochMillis = createdAtEpochMillis,
            voiceCueStartOverride = voiceCueStartOverride,
            voiceCueMidRemainingOverride = voiceCueMidRemainingOverride,
            voiceCueLastTenOverride = voiceCueLastTenOverride
        )
    }

    companion object {
        fun fromDomain(timer: CountdownTimer): TimerEntity {
            return TimerEntity(
                id = timer.id,
                label = timer.label,
                originalDurationMillis = timer.originalDurationMillis,
                endsAtEpochMillis = timer.endsAtEpochMillis,
                pausedRemainingMillis = timer.pausedRemainingMillis,
                phase = timer.phase.name,
                createdAtEpochMillis = timer.createdAtEpochMillis,
                voiceCueStartOverride = timer.voiceCueStartOverride,
                voiceCueMidRemainingOverride = timer.voiceCueMidRemainingOverride,
                voiceCueLastTenOverride = timer.voiceCueLastTenOverride
            )
        }
    }
}
