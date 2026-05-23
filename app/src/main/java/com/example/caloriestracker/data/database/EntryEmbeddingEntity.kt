package com.example.caloriestracker.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EntryEmbeddingEntity(
    @PrimaryKey val entryId: Long,
    val embedding: ByteArray // 384 dimensions * 4 bytes = 1536 bytes
)
