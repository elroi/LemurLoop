package com.elroi.lemurloop.data.repository

import com.elroi.lemurloop.data.local.dao.TimerDao
import com.elroi.lemurloop.data.local.entity.TimerEntity
import com.elroi.lemurloop.domain.repository.TimerRepository
import com.elroi.lemurloop.domain.timer.CountdownTimer
import com.elroi.lemurloop.domain.timer.TimerOrdering
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TimerRepositoryImpl(
    private val dao: TimerDao
) : TimerRepository {
    override fun observeAll(): Flow<List<CountdownTimer>> {
        return dao.observeAll().map { entities ->
            TimerOrdering.newestFirst(entities.map { it.toDomain() })
        }
    }

    override suspend fun getAll(): List<CountdownTimer> {
        return TimerOrdering.newestFirst(dao.getAll().map { it.toDomain() })
    }

    override suspend fun getById(id: Long): CountdownTimer? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun upsert(timer: CountdownTimer): Long {
        return dao.upsert(TimerEntity.fromDomain(timer))
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }

    override suspend fun count(): Int = dao.count()
}
