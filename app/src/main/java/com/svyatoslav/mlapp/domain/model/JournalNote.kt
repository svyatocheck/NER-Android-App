package com.svyatoslav.mlapp.domain.model

data class JournalNote(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val llmReply: String? = null, // Ответ мудрого кота (если есть)
    val isSynced: Boolean = false, // Для будущей синхронизации
    val privacyLevel: Int = 1      // 0 — нет защиты, 1 — с маскированием, 2 — без LLM
)

