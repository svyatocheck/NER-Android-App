package com.svyatoslav.mlapp.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.svyatoslav.mlapp.data.ITextPreprocessingRepository
import com.svyatoslav.mlapp.domain.ProcessTextUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state model
 */
data class UiState(
    val maskedText: String = "",
    val inferenceTime: Long = 0L,
    val errorMessage: String? = null
)

/**
 * ViewModel for the main fragment of an application.
 * Sends requests for NER and LLM (Temporarily)
 */
class MainViewModel(
    private val processText: ProcessTextUseCase
) : ViewModel() {

    // LiveData for masked text result processing
    val resultLiveData = MutableLiveData<String>()

    // LiveData for latency metric
    val timeLiveData = MutableLiveData<Long>()

    // LiveData for errors showing
    val errorLiveData = MutableLiveData<String?>()

    // StateFlow for saving UI states. Not using for now
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    // Coroutine (async work) job
    private var job: Job? = null

    /**
     * Launch text preprocessing.
     * Based on `NER` parameter, when execute the logic:
     * - FAST: Just `NER` model preprocessing, nothing more
     * - SECURE: Full pipeline - regex + ner + hmac + dp
     */
    fun processText(text: String, ner: Boolean) {
        // avoid multiple tasks at once
        job?.cancel()
        // launch async job, async job will be closed when ViewModel dies
        job = viewModelScope.launch {
            try {
                // TODO: add option to avoid preprocessing
                val mode = if (!ner) ITextPreprocessingRepository.Mode.FAST else ITextPreprocessingRepository.Mode.SECURE

                // Launch preprocessing, get latency and masked text
                val (masked, elapsed) = processText.invoke(text, mode)

                // Place updates on the screen
                resultLiveData.postValue(masked)
                timeLiveData.postValue(elapsed)
                _uiState.update {
                    it.copy(maskedText = masked, inferenceTime = elapsed, errorMessage = null)
                }
            } catch (e: Exception) {
                // Catch errors and show them
                errorLiveData.postValue(e.message)
                _uiState.update {
                    it.copy(errorMessage = e.message)
                }
            }
        }
    }

    /**
     * Reset error message
     */
    fun errorMessageShown() {
        errorLiveData.postValue(null)
        _uiState.update { it.copy(errorMessage = null) }
    }
}
