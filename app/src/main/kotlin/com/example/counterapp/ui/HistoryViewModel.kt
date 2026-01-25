package com.example.counterapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.counterapp.data.Counter
import com.example.counterapp.data.CounterRepository
import com.example.counterapp.data.EventLog
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class HistoryRange {
    LAST_7_DAYS, LAST_30_DAYS, YTD, ALL
}

data class DailyAggregation(
    val date: LocalDate,
    val added: Int,
    val cumulative: Int
)

class HistoryViewModel(
    private val repository: CounterRepository,
    private val counterId: Long
) : ViewModel() {
    
    val counter: StateFlow<Counter?> = flow {
        emit(repository.getCounterById(counterId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val logs: StateFlow<List<EventLog>> = repository.getLogsForCounter(counterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedRange = MutableStateFlow(HistoryRange.LAST_7_DAYS)
    val selectedRange: StateFlow<HistoryRange> = _selectedRange

    val dailyStats: StateFlow<List<DailyAggregation>> = combine(logs, _selectedRange) { allLogs, range ->
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val sortedLogs = allLogs.sortedBy { it.timestamp }
        
        val maxLogDate = if (sortedLogs.isEmpty()) today 
        else Instant.ofEpochMilli(sortedLogs.maxOf { it.timestamp }).atZone(zoneId).toLocalDate()
        
        val endDate = if (maxLogDate.isAfter(today)) maxLogDate else today

        val startDate = when (range) {
            HistoryRange.LAST_7_DAYS -> today.minusDays(6)
            HistoryRange.LAST_30_DAYS -> today.minusDays(29)
            HistoryRange.YTD -> today.withDayOfYear(1)
            HistoryRange.ALL -> {
                if (sortedLogs.isEmpty()) today
                else {
                    Instant.ofEpochMilli(sortedLogs.minOf { it.timestamp })
                        .atZone(zoneId)
                        .toLocalDate()
                }
            }
        }

        // Map of Date -> Total count at end of day and sum of increments that day
        val dailySum = sortedLogs.groupBy {
            Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
        }.mapValues { (_, logs) ->
            logs.sumOf { it.amountChanged }
        }
        
        // Find total as of end of day for each day there's a log
        val dailyClosingTotal = sortedLogs.groupBy {
            Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
        }.mapValues { (_, logs) ->
            logs.last().resultingCount
        }

        val result = mutableListOf<DailyAggregation>()
        var current = startDate
        var lastTotal = 0
        
        // Calculate the starting total (total before startDate)
        val beforeLogs = sortedLogs.filter { 
            Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate().isBefore(startDate)
        }
        if (beforeLogs.isNotEmpty()) {
            lastTotal = beforeLogs.last().resultingCount
        }

        while (!current.isAfter(endDate)) {
            val added = dailySum[current] ?: 0
            val todayTotal = dailyClosingTotal[current] ?: lastTotal
            
            result.add(DailyAggregation(
                date = current,
                added = added,
                cumulative = todayTotal - added
            ))
            
            lastTotal = todayTotal
            current = current.plusDays(1)
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setRange(range: HistoryRange) {
        _selectedRange.value = range
    }

    fun updateLog(logId: Long, newAmount: Int, newTimestamp: Long) {
        viewModelScope.launch {
            repository.updateLogAndRecalculate(counterId, logId, newAmount, newTimestamp)
        }
    }

    fun deleteLog(logId: Long) {
        viewModelScope.launch {
            repository.deleteLogAndRecalculate(counterId, logId)
        }
    }
}
