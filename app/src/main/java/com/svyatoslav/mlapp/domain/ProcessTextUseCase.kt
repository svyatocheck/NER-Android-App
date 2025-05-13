package com.svyatoslav.mlapp.domain

import com.svyatoslav.mlapp.data.ITextPreprocessingRepository

/**
 * Use case class that delegates text processing to the repository.
 * Acts as a domain-level abstraction between ViewModel and data layer.
 */
class ProcessTextUseCase(
    private val repository: ITextPreprocessingRepository
) {
    /**
     * Invokes the preprocessing operation.
     *
     * @param text The input text to process.
     * @param mode The processing mode (e.g., FAST or SECURE).
     * @return A pair containing the masked text and the time it took to process (in milliseconds).
     */
    suspend operator fun invoke(
        text: String,
        mode: ITextPreprocessingRepository.Mode
    ): Pair<String, Long> {
        return repository.preprocess(text, mode)
    }
}
