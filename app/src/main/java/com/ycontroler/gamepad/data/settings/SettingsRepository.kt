package com.ycontroler.gamepad.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

private val Context.settingsDataStore by preferencesDataStore(name = "yc_settings")

/**
 * Ayarları DataStore'da JSON olarak tutar. Tüm yazmalar tek bir mutex ile serileştirilir,
 * böylece eşzamanlı güncellemeler birbirini ezmez.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
    private val key = stringPreferencesKey("settings_json")
    private val mutex = Mutex()

    val settings: Flow<YcSettings> = context.settingsDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs -> prefs[key]?.let(::decode) ?: YcSettings() }

    /** Atomik güncelleme. */
    suspend fun update(transform: (YcSettings) -> YcSettings) {
        mutex.withLock {
            context.settingsDataStore.edit { prefs ->
                val current = prefs[key]?.let(::decode) ?: YcSettings()
                prefs[key] = json.encodeToString(YcSettings.serializer(), transform(current))
            }
        }
    }

    suspend fun reset() {
        mutex.withLock {
            context.settingsDataStore.edit { it.remove(key) }
        }
    }

    /** Yedeği geri yüklerken tüm ayarları tek seferde değiştirir. */
    suspend fun replace(settings: YcSettings) {
        mutex.withLock {
            context.settingsDataStore.edit { prefs ->
                prefs[key] = json.encodeToString(YcSettings.serializer(), settings)
            }
        }
    }

    private fun decode(raw: String): YcSettings =
        runCatching { json.decodeFromString(YcSettings.serializer(), raw) }.getOrElse { YcSettings() }
}
