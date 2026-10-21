package dev.fsales.storage.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BenchRow::class], version = 1, exportSchema = false)
abstract class BenchDatabase : RoomDatabase() {
    abstract fun dao(): BenchDao
}
