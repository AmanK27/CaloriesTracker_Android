package com.example.caloriestracker.data.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class EntryWithDetails(
    @Embedded val entry: EntryEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "entryId"
    )
    val attachments: List<AttachmentEntity>,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "tagId",
        associateBy = Junction(
            value = EntryTagCrossRef::class,
            parentColumn = "entryId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)
