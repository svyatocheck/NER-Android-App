package com.svyatoslav.mlapp.core

interface IDpNoiser {
    val lastLatencyMs: Long

    fun addNoise(inputText : String) : String
}