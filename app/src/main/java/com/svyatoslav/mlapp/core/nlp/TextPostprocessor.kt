package com.svyatoslav.mlapp.core.nlp

import android.util.Log
import com.svyatoslav.mlapp.core.ITextPostprocessor
import com.svyatoslav.mlapp.core.nlp.RegexDetector.PIISpan
import com.svyatoslav.mlapp.data.model.FeatureModel

/**
 * Defines a centralized enum of supported PII entity types and their placeholder formats.
 * Used across the masking pipeline to ensure consistent label replacement.
 */
object PIILabels {

    /**
     * Enum of known entity types and their associated standardized placeholders.
     */
    enum class EntityType(val tag: String) {
        FIRST_NAME("[GIVENNAME]"),
        MIDDLE_NAME("[GIVENNAME]"),
        LAST_NAME("[LASTNAME]"),
        EMAIL("[EMAIL]"),
        PHONENUMBER("[PHONENUMBER]"),
        ID("[ID]"),
        PASSPORT("[ID]"),
        IBAN("[IBAN]"),
        ACCOUNTNUMBER("[MASKEDNUMBER]"),
        CRYPTO("[CRYPTO]"),
        USERNAME("[USERNAME]"),
        CITY("[LOCATION]"),
        LOCATION("[LOCATION]"),
        POSTCODE("[POSTCODE]"),
        POBOX("[POBOX]"),
        IP("[IP]"),
        URL("[URL]"),
        DATE("[DATE]"),
        TIME("[TIME]")
    }

    /**
     * Precomputed map from enum name (e.g., "EMAIL") to its placeholder value (e.g., "[EMAIL]").
     */
    val labelMap: Map<String, String> = EntityType.entries.associate {
        it.name to it.tag
    }

    /**
     * Returns the standardized placeholder for a given label name.
     * Defaults to [LABEL] if unknown.
     */
    fun getPlaceholder(label: String): String {
        return labelMap[label.uppercase()] ?: "[$label]"
    }
}

/**
 * Combines the outputs from NER tagging and regex detection,
 * masks detected PII tokens with standardized labels,
 * and applies final formatting cleanup to produce user-facing text.
 */
class TextPostprocessor : ITextPostprocessor {
    /**
     * Final cleanup of the masked string.
     * - Removes extra whitespace
     * - Fixes spacing around punctuation
     * - Collapses multiple consecutive [LABEL]s into one
     */
    override fun finalClean(input: String): String {
        return input
            // Обрамляем метки пробелами
            .replace(Regex("\\[(\\w+)]"), " [$1] ")
            // Сводим множественные пробелы в один
            .replace("\\s+".toRegex(), " ")
            // Убираем пробел перед пунктуацией, кроме [ и ]
            .replace(Regex("\\s+([\\p{Punct}&&[^\\[\\]]])"), "$1")
            // Нормализуем подряд идущие метки [A][A] → [A]
            // Схлопываем одинаковые метки, разделённые любым количеством пробелов
            .replace(Regex("(\\[\\w+])(?:\\s+\\1)+")) { m ->
                m.groupValues[1]
            }

            .trim()
    }


    /**
     * Merges NER and regex spans to create a final masked version of the text.
     *
     * @param tokens Tokenized input with mapping to original words.
     * @param tags Predicted tag indices from the NER model.
     * @param regexSpans Detected spans from regex rules.
     * @param id2label Mapping from tag ID to NER label string.
     * @return Clean, masked string suitable for display or downstream use.
     */
    override fun mergeAndMask(
        tokens: FeatureModel,
        tags: IntArray,
        regexSpans: List<PIISpan>,
        id2label: Map<Int, String>,
    ): String {
        val sb = StringBuilder()
        val maskedIndices = mutableSetOf<Int>()

        // Convert regex spans to token indices (based on character offsets)
        val tokenSpans = tokens.tokenToOrigMap
        val tokenOffsetsToMask = tokenSpans.filter { (_, origIdx) ->
            regexSpans.any { span ->
                val tokenText = tokens.origTokens.getOrNull(origIdx) ?: return@any false
                val offset = tokens.origTokens.subList(0, origIdx).joinToString(" ").length
                span.start in offset..(offset + tokenText.length)
            }
        }.keys
        maskedIndices.addAll(tokenOffsetsToMask)

        var prevOrigIdx = -1

        // Iterate through each token and decide whether to mask or keep
        for ((tokenIdx, origIdx) in tokenSpans) {
            val tagId = tags.getOrElse(tokenIdx) { 0 }
            val label = id2label[tagId] ?: "O"
            val isNER = label.startsWith("B-") || label.startsWith("I-")

            // Try to resolve the label either from NER or from regex match
            val regexLabel = regexSpans.find { span ->
                val token = tokens.origTokens.getOrNull(origIdx) ?: ""
                val offset = tokens.origTokens.subList(0, origIdx).joinToString(" ").length
                span.start in offset..(offset + token.length)
            }?.label

            val replLabel = regexLabel ?: if (isNER) {
                label.removePrefix("B-").removePrefix("I-")
            } else {
                null
            }

            // If it's a PII token and hasn't been masked in this position yet
            if (replLabel != null && origIdx != prevOrigIdx) {
                val repl = PIILabels.getPlaceholder(replLabel)
                if (sb.isNotEmpty() && !sb.last().isWhitespace() && sb.last() != '\n') sb.append(" ")
                sb.append(repl)
            }
            // If it's a non-PII token and not in regex-masked list
            else if (!maskedIndices.contains(tokenIdx) && origIdx != prevOrigIdx) {
                val tok = tokens.origTokens.getOrNull(origIdx)?.trim() ?: continue
                if (tok.isNotEmpty()) {
                    if (sb.isNotEmpty() && !sb.last().isWhitespace()) sb.append(" ")
                    sb.append(tok)
                }
            }

            prevOrigIdx = origIdx
        }

        Log.d("POST", sb.toString())
        // Apply final cleanup to the composed string
        return finalClean(sb.toString())
    }
}
