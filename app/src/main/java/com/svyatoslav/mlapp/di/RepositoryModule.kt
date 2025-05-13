package com.svyatoslav.mlapp.di

import com.svyatoslav.mlapp.core.IDpNoiser
import com.svyatoslav.mlapp.core.IHMACMasker
import com.svyatoslav.mlapp.core.INerProcessing
import com.svyatoslav.mlapp.core.IRegexDetector
import com.svyatoslav.mlapp.core.ITextPostprocessor
import com.svyatoslav.mlapp.core.IWordPieceTokenizer
import com.svyatoslav.mlapp.core.models.NERModelPreprocessing
import com.svyatoslav.mlapp.core.nlp.DpNoiser
import com.svyatoslav.mlapp.core.nlp.HMACMasker
import com.svyatoslav.mlapp.core.nlp.RegexDetector
import com.svyatoslav.mlapp.core.nlp.TextPostprocessor
import com.svyatoslav.mlapp.core.nlp.WordPieceTokenizer
import com.svyatoslav.mlapp.data.ITextPreprocessingRepository
import com.svyatoslav.mlapp.data.repository.TextPreprocessingRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    // Core components
    single<IWordPieceTokenizer> { WordPieceTokenizer(androidContext()) }
    single<INerProcessing> { NERModelPreprocessing(androidContext(), get()) }
    single<IRegexDetector>{ RegexDetector() }
    single<IDpNoiser>{ DpNoiser() }
    single<IHMACMasker>{ HMACMasker("secret-key") }
    single<ITextPostprocessor> { TextPostprocessor() }

    // Repository
    single<ITextPreprocessingRepository> {
        TextPreprocessingRepository(
            ner = get(),
            regex = get(),
            dp = get(),
            post = get(),
            hmac = get()
        )
    }

    // single{ TextRepository(get()) }
}
