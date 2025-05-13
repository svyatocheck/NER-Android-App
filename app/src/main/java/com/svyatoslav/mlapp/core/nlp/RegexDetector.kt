package com.svyatoslav.mlapp.core.nlp

import android.util.Log
import com.svyatoslav.mlapp.core.IRegexDetector
import java.util.regex.Pattern

/**
 * RegexDetector provides a way to detect or replace common PII patterns
 * like email, phone, IBAN, card numbers, etc. using regular expressions.
 *
 * @param smartMode If true, enables span-based detection; otherwise supports only replacement.
 */
class RegexDetector(private val smartMode: Boolean = true) : IRegexDetector {

    data class PIISpan(val start: Int, val end: Int, val label: String)

    private val TAG = "RegexDetector"

    /**
     * A map of entity types to a list of patterns, allowing multiple regexes per category.
     */
    private val patterns: Map<String, List<Pattern>> = mapOf(
        "EMAIL" to listOf(
            Pattern.compile(
                "[a-z0-9!#\$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+",
                Pattern.CASE_INSENSITIVE
            )
        ),
        "PHONENUMBER" to listOf(
            Pattern.compile("((?:(?<![\\d-])(?:\\+?\\d{1,3}[-.\\s*]?)?(?:\\(?\\d{3}\\)?[-.\\s*]?)?\\d{3}[-.\\s*]?\\d{4}(?![\\d-])))"),
            Pattern.compile(
                "((?:(?:\\+?1\\s*(?:[.-]\\s*)?)?(?:\\(\\s*(?:[2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9])\\s*\\)|(?:[2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]))\\s*(?:[.-]\\s*)?)?(?:[2-9]1[02-9]|[2-9][02-9]1|[2-9][02-9]{2})\\s*(?:[.-]\\s*)?(?:[0-9]{4})(?:\\s*(?:#|x\\.?|ext\\.?|extension)\\s*(?:\\d+)?))",
                Pattern.CASE_INSENSITIVE
            )
        ),
        "ACCOUNTNUMBER" to listOf(
            Pattern.compile("((?:(?:\\d{4}[- ]?){3}\\d{4}|\\d{15,16}))(?![\\d])")
        ),
        "IBAN" to listOf(
            Pattern.compile("[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}")
        ),
        "ID" to listOf(
            Pattern.compile("\\b\\d{2}\\s?\\d{6}\\b")
        ),
        "DATE" to listOf(
            Pattern.compile(
                "(?:(?<!:)(?<!:\\d)[0-3]?\\d(?:st|nd|rd|th)?\\s+(?:of\\s+)?(?:jan\\.?|january|feb\\.?|february|mar\\.?|march|apr\\.?|april|may|jun\\.?|june|jul\\.?|july|aug\\.?|august|sep\\.?|september|oct\\.?|october|nov\\.?|november|dec\\.?|december)|(?:jan\\.?|january|feb\\.?|february|mar\\.?|march|apr\\.?|april|may|jun\\.?|june|jul\\.?|july|aug\\.?|august|sep\\.?|september|oct\\.?|october|nov\\.?|november|dec\\.?|december)\\s+(?<!:)(?<!:\\d)[0-3]?\\d(?:st|nd|rd|th)?)(?:,)?\\s*(?:\\d{4})?|[0-3]?\\d[-./][0-3]?\\d[-./]\\d{2,4}",
                Pattern.CASE_INSENSITIVE
            )
        ),
        "TIME" to listOf(
            Pattern.compile(
                "\\d{1,2}:\\d{2} ?(?:[ap]\\.?m\\.?)?|\\d[ap]\\.?m\\.?", Pattern.CASE_INSENSITIVE
            )
        ),
        "CRYPTO" to listOf(
            Pattern.compile("(?<![a-km-zA-HJ-NP-Z0-9])[13][a-km-zA-HJ-NP-Z0-9]{26,33}(?![a-km-zA-HJ-NP-Z0-9])")
        ),
        "LOCATION" to listOf(
            Pattern.compile(
                "\\d{1,4} [\\w\\s]{1,20}(?:street|st|avenue|ave|road|rd|highway|hwy|square|sq|trail|trl|drive|dr|court|ct|park|parkway|pkwy|circle|cir|boulevard|blvd)\\W?(?=\\s|\$)",
                Pattern.CASE_INSENSITIVE
            )
        ),
        "POSTCODE" to listOf(
            Pattern.compile("\\b\\d{5}(?:[-\\s]\\d{4})?\\b")
        ),
        "POBOX" to listOf(
            Pattern.compile("P\\.? ?O\\.? Box \\d+", Pattern.CASE_INSENSITIVE)
        ),
        "IP" to listOf(
            Pattern.compile(
                "(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)",
                Pattern.CASE_INSENSITIVE
            ), Pattern.compile(
                "\\s*(?!.*::.*::)(?:(?!:)|:(?=:))(?:[0-9a-f]{0,4}(?:(?<=::)|(?<!::):)){6}(?:[0-9a-f]{0,4}(?:(?<=::)|(?<!::):)[0-9a-f]{0,4}(?:(?<=::)|(?<!:)|(?<=:)(?<!::):)|(?:25[0-4]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(?:\\.(?:25[0-4]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})\\s*",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL or Pattern.COMMENTS
            )
        ),
        "URL" to listOf(
            // С протоколом http/https/ftp
            Pattern.compile(
                "\\b(?:https?|ftp)://[\\w\\-?=%.]+(?:\\.[\\w\\-?=%.]+)*(?:/[\\w\\-?=%.]*)*\\b",
                Pattern.CASE_INSENSITIVE
            ),
            // Просто домен вида example.com (или sub.example.co.uk)
            Pattern.compile(
                "\\b(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,}\\b",
                Pattern.CASE_INSENSITIVE
            )
        )
    )

    /**
     * Detects all regex-based PII spans in the input text.
     * Requires smartMode = true, otherwise throws error.
     *
     * @param text The raw input text.
     * @return A list of detected PIISpan objects with start/end indices and labels.
     */
    override fun detect(text: String): List<PIISpan> {
        if (!smartMode) error("Use detectAndReplace for non-smart mode")

        val spans = mutableListOf<PIISpan>()
        for ((label, patternList) in patterns) {
            for (pattern in patternList) {
                val matcher = pattern.matcher(text)
                Log.d(TAG, "Trying $label → $pattern against: $text")
                while (matcher.find()) {
                    spans += PIISpan(matcher.start(), matcher.end(), label)
                }
            }
        }
        Log.d(TAG, "Found ${spans.size} regex spans")
        return spans
    }

    /**
     * Performs simple replacement of all matched PII patterns with their corresponding labels.
     * This is a standalone fast replacement mode, useful when NER or span tracking is not required.
     *
     * Example: "My email is me@example.com" → "My email is [EMAIL]"
     *
     * @param input Input string possibly containing PII.
     * @return Transformed string with [LABEL] substitutions.
     */
    override fun detectAndReplace(input: String): String {
        var result = input
        for ((label, patternList) in patterns) {
            for (pattern in patternList) {
                result = pattern.matcher(result).replaceAll("[$label]")
            }
        }
        return result
    }
}
