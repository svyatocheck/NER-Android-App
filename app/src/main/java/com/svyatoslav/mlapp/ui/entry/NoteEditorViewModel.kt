package com.svyatoslav.mlapp.ui.entry

import android.util.Log
import androidx.lifecycle.*
import com.svyatoslav.mlapp.domain.CreateNewNoteUseCase
import com.svyatoslav.mlapp.domain.RetrieveJournalNoteUseCase
import com.svyatoslav.mlapp.domain.UpdateNoteUseCase
import com.svyatoslav.mlapp.domain.model.JournalNote
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NoteEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val retrieveUseCase: RetrieveJournalNoteUseCase,
    private val updateUseCase: UpdateNoteUseCase,
    private val createUseCase: CreateNewNoteUseCase
) : ViewModel() {

    private val _note   = MutableLiveData<JournalNote>()
    val note: LiveData<JournalNote> = _note

    val title   = MutableLiveData<String>()
    val content = MutableLiveData<String>()

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status

    private var saveJob: Job? = null

    init {
        val id = savedStateHandle.get<String>("noteId") ?: "-1"
        Log.d("VIEW MODEL", "$id")
        viewModelScope.launch {
            val loaded = if (id.toLong() >= 0) {
                retrieveUseCase(id.toLong())
            } else {
                _status.value = "Новая заметка"
                createUseCase()
            }
            loaded?.let {
                _note.value = it
                title.value   = it.title
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
            Log.d("VIEW MODEL", "$_note ${title.value}")
            val current = _note.value ?: return@launch

            val newTitle   = title.value.orEmpty()
            val newContent = content.value.orEmpty()

            if (newTitle == current.title && newContent == current.content) {
                _status.value = "Без изменений"
                return@launch
            }

            val updated = current.copy(
                title     = newTitle,
                content   = newContent,
                updatedAt = System.currentTimeMillis()
            )
            updateUseCase(updated)
            _note.value = updated
            _status.value = "Сохранено"
        }
    }
}
