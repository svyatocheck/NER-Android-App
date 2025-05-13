package com.svyatoslav.mlapp.core

interface IHMACMasker {

    fun maskToken(token: String): String

    fun mask(string: String): String
}