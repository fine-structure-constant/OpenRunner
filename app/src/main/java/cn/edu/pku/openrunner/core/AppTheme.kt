package cn.edu.pku.openrunner.core

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

enum class AppThemeMode(val persistedValue: String, val delegateMode: Int) {
    SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO),
    DARK("dark", AppCompatDelegate.MODE_NIGHT_YES);

    companion object {
        fun fromPersistedValue(value: String?): AppThemeMode =
            entries.firstOrNull { it.persistedValue == value } ?: SYSTEM
    }
}

class AppThemeStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        FILE_NAME,
        Context.MODE_PRIVATE
    )

    val mode: AppThemeMode
        get() = AppThemeMode.fromPersistedValue(preferences.getString(KEY_MODE, null))

    fun applySavedMode() {
        AppCompatDelegate.setDefaultNightMode(mode.delegateMode)
    }

    fun setMode(mode: AppThemeMode) {
        preferences.edit { putString(KEY_MODE, mode.persistedValue) }
        AppCompatDelegate.setDefaultNightMode(mode.delegateMode)
    }

    companion object {
        private const val FILE_NAME = "openrunner_appearance"
        private const val KEY_MODE = "theme_mode"
    }
}
