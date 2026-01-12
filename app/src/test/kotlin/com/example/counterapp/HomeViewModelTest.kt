package com.example.counterapp

import com.example.counterapp.data.Counter
import com.example.counterapp.data.CounterRepository
import com.example.counterapp.ui.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: CounterRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock(CounterRepository::class.java)
        
        val counters = listOf(Counter(id = 1, name = "Test", currentCount = 5))
        `when`(repository.allCounters).thenReturn(flowOf(counters))
        
        viewModel = HomeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `counters flow should emit repository data`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val counters = viewModel.counters.value
        assertEquals(1, counters.size)
        assertEquals("Test", counters[0].name)
        assertEquals(5, counters[0].currentCount)
    }
}
