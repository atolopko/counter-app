package com.example.counterapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Counter::class, EventLog::class], version = 2, exportSchema = true)
abstract class CounterDatabase : RoomDatabase() {
    abstract fun counterDao(): CounterDao
    abstract fun eventLogDao(): EventLogDao

    companion object {
        @Volatile
        private var INSTANCE: CounterDatabase? = null

        fun getDatabase(context: Context): CounterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CounterDatabase::class.java,
                    "counter_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
