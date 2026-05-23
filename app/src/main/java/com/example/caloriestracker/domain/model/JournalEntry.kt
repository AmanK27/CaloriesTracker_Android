package com.example.caloriestracker.domain.model

import java.util.UUID

data class JournalEntry(
    val id: UUID = UUID.randomUUID(),
    val timestamp: Long = System.currentTimeMillis(),
    val content: String = "", // Default to empty string
    val title: String? = null,
    val mood: String? = null,
    val summary: String? = null,
    val sentiment: Float? = null,
    val needsSync: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val attachments: List<Attachment> = emptyList()
)
