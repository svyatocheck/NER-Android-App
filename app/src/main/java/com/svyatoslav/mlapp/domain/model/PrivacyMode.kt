package com.svyatoslav.mlapp.domain.model

enum class PrivacyMode(val raw: String) {
    NONE("none"),
    SECURE("secure"),
    MAXIMUM("max");

    companion object {
        fun fromString(value: String?): PrivacyMode =
            entries.find { it.raw == value } ?: NONE
    }
}
