package com.svyatoslav.mlapp.domain

import android.content.Context
import com.svyatoslav.mlapp.domain.model.PrivacyMode
import androidx.core.content.edit

object PrivacySettings {

    private const val PREF_NAME = "wisdomCatToken"
    private const val PRIVACY_KEY = "privacy_mode"

    fun savePrivacyMode(context: Context, mode: PrivacyMode) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit() { putString(PRIVACY_KEY, mode.raw) }
    }

    fun getPrivacyMode(context: Context): PrivacyMode {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return PrivacyMode.fromString(prefs.getString(PRIVACY_KEY, null))
    }
}
