package com.svyatoslav.mlapp.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.svyatoslav.mlapp.data.model.JournalNoteEntity

@Database(entities = [JournalNoteEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
