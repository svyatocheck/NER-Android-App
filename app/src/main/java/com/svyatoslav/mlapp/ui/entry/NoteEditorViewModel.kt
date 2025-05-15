package com.svyatoslav.mlapp.ui.entry

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.svyatoslav.mlapp.data.ITextPreprocessingRepository
import com.svyatoslav.mlapp.domain.CreateNewNoteUseCase
import com.svyatoslav.mlapp.domain.PrivacySettings
import com.svyatoslav.mlapp.domain.RetrieveJournalNoteUseCase
import com.svyatoslav.mlapp.domain.UpdateNoteUseCase
import com.svyatoslav.mlapp.domain.WisdomUseCase
import com.svyatoslav.mlapp.domain.model.JournalNote
import com.svyatoslav.mlapp.domain.model.PrivacyMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NoteEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val retrieveUseCase: RetrieveJournalNoteUseCase,
    private val updateUseCase: UpdateNoteUseCase,
    private val createUseCase: CreateNewNoteUseCase,
    private val wisdomUseCase: WisdomUseCase
) : ViewModel() {

    private val _note = MutableLiveData<JournalNote>()
    val note: LiveData<JournalNote> = _note

    val title = MutableLiveData<String>()
    val content = MutableLiveData<String>()

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status

    private var saveJob: Job? = null

    init {
        val id = savedStateHandle.get<String>("noteId") ?: "-1"
        viewModelScope.launch {
            val loaded = if (id.toLong() >= 0) {
                retrieveUseCase(id.toLong())
            } else {
                _status.value = "Новая заметка"
                createUseCase()
            }
            loaded?.let {
                _note.value = it
                title.value = it.title
                content.value = it.content
            }
        }
    }

    fun updateTitle(text: String) {
        title.value = text
    }

    fun updateContent(text: String) {
        content.value = text
    }

    fun triggerSave() {
        saveJob?.cancel()
        _status.value = "Сохраняется..."
        saveJob = viewModelScope.launch {
            delay(500)
            val current = _note.value ?: return@launch
            val newTitle = title.value.orEmpty()
            val newContent = content.value.orEmpty()

            if (newTitle == current.title && newContent == current.content) {
                _status.value = "Без изменений"
                return@launch
            }

            val updated = current.copy(
                title = newTitle,
                content = newContent,
                updatedAt = System.currentTimeMillis()
            )
            updateUseCase(updated)
            _note.value = updated
            _status.value = "Сохранено"
        }
    }

    // ---------- МУДРЫЙ КОТ -----------

    private val _wisdomReply = MutableLiveData<String>()
    val wisdomReply: LiveData<String> = _wisdomReply

    private val _isLoadingWisdom = MutableLiveData<Boolean>()
    val isLoadingWisdom: LiveData<Boolean> = _isLoadingWisdom

    fun getWisdomFromLlm(question: String) {
        viewModelScope.launch {
            _isLoadingWisdom.value = true
            try {
                val (reply, time) = wisdomUseCase(question)
                _wisdomReply.value = reply
                //timeLiveData.value = time
            } catch (e: Exception) {
                _wisdomReply.value = "Ошибка: ${e.message}"
            } finally {
                _isLoadingWisdom.value = false
            }
        }

    }

//    // --------- Обработка текста -----------
//
//    val resultLiveData = MutableLiveData<String>()
//    val timeLiveData = MutableLiveData<Long>()
//    val errorLiveData = MutableLiveData<String?>()
//
//    private val _uiState = MutableStateFlow(UiState())
//    val uiState: StateFlow<UiState> = _uiState
//
//    private var job: Job? = null
//
//    fun processText(text: String, ner: Boolean) {
//        job?.cancel()
//        job = viewModelScope.launch {
//            try {
//                val (masked, elapsed) = processText.invoke(text, mode)
//                resultLiveData.postValue(masked)
//                timeLiveData.postValue(elapsed)
//                _uiState.update {
//                    it.copy(maskedText = masked, inferenceTime = elapsed, errorMessage = null)
//                }
//            } catch (e: Exception) {
//                errorLiveData.postValue(e.message)
//                _uiState.update {
//                    it.copy(errorMessage = e.message)
//                }
//            }
//        }
//    }

//    fun errorMessageShown() {
//        errorLiveData.postValue(null)
//        _uiState.update { it.copy(errorMessage = null) }
//    }
}

