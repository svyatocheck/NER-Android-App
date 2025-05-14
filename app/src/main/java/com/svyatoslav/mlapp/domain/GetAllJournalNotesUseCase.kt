package com.svyatoslav.mlapp.domain

import androidx.lifecycle.map
import com.svyatoslav.mlapp.data.IJournalRepository
import com.svyatoslav.mlapp.domain.model.JournalNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetAllJournalNotesUseCase(
    private val repository: IJournalRepository
) {
    operator fun invoke(): Flow<List<JournalNote>> {
        return repository.getAllNotes().map { list ->
            list.map {
                JournalNote(
                    id = it.id,
                    title = it.title,
                    content = it.content,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    llmReply = it.llmReply,
                    isSynced = it.isSynced,
                    privacyLevel = it.privacyLevel
                )
            }
        } as Flow<List<JournalNote>>
    }
}