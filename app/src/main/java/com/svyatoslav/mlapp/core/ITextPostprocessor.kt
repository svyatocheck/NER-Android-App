package com.svyatoslav.mlapp.core

import com.svyatoslav.mlapp.core.nlp.RegexDetector.PIISpan
import com.svyatoslav.mlapp.data.model.FeatureModel

interface ITextPostprocessor {

    fun finalClean(input: String): String

    fun mergeAndMask(
        tokens: FeatureModel,
        tags: IntArray,
        regexSpans: List<PIISpan>,
        id2label: Map<Int, String>
    ): String
}
