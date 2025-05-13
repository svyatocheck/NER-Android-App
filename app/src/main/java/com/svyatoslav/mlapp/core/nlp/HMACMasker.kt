package com.svyatoslav.mlapp.core.nlp

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import com.svyatoslav.mlapp.core.IHMACMasker

/**
 * HMAC-based masker that replaces PII tags like [EMAIL] or [ID] with deterministic hashed equivalents.
 *
 * This is useful in "secure" processing modes, where tags should be anonymized but consistently identifiable
 * across different sessions without revealing the actual label.
 *
 * For example, [EMAIL] becomes [HMAC_a1b2c3d4].
 *
 * @param secretKey A shared secret used to generate the HMAC-SHA256 hash.
 */
class HMACMasker(secretKey: String) : IHMACMasker {
    private val mac: Mac

    init {
        // Initialize HMAC with the provided secret key
        val keySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
        mac = Mac.getInstance("HmacSHA256")
        mac.init(keySpec)
    }

    /**
     * Hashes a single token (e.g., "EMAIL") using HMAC-SHA256 and returns a shortened, encoded string.
     *
     * Example: "EMAIL" → "[HMAC_a1b2c3d4]"
     */
    override fun maskToken(token: String): String {
        val hash = mac.doFinal(token.toByteArray())
        val encoded = Base64.encodeToString(hash, Base64.NO_WRAP)
        return "[HMAC_${encoded.take(8)}]"  // Truncate to 8 characters for readability
    }

    /**
     * Applies HMAC masking to all tokens in the format [TOKEN] within the input text.
     * For example, "Hello [EMAIL]!" → "Hello [HMAC_a1b2c3d4]!"
     *
     * @param input Input string containing tags to mask.
     * @return Transformed string with HMAC-masked tokens.
     */
    override fun mask(input: String): String {
        val regex = "\\[(\\w+)]".toRegex()

        // Replace each tag [LABEL] with its HMAC-based equivalent
        return regex.replace(input) { matchResult ->
            val token = matchResult.groupValues[1]
            maskToken(token)
        }
    }
}
