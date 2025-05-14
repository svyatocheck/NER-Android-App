package com.svyatoslav.mlapp.domain

import com.svyatoslav.mlapp.data.IJournalRepository
import com.svyatoslav.mlapp.data.model.JournalNoteEntity
import com.svyatoslav.mlapp.domain.model.JournalNote

/**
 * Use case class that delegates text processing to the repository.
 * Acts as a domain-level abstraction between ViewModel and data layer.
 */
class UpdateNoteUseCase(
    private val repository: IJournalRepository,
) {
    suspend operator fun invoke(note: JournalNote) {
        repository.saveNote(
            JournalNoteEntity(
                id = note.id,
                title = note.title,
                content = note.content,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt,
                llmReply = note.llmReply,
                isSynced = note.isSynced,
                privacyLevel = note.privacyLevel
            )
        )
    }
}