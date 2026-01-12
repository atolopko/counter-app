package com.example.counterapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterDao {
    @Query("SELECT * FROM counters")
    fun getAllCounters(): Flow<List<Counter>>

    @Query("SELECT * FROM counters WHERE id = :id")
    suspend fun getCounterById(id: Long): Counter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCounter(counter: Counter): Long

    @Update
    suspend fun updateCounter(counter: Counter)

    @Delete
    suspend fun deleteCounter(counter: Counter)
}

@Dao
interface EventLogDao {
    @Query("SELECT * FROM event_logs WHERE counterId = :counterId ORDER BY timestamp ASC")
    fun getLogsForCounterAsc(counterId: Long): Flow<List<EventLog>>

    @Query("SELECT * FROM event_logs WHERE counterId = :counterId ORDER BY timestamp ASC")
    suspend fun getAllLogsForCounterAsc(counterId: Long): List<EventLog>

    @Query("SELECT * FROM event_logs WHERE counterId = :counterId ORDER BY timestamp DESC")
    fun getLogsForCounter(counterId: Long): Flow<List<EventLog>>

    @Insert
    suspend fun insertLog(log: EventLog)

    @Update
    suspend fun updateLog(log: EventLog)

    @Update
    suspend fun updateLogs(logs: List<EventLog>)

    @Query("SELECT * FROM event_logs WHERE counterId = :counterId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLogForCounter(counterId: Long): EventLog?

    @Delete
    suspend fun deleteEventLog(log: EventLog)

    @Query("DELETE FROM event_logs WHERE counterId = :counterId")
    suspend fun deleteLogsForCounter(counterId: Long)

    @Transaction
    suspend fun updateLogsAndCounter(logs: List<EventLog>, counter: Counter, counterDao: CounterDao) {
        updateLogs(logs)
        counterDao.updateCounter(counter)
    }

    @Transaction
    suspend fun deleteLogAndUpdateCounter(log: EventLog, logs: List<EventLog>, counter: Counter, counterDao: CounterDao) {
        deleteEventLog(log)
        updateLogs(logs)
        counterDao.updateCounter(counter)
    }
}
