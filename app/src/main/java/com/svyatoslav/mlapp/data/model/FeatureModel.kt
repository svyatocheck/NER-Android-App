package com.svyatoslav.mlapp.data.model

/**
 * Data class representing all the tensors and mappings needed for model inference.
 */
data class FeatureModel(
    val inputIds: List<Int>,
    val inputMask: List<Int>,
    val segmentIds: List<Int>,
    val origTokens: List<String>,
    val tokenToOrigMap: Map<Int, Int>
)