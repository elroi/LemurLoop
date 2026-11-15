package com.elroi.lemurloop.domain.repository

import com.elroi.lemurloop.domain.timer.CountdownTimer
import kotlinx.coroutines.flow.Flow

interface TimerRepository {
    fun observeAll(): Flow<List<CountdownTimer>>
    suspend fun getAll(): List<CountdownTimer>
    suspend fun getById(id: Long): CountdownTimer?
    suspend fun upsert(timer: CountdownTimer): Long
    suspend fun delete(id: Long)
    suspend fun deleteAll()
    suspend fun count(): Int
}
