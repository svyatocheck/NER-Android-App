package com.svyatoslav.mlapp.di

import org.koin.dsl.module

val appModule = module {
    includes(
        viewModelModule,
        useCaseModule,
        repositoryModule,
    )
}