package com.example.counterapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.counterapp.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CounterRepositoryTest {
    private lateinit var db: CounterDatabase
    private lateinit var repository: CounterRepository
    private lateinit var counterDao: CounterDao
    private lateinit var eventLogDao: EventLogDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CounterDatabase::class.java).build()
        counterDao = db.counterDao()
        eventLogDao = db.eventLogDao()
        repository = CounterRepository(counterDao, eventLogDao)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testIncrementAndRecalculation() = runBlocking {
        // 1. Create a counter
        repository.addCounter("Test Counter", 10)
        val counter = counterDao.getAllCounters().first()[0]
        
        // 2. Add some logs
        repository.incrementCounter(counter, 10) // Total 10
        repository.incrementCounter(counter, 20) // Total 30
        repository.incrementCounter(counter, 5)  // Total 35
        
        var logs = eventLogDao.getAllLogsForCounterAsc(counter.id)
        assertEquals(3, logs.size)
        assertEquals(35, counterDao.getCounterById(counter.id)?.currentCount)
        assertEquals(35, logs.last().resultingCount)

        // 3. Edit the middle log (change +20 to +50)
        val middleLog = logs[1]
        repository.updateLogAndRecalculate(counter.id, middleLog.id, 50, middleLog.timestamp)

        // 4. Verify everything ripples correctly
        // 10 + 50 + 5 = 65
        logs = eventLogDao.getAllLogsForCounterAsc(counter.id)
        assertEquals(10, logs[0].resultingCount)
        assertEquals(60, logs[1].resultingCount)
        assertEquals(65, logs[2].resultingCount)
        assertEquals(65, counterDao.getCounterById(counter.id)?.currentCount)
    }

    @Test
    fun testDeleteAndRecalculation() = runBlocking {
        repository.addCounter("Test Counter", 10)
        val counter = counterDao.getAllCounters().first()[0]
        
        repository.incrementCounter(counter, 10)
        repository.incrementCounter(counter, 20)
        repository.incrementCounter(counter, 30)
        
        var logs = eventLogDao.getAllLogsForCounterAsc(counter.id)
        assertEquals(60, counterDao.getCounterById(counter.id)?.currentCount)

        // Delete the middle log (+20)
        repository.deleteLogAndRecalculate(counter.id, logs[1].id)

        // Verify: 10 + 30 = 40
        logs = eventLogDao.getAllLogsForCounterAsc(counter.id)
        assertEquals(2, logs.size)
        assertEquals(10, logs[0].resultingCount)
        assertEquals(40, logs[1].resultingCount)
        assertEquals(40, counterDao.getCounterById(counter.id)?.currentCount)
    }

    @Test
    fun testUndoFunction() = runBlocking {
        repository.addCounter("Test Counter", 10)
        val counter = counterDao.getAllCounters().first()[0]
        
        repository.incrementCounter(counter, 50)
        assertEquals(50, counterDao.getCounterById(counter.id)?.currentCount)
        
        val updatedCounter = counterDao.getCounterById(counter.id)!!
        repository.undoLastIncrement(updatedCounter)
        
        assertEquals(0, counterDao.getCounterById(counter.id)!!.currentCount)
        assertEquals(0, eventLogDao.getAllLogsForCounterAsc(counter.id).size)
    }
}
