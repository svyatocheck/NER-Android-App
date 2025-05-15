package com.svyatoslav.mlapp.di

import com.svyatoslav.mlapp.ui.entry.NoteEditorViewModel
import com.svyatoslav.mlapp.ui.journal.JournalListViewModel
import com.svyatoslav.mlapp.ui.main.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { MainViewModel(get()) }

    viewModel{ 
        NoteEditorViewModel(
            retrieveUseCase = get(),
            updateUseCase = get(),
            savedStateHandle = get(),
            createUseCase = get(),
            wisdomUseCase = get()
        ) 
    }

    viewModel { JournalListViewModel(get(), get()) }
}