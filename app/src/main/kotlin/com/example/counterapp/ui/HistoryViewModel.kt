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
    val count: Int
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

    val dailyStats: StateFlow<List<DailyAggregation>> = combine(logs, _selectedRange) { logs, range ->
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        
        val maxLogDate = if (logs.isEmpty()) today 
        else Instant.ofEpochMilli(logs.maxOf { it.timestamp }).atZone(zoneId).toLocalDate()
        
        val endDate = if (maxLogDate.isAfter(today)) maxLogDate else today

        val startDate = when (range) {
            HistoryRange.LAST_7_DAYS -> today.minusDays(6)
            HistoryRange.LAST_30_DAYS -> today.minusDays(29)
            HistoryRange.YTD -> today.withDayOfYear(1)
            HistoryRange.ALL -> {
                if (logs.isEmpty()) today
                else {
                    Instant.ofEpochMilli(logs.minOf { it.timestamp })
                        .atZone(zoneId)
                        .toLocalDate()
                }
            }
        }

        val dayLogs = logs.groupBy {
            Instant.ofEpochMilli(it.timestamp)
                .atZone(zoneId)
                .toLocalDate()
        }

        val result = mutableListOf<DailyAggregation>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            val sum = dayLogs[current]?.sumOf { it.amountChanged } ?: 0
            result.add(DailyAggregation(current, sum))
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
