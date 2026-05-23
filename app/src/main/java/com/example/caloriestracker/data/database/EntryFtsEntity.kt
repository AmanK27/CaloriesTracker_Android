package com.example.caloriestracker.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries_fts")
data class EntryFtsEntity(
    @PrimaryKey val rowid: Long,
    val title: String?,
    val content: String
)
