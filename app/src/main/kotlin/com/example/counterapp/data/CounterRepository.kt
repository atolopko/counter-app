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

    suspend fun undoLastIncrement(counter: Counter) {
        val latestLog = eventLogDao.getLatestLogForCounter(counter.id)
        if (latestLog != null) {
            val newCount = counter.currentCount - latestLog.amountChanged
            val updatedCounter = counter.copy(currentCount = newCount)
            counterDao.updateCounter(updatedCounter)
            eventLogDao.deleteEventLog(latestLog)
        }
    }

    suspend fun updateLogAndRecalculate(counterId: Long, logId: Long, newAmount: Int, newTimestamp: Long) {
        val logs = eventLogDao.getAllLogsForCounterAsc(counterId).toMutableList()
        val logIndex = logs.indexOfFirst { it.id == logId }
        if (logIndex != -1) {
            // Update the target log
            logs[logIndex] = logs[logIndex].copy(amountChanged = newAmount, timestamp = newTimestamp)
            
            // Re-sort in case timestamp changed
            logs.sortBy { it.timestamp }
            
            // Recalculate resulting counts
            var runningCount = 0
            val updatedLogs = logs.map { log ->
                runningCount += log.amountChanged
                log.copy(resultingCount = runningCount)
            }
            
            // Update logs in database and counter total atomically
            val counter = counterDao.getCounterById(counterId)
            if (counter != null) {
                eventLogDao.updateLogsAndCounter(updatedLogs, counter.copy(currentCount = runningCount), counterDao)
            }
        }
    }
    suspend fun deleteLogAndRecalculate(counterId: Long, logId: Long) {
        val logs = eventLogDao.getAllLogsForCounterAsc(counterId).toMutableList()
        val logIndex = logs.indexOfFirst { it.id == logId }
        if (logIndex != -1) {
            val logToDelete = logs[logIndex]
            logs.removeAt(logIndex)
            
            // Recalculate resulting counts
            var runningCount = 0
            val updatedLogs = logs.map { log ->
                runningCount += log.amountChanged
                log.copy(resultingCount = runningCount)
            }
            
            // Delete the log, update other logs and counter total atomically
            val counter = counterDao.getCounterById(counterId)
            if (counter != null) {
                eventLogDao.deleteLogAndUpdateCounter(
                    logToDelete,
                    updatedLogs, 
                    counter.copy(currentCount = runningCount), 
                    counterDao
                )
            }
        }
    }
}
