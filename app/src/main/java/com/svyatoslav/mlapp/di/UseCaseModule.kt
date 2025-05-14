package com.svyatoslav.mlapp.di

import com.svyatoslav.mlapp.domain.CreateNewNoteUseCase
import com.svyatoslav.mlapp.domain.DeleteNoteUseCase
import com.svyatoslav.mlapp.domain.GetAllJournalNotesUseCase
import com.svyatoslav.mlapp.domain.ProcessTextUseCase
import com.svyatoslav.mlapp.domain.RetrieveJournalNoteUseCase
import com.svyatoslav.mlapp.domain.UpdateNoteUseCase
import org.koin.dsl.module

val useCaseModule = module {
    factory { ProcessTextUseCase(get()) }

    factory { RetrieveJournalNoteUseCase(get()) }
    factory { UpdateNoteUseCase(get()) }

    factory { GetAllJournalNotesUseCase(get()) }
    factory { DeleteNoteUseCase(get()) }
    factory { CreateNewNoteUseCase(get()) }

}