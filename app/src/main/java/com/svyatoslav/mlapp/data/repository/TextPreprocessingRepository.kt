package com.svyatoslav.mlapp.data.repository

import android.util.Log
import com.svyatoslav.mlapp.core.IDpNoiser
import com.svyatoslav.mlapp.core.IHMACMasker
import com.svyatoslav.mlapp.core.INerProcessing
import com.svyatoslav.mlapp.core.IRegexDetector
import com.svyatoslav.mlapp.core.ITextPostprocessor
import com.svyatoslav.mlapp.data.ITextPreprocessingRepository
import com.svyatoslav.mlapp.data.ITextPreprocessingRepository.Mode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of ITextPreprocessingRepository.
 * Combines multiple privacy-preserving components into a unified preprocessing pipeline.
 */
class TextPreprocessingRepository(
    private val ner: INerProcessing,         // Named Entity Recognition component
    private val regex: IRegexDetector,       // Regex-based PII detector
    private val dp: IDpNoiser,               // Differential Privacy noiser
    private val post: ITextPostprocessor,    // Merges spans and applies masking
    private val hmac: IHMACMasker            // HMAC-based deterministic masking
) : ITextPreprocessingRepository {

    /**
     * Preprocess the input text based on the selected mode.
     *
     * @param text Input string that may contain PII.
     * @param mode Defines the processing mode: FAST (lightweight) or SECURE (strong privacy).
     * @return A pair of masked text and the total time taken for processing (ms).
     */
    override suspend fun preprocess(text: String, mode: Mode): Pair<String, Long> =
        withContext(Dispatchers.IO) {

            // Step 1: Detect spans using regex rules
            val spansFromRegex = regex.detect(text)
            Log.d("REPO", spansFromRegex.toString())

            // Step 2: Apply NER to get tags and token features
            val (feature, tags, nerLatency) = ner.predictTags(text)

            // Step 3: Merge regex spans with NER output and apply initial masking
            val rawMasked = post.mergeAndMask(
                tokens = feature,
                tags = tags,
                regexSpans = spansFromRegex,
                id2label = ner.id2label
            )

            // Step 4: Depending on mode, apply further privacy transformations
            val finalMasked = when (mode) {
                Mode.FAST -> rawMasked                             // Basic masking only
                Mode.SECURE -> dp.addNoise(hmac.mask(rawMasked))   // HMAC + differential privacy
                Mode.NONE -> {
                    return@withContext text to 0L
                }
            }

            // Step 5: Compute total latency (NER + optional DP latency)
            val totalLatency = nerLatency + if (mode == Mode.SECURE) dp.lastLatencyMs else 0L

            // Return result with timing
            return@withContext finalMasked to totalLatency
        }
}
