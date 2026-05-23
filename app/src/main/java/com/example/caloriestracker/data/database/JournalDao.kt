package com.example.caloriestracker.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Transaction
    @Query("SELECT * FROM entries ORDER BY timestamp DESC")
    fun getEntries(): Flow<List<EntryWithDetails>>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id LIMIT 1")
    fun getEntryById(id: Long): Flow<EntryWithDetails?>

    @Transaction
    @Query("SELECT * FROM entries WHERE uuid = :uuid LIMIT 1")
    fun getEntryByUuid(uuid: String): Flow<EntryWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: EntryEntity): Long

    @Update
    suspend fun updateEntry(entry: EntryEntity)

    @Delete
    suspend fun deleteEntry(entry: EntryEntity)

    // Tag Operations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntryTag(entryTag: EntryTagCrossRef)

    @Query("DELETE FROM entry_tags WHERE entryId = :entryId")
    suspend fun deleteEntryTagsByEntryId(entryId: Long)

    // Attachment Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity): Long

    @Query("DELETE FROM attachments WHERE entryId = :entryId")
    suspend fun deleteAttachmentsByEntryId(entryId: Long)

    // Lexical Search via SQLite FTS5
    @Transaction
    @Query("""
        SELECT entries.* FROM entries
        JOIN entries_fts ON entries.id = entries_fts.rowid
        WHERE entries_fts.content MATCH :query OR entries_fts.title MATCH :query
    """)
    fun searchEntriesLexical(query: String): Flow<List<EntryWithDetails>>

    @Query("INSERT OR REPLACE INTO entries_fts(rowid, title, content) VALUES(:rowid, :title, :content)")
    suspend fun insertFtsEntry(rowid: Long, title: String?, content: String)

    @Query("DELETE FROM entries_fts WHERE rowid = :rowid")
    suspend fun deleteFtsEntry(rowid: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: EntryEmbeddingEntity)

    @Query("SELECT * FROM entry_embeddings")
    suspend fun getAllEmbeddings(): List<EntryEmbeddingEntity>

    @Transaction
    @Query("SELECT * FROM entries WHERE id IN (:ids)")
    suspend fun getEntriesByIds(ids: List<Long>): List<EntryWithDetails>
}
