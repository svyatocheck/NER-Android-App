package com.svyatoslav.mlapp.core

import com.svyatoslav.mlapp.data.model.FeatureModel

interface IWordPieceTokenizer {
    fun convert(query: String?, context: String): FeatureModel
}

