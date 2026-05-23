package com.example.caloriestracker.data.repository

import com.example.caloriestracker.data.database.*
import com.example.caloriestracker.data.search.EmbeddingEngine
import com.example.caloriestracker.data.network.AiService
import com.example.caloriestracker.domain.model.Attachment
import com.example.caloriestracker.domain.model.JournalEntry
import com.example.caloriestracker.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

class JournalRepositoryImpl @Inject constructor(
    private val journalDao: JournalDao,
    private val embeddingGenerator: EmbeddingEngine,
    private val aiService: dagger.Lazy<AiService>
) : JournalRepository {

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)


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

        // Generate and save local embedding vector
        try {
            val textToEmbed = "${entry.title ?: ""} ${entry.content}".trim()
            if (textToEmbed.isNotEmpty()) {
                val vector = embeddingGenerator.generateEmbedding(textToEmbed)
                val buffer = java.nio.ByteBuffer.allocate(vector.size * 4)
                buffer.asFloatBuffer().put(vector)
                
                journalDao.insertEmbedding(
                    EntryEmbeddingEntity(
                        entryId = finalDbId,
                        embedding = buffer.array()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Run AI analysis (summary, sentiment, tags) in the background if content is not empty
        val textToAnalyze = entry.content.trim()
        if (textToAnalyze.isNotEmpty()) {
            scope.launch {
                try {
                    val currentDetails = journalDao.getEntryByUuid(uuidString).firstOrNull()
                    val currentEntry = currentDetails?.entry
                    
                    val needsAiAnalysis = currentEntry == null || 
                            currentEntry.summary == null || 
                            currentEntry.sentiment == null || 
                            currentEntry.content != textToAnalyze

                    if (needsAiAnalysis) {
                        val result = aiService.get().generateSummaryAndSentiment(textToAnalyze)
                        val suggestedTags = aiService.get().generateSmartTags(textToAnalyze)

                        if (result != null || suggestedTags != null) {
                            val latestDetails = journalDao.getEntryByUuid(uuidString).firstOrNull()
                            if (latestDetails != null) {
                                val entryToUpdate = latestDetails.entry.copy(
                                    summary = result?.summary ?: latestDetails.entry.summary,
                                    sentiment = result?.sentiment ?: latestDetails.entry.sentiment
                                )
                                journalDao.updateEntry(entryToUpdate)

                                suggestedTags?.forEach { tag ->
                                    val cleanTag = tag.trim().lowercase()
                                    if (cleanTag.isNotEmpty()) {
                                        var tagEntity = journalDao.getTagByName(cleanTag)
                                        val tagId = if (tagEntity == null) {
                                            journalDao.insertTag(TagEntity(name = cleanTag))
                                        } else {
                                            tagEntity.tagId.toLong()
                                        }
                                        if (tagId > 0) {
                                            journalDao.insertEntryTag(
                                                EntryTagCrossRef(
                                                    entryId = latestDetails.entry.id,
                                                    tagId = tagId.toInt()
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
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

        val ftsQuery = if (!cleanQuery.endsWith("*")) "$cleanQuery*" else cleanQuery
        val ftsFlow = journalDao.searchEntriesLexical(ftsQuery).map { list -> list.map { it.toDomain() } }

        val semanticFlow = kotlinx.coroutines.flow.flow {
            try {
                val queryVector = embeddingGenerator.generateEmbedding(cleanQuery)
                val storedEmbeddings = journalDao.getAllEmbeddings()
                
                val matchingIds = mutableListOf<Long>()
                for (record in storedEmbeddings) {
                    val buffer = java.nio.ByteBuffer.wrap(record.embedding)
                    val floatBuffer = buffer.asFloatBuffer()
                    val storedVector = FloatArray(floatBuffer.limit())
                    floatBuffer.get(storedVector)

                    var dotProduct = 0f
                    for (i in queryVector.indices) {
                        dotProduct += queryVector[i] * storedVector[i]
                    }

                    // Cosine similarity threshold for relevance
                    if (dotProduct > 0.45f) {
                        matchingIds.add(record.entryId)
                    }
                }

                if (matchingIds.isNotEmpty()) {
                    val entries = journalDao.getEntriesByIds(matchingIds).map { it.toDomain() }
                    emit(entries)
                } else {
                    emit(emptyList())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emit(emptyList())
            }
        }

        return ftsFlow.combine(semanticFlow) { ftsList, semanticList ->
            val mergedMap = mutableMapOf<UUID, JournalEntry>()
            ftsList.forEach { mergedMap[it.id] = it }
            semanticList.forEach { mergedMap[it.id] = it }
            mergedMap.values.sortedByDescending { it.timestamp }
        }
    }

    override suspend fun getSemanticContext(query: String, limit: Int): List<JournalEntry> {
        try {
            val queryVector = embeddingGenerator.generateEmbedding(query)
            val stored = journalDao.getAllEmbeddings()
            
            val scored = stored.map { record ->
                val buffer = java.nio.ByteBuffer.wrap(record.embedding)
                val floatBuffer = buffer.asFloatBuffer()
                val storedVector = FloatArray(floatBuffer.limit())
                floatBuffer.get(storedVector)

                var dotProduct = 0f
                for (i in queryVector.indices) {
                    dotProduct += queryVector[i] * storedVector[i]
                }
                record.entryId to dotProduct
            }.filter { it.second > 0.40f }
             .sortedByDescending { it.second }
             .take(limit)

            if (scored.isEmpty()) return emptyList()

            return journalDao.getEntriesByIds(scored.map { it.first }).map { it.toDomain() }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
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
