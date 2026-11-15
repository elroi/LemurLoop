package com.elroi.lemurloop.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.elroi.lemurloop.data.local.entity.TimerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {
    @Query("SELECT * FROM timers ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<TimerEntity>>

    @Query("SELECT * FROM timers ORDER BY createdAtEpochMillis DESC")
    suspend fun getAll(): List<TimerEntity>

    @Query("SELECT * FROM timers WHERE id = :id")
    suspend fun getById(id: Long): TimerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TimerEntity): Long

    @Query("DELETE FROM timers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM timers")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM timers")
    suspend fun count(): Int
}
