package com.svyatoslav.mlapp.core.nlp

import com.svyatoslav.mlapp.core.IDpNoiser

/**
 * A simple noising component that simulates Differential Privacy by randomly masking
 * a subset of PII tokens in the format [LABEL].
 *
 * This is a heuristic-based approach, not a formal DP mechanism, but useful for testing privacy-preserving pipelines.
 *
 * @param maskFraction The fraction of PII tags (e.g., [EMAIL], [ID]) to mask.
 * @param detailed If true, replaces with label-aware tags like [MASKED_EMAIL]; otherwise, just [MASKED].
 */
class DpNoiser(
    private val maskFraction: Double = 0.7, // Fraction of tags to mask (e.g., 70%)
    private val detailed: Boolean = false   // Use [MASKED_LABEL] or just [MASKED]
) : IDpNoiser {

    override var lastLatencyMs: Long = 0L // Reserved for compatibility; not used here

    /**
     * Randomly replaces a portion of [LABEL] tags with masked placeholders.
     *
     * Example:
     * Input:  "Hello [EMAIL], your ID is [ID]"
     * Output: "Hello [MASKED], your ID is [MASKED_ID]" (depending on maskFraction and detailed)
     *
     * @param text Input string containing PII labels in brackets.
     * @return Noised version of the string with randomized masking.
     */
    override fun addNoise(text: String): String {
        // Regex to match all bracketed tokens: [EMAIL], [PHONE], etc.
        val regex = "\\[(\\w+)]".toRegex()
        val matches = regex.findAll(text).toList()

        val total = matches.size
        // Randomly shuffle and select a subset of matches to mask
        val toMask = matches.shuffled().take((total * maskFraction).toInt()).toSet()

        val masked = StringBuilder()
        var lastIdx = 0

        // Iterate through each detected [LABEL] match
        for (match in matches) {
            val range = match.range

            // Append text before this match
            masked.append(text.substring(lastIdx, range.first))

            if (match in toMask) {
                val label = match.groupValues[1]
                // Replace with [MASKED] or [MASKED_LABEL]
                masked.append(if (detailed) "[MASKED_$label]" else "[MASKED]")
            } else {
                // Keep the original tag unchanged
                masked.append(match.value)
            }

            lastIdx = range.last + 1
        }

        // Append any remaining text after the last match
        masked.append(text.substring(lastIdx))
        return masked.toString()
    }
}
