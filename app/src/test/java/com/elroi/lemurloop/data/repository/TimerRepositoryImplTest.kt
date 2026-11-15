package com.elroi.lemurloop.data.repository

import com.elroi.lemurloop.data.local.dao.TimerDao
import com.elroi.lemurloop.data.local.entity.TimerEntity
import com.elroi.lemurloop.domain.timer.TimerPhase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerRepositoryImplTest {

    @Test
    fun getAll_ordersNewestCreatedAtFirstEvenWhenTheDaoDoesNot() = runBlocking {
        val older = entity(id = 1L, createdAt = 10L)
        val newer = entity(id = 2L, createdAt = 50L)
        val repository = TimerRepositoryImpl(OldestFirstTimerDao(listOf(older, newer)))

        assertEquals(listOf(2L, 1L), repository.getAll().map { it.id })
        assertEquals(10L, repository.getAll().first { it.id == 1L }.createdAtEpochMillis)
    }

    private fun entity(id: Long, createdAt: Long) = TimerEntity(
        id = id,
        label = "t$id",
        originalDurationMillis = 1_000L,
        endsAtEpochMillis = 20L,
        pausedRemainingMillis = null,
        phase = TimerPhase.Running.name,
        createdAtEpochMillis = createdAt
    )

    private class OldestFirstTimerDao(private val rows: List<TimerEntity>) : TimerDao {
        override fun observeAll(): Flow<List<TimerEntity>> = flowOf(rows)
        override suspend fun getAll(): List<TimerEntity> = rows
        override suspend fun getById(id: Long): TimerEntity? = rows.firstOrNull { it.id == id }
        override suspend fun upsert(entity: TimerEntity): Long = entity.id
        override suspend fun deleteById(id: Long) = Unit
        override suspend fun deleteAll() = Unit
        override suspend fun count(): Int = rows.size
    }
}
