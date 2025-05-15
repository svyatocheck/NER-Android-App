package com.svyatoslav.mlapp.data.source.remote

import com.svyatoslav.mlapp.data.model.ChatResponse
import com.svyatoslav.mlapp.data.model.WisdomRequest
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface WisdomApiService {
    @POST("openai/v1/chat/completions")
    suspend fun getWisdom(
        @Body request: WisdomRequest,
        @Header("Authorization") token: String = ""
    ): ChatResponse
}
