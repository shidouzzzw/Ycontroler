package com.ycontroler.gamepad.data.backup

import android.content.Context
import android.net.Uri
import com.ycontroler.gamepad.data.layout.LayoutProfile
import com.ycontroler.gamepad.data.layout.LayoutStore
import com.ycontroler.gamepad.data.settings.SettingsRepository
import com.ycontroler.gamepad.data.settings.YcSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Yedek dosyası biçimi. İleri sürümlerde yeni alanlar eklenebilir. */
@Serializable
data class BackupPayload(
    val app: String = "Ycontroler",
    val version: Int = 1,
    val settings: YcSettings = YcSettings(),
    val layouts: List<LayoutProfile> = emptyList()
)

/**
 * Ayarları (ve ileride düzenleri) SAF üzerinden JSON olarak dışa/içe aktarır.
 * Dosya işlemleri IO dispatcher'ında yapılır, arayüz bloke olmaz.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val layoutStore: LayoutStore
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun suggestedFileName(): String = "ycontroler-backup.json"

    suspend fun exportTo(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val settings = settingsRepository.settings.first()
            val payload = BackupPayload(
                settings = settings,
                layouts = layoutStore.layouts.value
            )
            val text = json.encodeToString(BackupPayload.serializer(), payload)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(text.toByteArray(Charsets.UTF_8))
            } ?: error("Çıktı akışı açılamadı")
        }.isSuccess
    }

    suspend fun importFrom(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Girdi akışı açılamadı")
            val payload = json.decodeFromString(BackupPayload.serializer(), text)
            require(payload.app == "Ycontroler") { "Geçersiz yedek dosyası" }
            settingsRepository.replace(payload.settings)
            if (payload.layouts.isNotEmpty()) layoutStore.replaceAll(payload.layouts)
        }.isSuccess
    }
}
