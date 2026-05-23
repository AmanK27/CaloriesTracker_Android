package com.example.caloriestracker.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entries",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String, // UUID generated locally for sync
    val timestamp: Long,
    val content: String,
    val title: String?,
    val mood: String?,
    val summary: String?,
    val sentiment: Float?,
    val needsSync: Boolean,
    val lastModified: Long
)
