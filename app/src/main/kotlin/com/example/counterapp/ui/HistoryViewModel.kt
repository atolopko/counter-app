package com.example.counterapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.counterapp.data.Counter
import com.example.counterapp.data.CounterRepository
import com.example.counterapp.data.EventLog
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: CounterRepository,
    private val counterId: Long
) : ViewModel() {
    
    val counter: StateFlow<Counter?> = flow {
        emit(repository.getCounterById(counterId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val logs: StateFlow<List<EventLog>> = repository.getLogsForCounter(counterId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
