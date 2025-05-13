package com.svyatoslav.mlapp.data.repository

import com.svyatoslav.mlapp.data.source.RemoteDataSource

class TextRepository(
    private val dataSource: RemoteDataSource
) {
//    suspend fun sendText(text: String, ner: Boolean): String =
//        dataSource.submit(text, ner)
}
