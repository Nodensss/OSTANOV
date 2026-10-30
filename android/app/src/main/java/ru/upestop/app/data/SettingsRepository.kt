package ru.upestop.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import java.io.IOException
import ru.upestop.app.BuildConfig

private val Context.settingsStore by preferencesDataStore("settings")
data class Settings(val keepScreen: Boolean = true, val sound: Boolean = true, val vibration: Boolean = true, val testMode: Boolean = false)
enum class Setting(val key: String) { KEEP_SCREEN("keep_screen"), SOUND("sound"), VIBRATION("vibration"), TEST_MODE("test_mode") }
class SettingsRepository(private val context: Context) {
    val settings: Flow<Settings> = context.settingsStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { prefs -> Settings(prefs[booleanPreferencesKey("keep_screen")] ?: true,
        prefs[booleanPreferencesKey("sound")] ?: true, prefs[booleanPreferencesKey("vibration")] ?: true,
        BuildConfig.DEBUG && (prefs[booleanPreferencesKey("test_mode")] ?: false)) }
    suspend fun set(setting: Setting, value: Boolean) {
        if (setting == Setting.TEST_MODE && !BuildConfig.DEBUG) return
        context.settingsStore.edit { it[booleanPreferencesKey(setting.key)] = value }
    }
}
