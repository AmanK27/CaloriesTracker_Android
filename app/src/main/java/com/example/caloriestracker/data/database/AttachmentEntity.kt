package com.example.caloriestracker.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["entryId"])]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val attId: Long = 0,
    val entryId: Long, // Changed to Long to match EntryEntity id
    val type: String, // "image", "audio"
    val path: String // local file path URI
)
