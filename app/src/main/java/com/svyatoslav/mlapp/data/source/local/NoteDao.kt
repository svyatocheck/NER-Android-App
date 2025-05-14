package com.svyatoslav.mlapp.data.source.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.svyatoslav.mlapp.data.model.JournalNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM journal_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<JournalNoteEntity>>

    @Query("SELECT * FROM journal_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): JournalNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: JournalNoteEntity): Long

    @Update
    suspend fun update(note: JournalNoteEntity)

    @Delete
    suspend fun delete(note: JournalNoteEntity)
}
