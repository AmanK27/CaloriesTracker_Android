package com.example.caloriestracker.data.repository

import com.example.caloriestracker.data.database.*
import com.example.caloriestracker.domain.model.Attachment
import com.example.caloriestracker.domain.model.JournalEntry
import com.example.caloriestracker.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class JournalRepositoryImpl @Inject constructor(
    private val journalDao: JournalDao
) : JournalRepository {

    override fun getEntries(): Flow<List<JournalEntry>> {
        return journalDao.getEntries().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getEntryById(id: UUID): Flow<JournalEntry?> {
        return journalDao.getEntryByUuid(id.toString()).map { it?.toDomain() }
    }

    override suspend fun saveEntry(entry: JournalEntry) {
        val uuidString = entry.id.toString()
        // Check if entry already exists locally to find its auto-generated long ID
        val existingDetails = journalDao.getEntryByUuid(uuidString).firstOrNull()
        val dbId = existingDetails?.entry?.id ?: 0L

        val entryEntity = EntryEntity(
            id = dbId,
            uuid = uuidString,
            timestamp = entry.timestamp,
            content = entry.content,
            title = entry.title,
            mood = entry.mood,
            summary = entry.summary,
            sentiment = entry.sentiment,
            needsSync = entry.needsSync,
            lastModified = entry.lastModified
        )

        // Insert or update EntryEntity
        val finalDbId = if (dbId == 0L) {
            journalDao.insertEntry(entryEntity)
        } else {
            journalDao.updateEntry(entryEntity)
            dbId
        }

        // Handle Tags
        journalDao.deleteEntryTagsByEntryId(finalDbId)
        entry.tags.forEach { tagName ->
            val trimmedName = tagName.trim().lowercase()
            if (trimmedName.isNotEmpty()) {
                var tagEntity = journalDao.getTagByName(trimmedName)
                val tagId = if (tagEntity == null) {
                    journalDao.insertTag(TagEntity(name = trimmedName))
                } else {
                    tagEntity.tagId.toLong()
                }
                if (tagId > 0) {
                    journalDao.insertEntryTag(EntryTagCrossRef(entryId = finalDbId, tagId = tagId.toInt()))
                }
            }
        }

        // Handle Attachments
        journalDao.deleteAttachmentsByEntryId(finalDbId)
        entry.attachments.forEach { attachment ->
            journalDao.insertAttachment(
                AttachmentEntity(
                    entryId = finalDbId,
                    type = attachment.type,
                    path = attachment.path
                )
            )
        }

        // Explicitly update SQLite FTS5 table
        journalDao.insertFtsEntry(
            rowid = finalDbId,
            title = entry.title,
            content = entry.content
        )
    }

    override suspend fun deleteEntry(entry: JournalEntry) {
        val existingDetails = journalDao.getEntryByUuid(entry.id.toString()).firstOrNull()
        existingDetails?.entry?.let {
            journalDao.deleteEntry(it)
            journalDao.deleteFtsEntry(it.id)
        }
    }

    override fun searchEntries(query: String): Flow<List<JournalEntry>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return getEntries()
        }
        // SQLite FTS5 matching query formulation (escaping and wrapping in wildcard matches if needed)
        val formattedQuery = if (!cleanQuery.endsWith("*")) "$cleanQuery*" else cleanQuery
        return journalDao.searchEntriesLexical(formattedQuery).map { list ->
            list.map { it.toDomain() }
        }
    }

    // Mapper helper extensions
    private fun EntryWithDetails.toDomain(): JournalEntry {
        return JournalEntry(
            id = UUID.fromString(this.entry.uuid),
            timestamp = this.entry.timestamp,
            content = this.entry.content,
            title = this.entry.title,
            mood = this.entry.mood,
            summary = this.entry.summary,
            sentiment = this.entry.sentiment,
            needsSync = this.entry.needsSync,
            lastModified = this.entry.lastModified,
            tags = this.tags.map { it.name },
            attachments = this.attachments.map {
                Attachment(
                    id = it.attId,
                    entryId = UUID.fromString(this.entry.uuid),
                    type = it.type,
                    path = it.path
                )
            }
        )
    }
}
