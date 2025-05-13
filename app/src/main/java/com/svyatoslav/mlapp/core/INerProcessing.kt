package com.svyatoslav.mlapp.core

import com.svyatoslav.mlapp.data.model.FeatureModel

interface INerProcessing {

    val id2label: Map<Int, String>

    fun infer(inputText: String) : Pair<String, Long>

    fun predictTags(inputText: String): Triple<FeatureModel, IntArray, Long>
}