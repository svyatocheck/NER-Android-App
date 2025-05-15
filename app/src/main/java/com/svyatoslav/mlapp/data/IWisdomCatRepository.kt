package com.svyatoslav.mlapp.data

interface IWisdomCatRepository {
    suspend fun askCat(string: String): Pair<String, Long>
}