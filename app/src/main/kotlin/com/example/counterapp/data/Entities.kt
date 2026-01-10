package com.example.counterapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "counters")
data class Counter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currentCount: Int = 0,
    val lastIncrementAmount: Int = 10
)

@Entity(tableName = "event_logs")
data class EventLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val counterId: Long,
    val timestamp: Long,
    val amountChanged: Int,
    val resultingCount: Int
)
