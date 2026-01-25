package com.example.counterapp.data

import kotlinx.coroutines.flow.Flow
import java.util.*

class CounterRepository(
    private val counterDao: CounterDao,
    private val eventLogDao: EventLogDao
) {
    val allCounters: Flow<List<Counter>> = counterDao.getAllCounters()

    fun getLogsForCounter(counterId: Long): Flow<List<EventLog>> =
        eventLogDao.getLogsForCounter(counterId)

    fun getTodaySums(since: Long): Flow<List<CounterSum>> =
        eventLogDao.getSumsSince(since)

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

    suspend fun importFromText(text: String): Map<String, Int> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var currentCounterName: String? = null
        // Regex to identify data lines: MM/DD/YY values (space delimited)
        val dataRegex = Regex("""^(\d{1,2}/\d{1,2}(?:/\d{2,4})?)\s+(.+)$""")
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        val pendingData = mutableMapOf<String, MutableList<Pair<Long, Int>>>()
        val summary = mutableMapOf<String, Int>()

        for (line in lines) {
            val match = dataRegex.find(line)
            if (match != null) {
                // This is a data line. If we don't have a counter name yet, we can't do anything with it.
                val counterName = currentCounterName ?: continue
                
                val dateStr = match.groupValues[1]
                val valuesStr = match.groupValues[2]
                
                val dateParts = dateStr.split("/")
                val month = dateParts[0].toInt() - 1
                val day = dateParts[1].toInt()
                val year = if (dateParts.size > 2) {
                    var y = dateParts[2].toInt()
                    if (y < 100) y += 2000
                    y
                } else {
                    currentYear
                }
                
                calendar.set(year, month, day, 12, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val timestamp = calendar.timeInMillis
                
                val amounts = valuesStr.split(Regex("\\s+"))
                    .filter { it.isNotEmpty() }
                    .mapNotNull { it.toIntOrNull() }
                
                if (amounts.isNotEmpty()) {
                    pendingData.getOrPut(counterName) { mutableListOf() }
                        .addAll(amounts.map { timestamp to it })
                    summary[counterName] = (summary[counterName] ?: 0) + amounts.size
                }
            } else {
                // This is not a data line, so it must be a counter name.
                currentCounterName = line
            }
        }

        if (pendingData.isEmpty()) throw Exception("No valid data found in import text.")

        // Apply imported data
        for ((name, newEntries) in pendingData) {
            var counter = counterDao.getCounterByName(name)
            if (counter == null) {
                val newId = counterDao.insertCounter(Counter(name = name))
                counter = counterDao.getCounterById(newId)
            }
            
            if (counter != null) {
                val existingLogs = eventLogDao.getAllLogsForCounterAsc(counter.id)
                val allEntries = (existingLogs.map { it.timestamp to it.amountChanged } + newEntries)
                    .sortedBy { it.first }
                
                var runningCount = 0
                val updatedLogs = allEntries.map { (ts, amount) ->
                    runningCount += amount
                    EventLog(
                        counterId = counter!!.id,
                        timestamp = ts,
                        amountChanged = amount,
                        resultingCount = runningCount
                    )
                }
                
                // Clear and replace logs for this counter to ensure consistency
                eventLogDao.replaceLogsAndUpdateCounter(updatedLogs, counter.id, counter.copy(currentCount = runningCount), counterDao)
            }
        }
        return summary
    }
}
