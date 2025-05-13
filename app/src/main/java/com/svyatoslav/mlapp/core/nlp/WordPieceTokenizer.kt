package com.svyatoslav.mlapp.core.nlp

import android.content.Context
import android.util.Log
import com.svyatoslav.mlapp.core.IWordPieceTokenizer
import com.svyatoslav.mlapp.core.models.NERModelPreprocessing
import com.svyatoslav.mlapp.data.model.FeatureModel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.Normalizer
import java.util.Locale

/**
 * Tokenizer that implements WordPiece tokenization for use in BERT-style models.
 * Converts input text into token IDs, masks, and segment identifiers suitable for model input.
 */
class WordPieceTokenizer(
    context: Context,
    vocabAssetPath: String = "vocab.txt",
) : IWordPieceTokenizer {

    private val vocab: Map<String, Int> // Token-to-ID vocabulary loaded from asset file
    private val unkId = 0               // ID for unknown tokens
    private val TAG = "WordPieceTokenizer"

    init {
        // Load the vocabulary file from assets and build a token-to-index map
        val stream = context.assets.open(vocabAssetPath)
        val map = mutableMapOf<String, Int>()
        BufferedReader(InputStreamReader(stream)).useLines { lines ->
            var index = 0
            for (line in lines) {
                val token = line.trim()
                map[token] = index++
            }
        }
        vocab = map
        Log.i("Tokenizer", "Loaded vocab with ${vocab.size} entries")
    }

    /**
     * Convert user input into model-ready token features.
     * Includes special tokens, segment IDs, and alignment information.
     *
     * @param query Optional prefix (e.g., question context).
     * @param context The main text to be tokenized.
     */
    override fun convert(query: String?, context: String): FeatureModel {
        Log.d(TAG, "Raw input: $context")

        // Tokenize query (optional question) and truncate to max length
        val queryTokens = tokenizeText(query ?: "").take(NERModelPreprocessing.MAX_QUERY_LEN)

        // Split original context into tokens by whitespace
        val origTokens = context.trim().split(regex = "\\s+".toRegex())
        val tokenToOrigIndex = mutableListOf<Int>()   // Maps sub-tokens to original token index
        val allDocTokens = mutableListOf<String>()    // All sub-tokens after WordPiece

        // Normalize and tokenize each original word into sub-tokens
        for ((i, token) in origTokens.withIndex()) {
            val normToken = normalize(token)
            val subTokens = tokenizeText(normToken)
            Log.d(TAG, "Word: $token → SubTokens: $subTokens")
            for (subToken in subTokens) {
                tokenToOrigIndex.add(i)
                allDocTokens.add(subToken)
            }
        }

        // Limit context to fit max model input length
        val maxContextLen = NERModelPreprocessing.MAX_SEQ_LEN - queryTokens.size - 3
        val docTokensTrimmed =
            if (allDocTokens.size > maxContextLen) allDocTokens.take(maxContextLen) else allDocTokens

        // Map BERT token index back to original word index
        val tokenToOrigMap = mutableMapOf<Int, Int>()

        // Build final token sequence with special tokens and segment IDs
        val tokens = mutableListOf("[CLS]")
        val segmentIds = mutableListOf(0)

        for ((i, docToken) in docTokensTrimmed.withIndex()) {
            tokens.add(docToken)
            segmentIds.add(0)
            tokenToOrigMap[tokens.size - 1] = tokenToOrigIndex[i]
        }

        tokens.add("[SEP]")
        segmentIds.add(0)

        for (queryToken in queryTokens) {
            tokens.add(queryToken)
            segmentIds.add(1)
        }

        tokens.add("[SEP]")
        segmentIds.add(1)

        // Convert tokens to IDs and construct input mask
        val inputIds = convertTokensToIds(tokens)
        val inputMask = MutableList(inputIds.size) { 1 }

        // Pad input to model's required sequence length
        while (inputIds.size < NERModelPreprocessing.MAX_SEQ_LEN) {
            inputIds.add(0)
            inputMask.add(0)
            segmentIds.add(0)
        }

        Log.d("Tokenizer", "Final Tokens: $tokens")
        Log.d("Tokenizer", "Token IDs: $inputIds")

        // Wrap everything into a feature model for the NER pipeline
        return FeatureModel(inputIds, inputMask, segmentIds, origTokens, tokenToOrigMap)
    }

    /**
     * Normalize input by lowercasing, removing control characters,
     * and applying Unicode normalization (NFKC).
     */
    private fun normalize(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFKC)
            .replace("\\p{C}".toRegex(), "")  // Remove control characters
            .trim()
            .lowercase(Locale.getDefault())
    }

    /**
     * Performs WordPiece tokenization:
     * Splits unknown words into known sub-word units from the vocabulary.
     */
    private fun tokenizeText(text: String): List<String> {
        val out = mutableListOf<String>()
        val words = text.split("\\s+".toRegex())

        for (word in words) {
            var start = 0

            while (start < word.length) {
                var end = word.length
                var match: String? = null

                // Try to find the longest valid subword from the vocabulary
                while (end > start) {
                    val sub = if (start == 0) word.substring(start, end)
                    else "##" + word.substring(start, end)
                    if (vocab.containsKey(sub)) {
                        match = sub
                        break
                    }
                    end--
                }

                if (match != null) {
                    out.add(match)
                    start += if (match.startsWith("##")) match.length - 2 else match.length
                } else {
                    out.add("[UNK]") // Fallback for unknown word
                    break
                }
            }
        }
        return out
    }

    /**
     * Converts a list of tokens into their corresponding IDs using the loaded vocabulary.
     */
    private fun convertTokensToIds(tokens: List<String>): MutableList<Int> {
        return tokens.map { vocab[it] ?: unkId }.toMutableList()
    }
}
