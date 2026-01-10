package com.example.counterapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.counterapp.data.Counter
import com.example.counterapp.data.CounterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: CounterRepository) : ViewModel() {
    val counters: StateFlow<List<Counter>> = repository.allCounters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun deleteCounter(counter: Counter) {
        viewModelScope.launch {
            repository.deleteCounter(counter)
        }
    }
}
