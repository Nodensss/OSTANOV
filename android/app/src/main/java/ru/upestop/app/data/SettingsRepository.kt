package ru.upestop.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import ru.upestop.app.BuildConfig
import java.io.IOException

data class AppSettings(
    val keepScreenOn: Boolean = true,
    val haptics: Boolean = true,
    /** Тестовый режим таймеров (1 мин = 1 с). Доступен только в debug-сборке. */
    val timerTestMode: Boolean = false,
)

private val Context.settingsStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            AppSettings(
                keepScreenOn = prefs[KEEP_SCREEN_ON] ?: true,
                haptics = prefs[HAPTICS] ?: true,
                timerTestMode = BuildConfig.DEBUG && (prefs[TIMER_TEST_MODE] ?: false),
            )
        }

    suspend fun setKeepScreenOn(value: Boolean) = set(KEEP_SCREEN_ON, value)

    suspend fun setHaptics(value: Boolean) = set(HAPTICS, value)

    suspend fun setTimerTestMode(value: Boolean) = set(TIMER_TEST_MODE, value)

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        context.settingsStore.edit { it[key] = value }
    }

    private companion object {
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val HAPTICS = booleanPreferencesKey("haptics")
        val TIMER_TEST_MODE = booleanPreferencesKey("timer_test_mode")
    }
}
