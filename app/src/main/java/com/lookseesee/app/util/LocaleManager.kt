package com.lookseesee.app.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

enum class AppLanguage(val tag: String) {
    ENGLISH("en"),
    CHINESE_SIMPLIFIED("zh-CN"),
}

/**
 * Switches the whole app's locale at runtime using the AndroidX per-app language API,
 * so the setup screen's language toggle takes effect immediately without a restart.
 */
object LocaleManager {
    fun apply(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    fun current(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        val tag = if (!locales.isEmpty) locales[0]?.language else null
        return if (tag == "zh") AppLanguage.CHINESE_SIMPLIFIED else AppLanguage.ENGLISH
    }
}
