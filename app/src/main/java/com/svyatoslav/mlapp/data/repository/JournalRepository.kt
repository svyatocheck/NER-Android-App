package com.svyatoslav.mlapp.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import com.svyatoslav.mlapp.data.IJournalRepository
import com.svyatoslav.mlapp.data.model.JournalNoteEntity
import com.svyatoslav.mlapp.data.source.local.NoteDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class JournalRepository(context: Context, private val dao: NoteDao) : IJournalRepository {

    override suspend fun updateNote(model: JournalNoteEntity) {
        dao.update(model.copy(updatedAt = System.currentTimeMillis()))
    }

    override fun getAllNotes(): Flow<List<JournalNoteEntity>> = dao.getAllNotes()

    override suspend fun getNote(id: Long): JournalNoteEntity? = dao.getNoteById(id)

    override suspend fun saveNote(note: JournalNoteEntity) {
        Log.d("Repo","saveNote id=${note.id}, title='${note.title}'")
        if (note.id == 0L) {
            val newId = dao.insert(note)
            Log.d("Repo","  inserted new id=$newId")
        } else {
            dao.update(note)
            Log.d("Repo","  updated id=${note.id}")
        }
    }

    override suspend fun deleteNote(note: JournalNoteEntity) = dao.delete(note)

    override suspend fun createNewNote(): JournalNoteEntity {
        val newNote = JournalNoteEntity(
            title = "",
            content = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newId = dao.insert(newNote)
        return newNote.copy(id = newId)
    }


}
