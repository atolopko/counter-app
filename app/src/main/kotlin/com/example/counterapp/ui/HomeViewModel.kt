package com.example.counterapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.counterapp.data.Counter
import com.example.counterapp.data.CounterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class CounterUiModel(
    val counter: Counter,
    val addedToday: Int
)

sealed class ImportStatus {
    object Idle : ImportStatus()
    data class Success(val results: Map<String, Int>) : ImportStatus()
    data class Error(val message: String) : ImportStatus()
}

class HomeViewModel(private val repository: CounterRepository) : ViewModel() {
    private fun getMidnightToday(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    val counters: StateFlow<List<CounterUiModel>> = combine(
        repository.allCounters,
        repository.getTodaySums(getMidnightToday())
    ) { allCounters, todaySums ->
        val sumsMap = todaySums.associate { it.counterId to it.total }
        allCounters.map { counter ->
            CounterUiModel(
                counter = counter,
                addedToday = sumsMap[counter.id] ?: 0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCounter(name: String, amount: Int) {
        viewModelScope.launch {
            repository.addCounter(name, amount)
        }
    }

    fun incrementCounter(counter: Counter, amount: Int) {
        viewModelScope.launch {
            repository.incrementCounter(counter, amount)
        }
    }

    fun updateCounter(counter: Counter) {
        viewModelScope.launch {
            repository.updateCounter(counter)
        }
    }

    fun undoLastIncrement(counter: Counter) {
        viewModelScope.launch {
            repository.undoLastIncrement(counter)
        }
    }

    fun deleteCounter(counter: Counter) {
        viewModelScope.launch {
            repository.deleteCounter(counter)
        }
    }

    private val _importStatus = MutableStateFlow<ImportStatus>(ImportStatus.Idle)
    val importStatus: StateFlow<ImportStatus> = _importStatus

    fun importData(text: String) {
        viewModelScope.launch {
            try {
                val results = repository.importFromText(text)
                _importStatus.value = ImportStatus.Success(results)
            } catch (e: Exception) {
                _importStatus.value = ImportStatus.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun clearImportStatus() {
        _importStatus.value = ImportStatus.Idle
    }
}
