package com.svyatoslav.mlapp.data.source

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// RemoteDataSource.kt
class RemoteDataSource(private val client: OkHttpClient) {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.example.com")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    //private val service = retrofit.create(ApiService::class.java)

//    suspend fun submit(text: String, ner: Boolean): String =
//        service.postText(RequestBody(text, ner))
}

// Настройка OkHttpClient с Certificate Pinning
fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .certificatePinner(
        CertificatePinner.Builder()
            .add("api.example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build()
    )
    .build()