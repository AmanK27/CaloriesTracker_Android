package com.example.caloriestracker.domain.repository

import com.example.caloriestracker.domain.model.JournalEntry
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface JournalRepository {
    fun getEntries(): Flow<List<JournalEntry>>
    fun getEntryById(id: UUID): Flow<JournalEntry?>
    suspend fun saveEntry(entry: JournalEntry)
    suspend fun deleteEntry(entry: JournalEntry)
    fun searchEntries(query: String): Flow<List<JournalEntry>>
}
