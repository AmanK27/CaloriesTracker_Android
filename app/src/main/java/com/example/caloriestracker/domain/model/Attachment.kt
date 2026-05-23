package com.example.caloriestracker.domain.model

import java.util.UUID

data class Attachment(
    val id: Long = 0,
    val entryId: UUID,
    val type: String, // "image", "audio"
    val path: String // local file path
)
