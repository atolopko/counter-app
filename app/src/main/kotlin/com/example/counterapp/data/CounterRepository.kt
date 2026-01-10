package com.example.counterapp.data

import kotlinx.coroutines.flow.Flow

class CounterRepository(
    private val counterDao: CounterDao,
    private val eventLogDao: EventLogDao
) {
    val allCounters: Flow<List<Counter>> = counterDao.getAllCounters()

    fun getLogsForCounter(counterId: Long): Flow<List<EventLog>> =
        eventLogDao.getLogsForCounter(counterId)

    suspend fun addCounter(name: String, initialIncrement: Int) {
        counterDao.insertCounter(Counter(name = name, lastIncrementAmount = initialIncrement))
    }

    suspend fun incrementCounter(counter: Counter, amount: Int) {
        val newCount = counter.currentCount + amount
        val updatedCounter = counter.copy(currentCount = newCount, lastIncrementAmount = amount)
        counterDao.updateCounter(updatedCounter)
        
        eventLogDao.insertLog(
            EventLog(
                counterId = counter.id,
                timestamp = System.currentTimeMillis(),
                amountChanged = amount,
                resultingCount = newCount
            )
        )
    }

    suspend fun updateCounter(counter: Counter) {
        counterDao.updateCounter(counter)
    }

    suspend fun deleteCounter(counter: Counter) {
        eventLogDao.deleteLogsForCounter(counter.id)
        counterDao.deleteCounter(counter)
    }

    suspend fun getCounterById(id: Long): Counter? = counterDao.getCounterById(id)
}
