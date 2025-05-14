package com.svyatoslav.mlapp.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.svyatoslav.mlapp.domain.DeleteNoteUseCase
import com.svyatoslav.mlapp.domain.GetAllJournalNotesUseCase
import com.svyatoslav.mlapp.domain.model.JournalNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JournalListViewModel(
    private val getAllJournalNotesUseCase: GetAllJournalNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase
) : ViewModel() {

    val notes = getAllJournalNotesUseCase().asLiveData()

    fun deleteNote(note: JournalNote) {
        viewModelScope.launch {
            deleteNoteUseCase(note)
        }
    }
}
