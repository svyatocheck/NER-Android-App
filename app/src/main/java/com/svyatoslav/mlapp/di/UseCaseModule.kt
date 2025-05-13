package com.svyatoslav.mlapp.di

import com.svyatoslav.mlapp.domain.ProcessTextUseCase
import org.koin.dsl.module

val useCaseModule = module {
    factory { ProcessTextUseCase(get()) }
}