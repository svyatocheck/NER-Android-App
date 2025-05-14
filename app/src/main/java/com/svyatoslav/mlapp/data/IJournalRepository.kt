package com.svyatoslav.mlapp.data

import androidx.lifecycle.LiveData
import com.svyatoslav.mlapp.data.model.JournalNoteEntity
import kotlinx.coroutines.flow.Flow

interface IJournalRepository {

    fun getAllNotes(): Flow<List<JournalNoteEntity>>

    suspend fun createNewNote(): JournalNoteEntity

    suspend fun updateNote(model: JournalNoteEntity)

    suspend fun getNote(id: Long): JournalNoteEntity?

    suspend fun saveNote(note: JournalNoteEntity)

    suspend fun deleteNote(note: JournalNoteEntity)

}