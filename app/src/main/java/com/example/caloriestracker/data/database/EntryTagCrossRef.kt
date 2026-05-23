package com.example.caloriestracker.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "entry_tags",
    primaryKeys = ["entryId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["tagId"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["entryId"]),
        Index(value = ["tagId"])
    ]
)
data class EntryTagCrossRef(
    val entryId: Long, // Changed to Long to match EntryEntity id
    val tagId: Int
)
