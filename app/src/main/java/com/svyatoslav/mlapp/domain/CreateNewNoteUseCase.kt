package com.svyatoslav.mlapp.domain

import com.svyatoslav.mlapp.data.IJournalRepository
import com.svyatoslav.mlapp.domain.model.JournalNote

class CreateNewNoteUseCase(
    private val repository: IJournalRepository
) {
    suspend operator fun invoke(): JournalNote {
        val entity = repository.createNewNote()
        return JournalNote(
            id = entity.id,
            title = entity.title,
            content = entity.content,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            llmReply = entity.llmReply,
            isSynced = entity.isSynced,
            privacyLevel = entity.privacyLevel
        )
    }
}
