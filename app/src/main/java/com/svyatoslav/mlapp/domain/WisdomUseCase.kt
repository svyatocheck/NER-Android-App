package com.svyatoslav.mlapp.domain

import android.content.Context
import com.svyatoslav.mlapp.data.ITextPreprocessingRepository
import com.svyatoslav.mlapp.data.IWisdomCatRepository
import com.svyatoslav.mlapp.data.repository.TextPreprocessingRepository
import com.svyatoslav.mlapp.domain.model.PrivacyMode

/**
 * Use case class that delegates text processing to the repository.
 * Acts as a domain-level abstraction between ViewModel and data layer.
 */
class WisdomUseCase(
    private val wisdomRepository: IWisdomCatRepository,
    private val preprocessingRepository: ITextPreprocessingRepository,
    private val context: Context
) {
    /**
     * Invokes the preprocessing operation.
     *
     * @param text The input text to process.
     * @param mode The processing mode (e.g., FAST or SECURE).
     * @return A pair containing the masked text and the time it took to process (in milliseconds).
     */
     suspend operator fun invoke(
        text: String
    ): Pair<String, Long> {
        val mode = when (PrivacySettings.getPrivacyMode(context)) {
            PrivacyMode.NONE -> ITextPreprocessingRepository.Mode.NONE
            PrivacyMode.SECURE -> ITextPreprocessingRepository.Mode.FAST
            PrivacyMode.MAXIMUM -> ITextPreprocessingRepository.Mode.SECURE
        }

        return if (mode != ITextPreprocessingRepository.Mode.NONE) {
            val prep = preprocessingRepository.preprocess(text, mode)
            wisdomRepository.askCat(prep.first)
         } else {
             wisdomRepository.askCat(text)
         }

    }
}
