package com.svyatoslav.mlapp.di

import android.content.Context
import androidx.core.content.edit
import androidx.room.Room
import com.svyatoslav.mlapp.core.IDpNoiser
import com.svyatoslav.mlapp.core.IHMACMasker
import com.svyatoslav.mlapp.core.INerProcessing
import com.svyatoslav.mlapp.core.IRegexDetector
import com.svyatoslav.mlapp.core.ITextPostprocessor
import com.svyatoslav.mlapp.core.IWordPieceTokenizer
import com.svyatoslav.mlapp.core.models.NERPreprocessing
import com.svyatoslav.mlapp.core.nlp.DpNoiser
import com.svyatoslav.mlapp.core.nlp.HMACMasker
import com.svyatoslav.mlapp.core.nlp.RegexDetector
import com.svyatoslav.mlapp.core.nlp.TextPostprocessor
import com.svyatoslav.mlapp.core.nlp.WordPieceTokenizer
import com.svyatoslav.mlapp.data.IJournalRepository
import com.svyatoslav.mlapp.data.ITextPreprocessingRepository
import com.svyatoslav.mlapp.data.IWisdomCatRepository
import com.svyatoslav.mlapp.data.repository.JournalRepository
import com.svyatoslav.mlapp.data.repository.TextPreprocessingRepository
import com.svyatoslav.mlapp.data.repository.WisdomCatRepository
import com.svyatoslav.mlapp.data.source.local.AppDatabase
import com.svyatoslav.mlapp.data.source.remote.WisdomApiService
import com.svyatoslav.mlapp.domain.WisdomUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val repositoryModule = module {
    // Core components
    single<IWordPieceTokenizer> { WordPieceTokenizer(androidContext()) }
    single<INerProcessing> { NERPreprocessing(androidContext(), get()) }
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

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "journal.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<AppDatabase>().noteDao() }
    single<IJournalRepository> { JournalRepository(get(), get()) }


    single<WisdomApiService> {
        Retrofit.Builder()
            .baseUrl("https://api.groq.com/")  // <- важно!
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WisdomApiService::class.java)
    }

    single<IWisdomCatRepository> {
        WisdomCatRepository(
            api = get(),
            context = get()
        )
    }

}
