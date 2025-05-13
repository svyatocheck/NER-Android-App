package com.svyatoslav.mlapp.core

import com.svyatoslav.mlapp.core.nlp.RegexDetector.PIISpan

interface IRegexDetector {

    fun detectAndReplace(inputText: String): String

    fun detect(text: String): List<PIISpan>
}