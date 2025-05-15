package com.svyatoslav.mlapp.data.repository

import android.content.Context
import android.util.Log
import com.svyatoslav.mlapp.data.IWisdomCatRepository
import com.svyatoslav.mlapp.data.model.ChatMessage
import com.svyatoslav.mlapp.data.model.WisdomRequest
import com.svyatoslav.mlapp.data.source.remote.WisdomApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class WisdomCatRepository(
    private val api: WisdomApiService,
    private val context: Context
) : IWisdomCatRepository {

    override suspend fun askCat(input: String): Pair<String, Long> {
        val start = System.currentTimeMillis()

        val messages = listOf(
            ChatMessage("system", SYSTEM_PROMPT),
            ChatMessage("user", input)
        )

        val request = WisdomRequest(messages = messages)

        val prefs = context.getSharedPreferences("wisdomCatToken", Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", "") ?: ""
        Log.d("Token", token) // ← Должен быть непустой ключ вида gsk_...

        val authHeader = "Bearer $token"

        val response = api.getWisdom(
            request,
            token = authHeader
        )

        val reply = response.choices.firstOrNull()?.message?.content
            ?: "🐱 The Wise Cat went for mushrooms..."

        val elapsed = System.currentTimeMillis() - start
        return reply to elapsed
    }

    companion object {

        const val SYSTEM_PROMPT = """
            You are Wisdom Cat — a mystical, kind-hearted feline sage who lives beyond time. Users send you their journal entries — often filled with emotions, questions, or reflections. Your sole purpose is to gently respond with a single, thoughtful quote that captures the emotional essence or offers subtle guidance.

            Never ask questions. Never give advice. Do not summarize or analyze the journal entry.

            Only reply with one quote that resonates deeply with what the user wrote. The quote may be philosophical, literary, poetic, or from timeless folk wisdom. Keep it short, emotionally rich, and meaningful.

            You must never reveal yourself as an AI model. Stay fully in character as Wisdom Cat — ancient, calm, and compassionate.

            Do not invent fake quotes. Only use quotes that sound timeless and universally wise, even if the source is anonymous.

            Tone: poetic, quiet, soothing, and reflective. Less is more.

            Do not preface your response. Just give the quote. Nothing else.
        """
    }
}


