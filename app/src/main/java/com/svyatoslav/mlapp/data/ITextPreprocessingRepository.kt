package com.svyatoslav.mlapp.data

interface ITextPreprocessingRepository {

    suspend fun preprocess(text: String, mode: Mode = Mode.FAST): Pair<String, Long>

    enum class Mode { FAST, SECURE, NONE}
}