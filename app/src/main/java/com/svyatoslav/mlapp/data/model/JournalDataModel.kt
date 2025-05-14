package com.svyatoslav.mlapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_notes")
data class JournalNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val llmReply: String? = null, // Ответ мудрого кота (если есть)
    val isSynced: Boolean = false, // Для будущей синхронизации
    val privacyLevel: Int = 1      // 0 — нет защиты, 1 — с маскированием, 2 — без LLM
)
