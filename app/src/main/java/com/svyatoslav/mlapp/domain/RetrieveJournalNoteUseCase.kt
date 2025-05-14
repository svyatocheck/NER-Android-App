package com.svyatoslav.mlapp.domain

import com.svyatoslav.mlapp.data.IJournalRepository
import com.svyatoslav.mlapp.domain.model.JournalNote

/**
 * Use case class that delegates text processing to the repository.
 * Acts as a domain-level abstraction between ViewModel and data layer.
 */
class RetrieveJournalNoteUseCase(
    private val repository: IJournalRepository
) {
    suspend operator fun invoke(
        id: Long
    ): JournalNote? {
        return repository.getNote(id)?.let{ it ->
            return@let JournalNote(
                it.id,
                it.title,
                it.content,
                it.createdAt
            )
        }
    }
}

