package com.svyatoslav.mlapp.data.model

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: ChatMessage
)


data class ChatMessage(
    val role: String,   // "system", "user", "assistant"
    val content: String
)

data class WisdomRequest(
    val model: String = "llama-3.1-8b-instant",
    val messages: List<ChatMessage>,
    val temperature: Double = 1.0,
    @SerializedName("top_p")
    val topP: Double = 1.0,
    @SerializedName("max_completion_tokens")
    val maxTokens: Int = 1024,
    val stream: Boolean = false,
    val stop: String? = null
)
