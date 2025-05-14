package com.svyatoslav.mlapp.domain

import com.svyatoslav.mlapp.data.IJournalRepository
import com.svyatoslav.mlapp.data.model.JournalNoteEntity
import com.svyatoslav.mlapp.domain.model.JournalNote

class DeleteNoteUseCase(
    private val repository: IJournalRepository
) {
    suspend operator fun invoke(note: JournalNote) {
        repository.deleteNote(
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
