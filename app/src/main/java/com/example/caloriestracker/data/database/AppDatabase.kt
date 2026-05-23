package com.example.caloriestracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        EntryEntity::class,
        TagEntity::class,
        EntryTagCrossRef::class,
        AttachmentEntity::class,
        EntryFtsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
}
