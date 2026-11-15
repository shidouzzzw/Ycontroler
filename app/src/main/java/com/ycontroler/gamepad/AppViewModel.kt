package com.ycontroler.gamepad

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ycontroler.gamepad.data.backup.BackupManager
import com.ycontroler.gamepad.data.layout.LayoutProfile
import com.ycontroler.gamepad.data.layout.LayoutStore
import com.ycontroler.gamepad.data.settings.SettingsRepository
import com.ycontroler.gamepad.data.settings.YcSettings
import com.ycontroler.gamepad.hid.HidConnectionState
import com.ycontroler.gamepad.hid.HidGamepadManager
import com.ycontroler.gamepad.hid.HidSupport
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Uygulama genelinde paylaşılan durum: ayarlar, HID bağlantısı ve eşleşmiş cihazlar.
 * Ekranlar bu tek ViewModel üzerinden hareket eder.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
    private val layoutStore: LayoutStore,
    val hid: HidGamepadManager
) : ViewModel() {

    val settings: StateFlow<YcSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, YcSettings())

    val support: StateFlow<HidSupport> = hid.support
    val connection: StateFlow<HidConnectionState> = hid.state
    val hosts = hid.hosts
    val latencyMs = hid.latencyMs
    val reportsPerSecond = hid.reportsPerSecond
    val batteryPercent = hid.batteryPercent
    val bluetoothEnabled = hid.bluetoothEnabled
    val input = hid.input
    val layouts = layoutStore.layouts

    init {
        viewModelScope.launch { hid.checkSupport() }
        viewModelScope.launch {
            settings.collect { current ->
                hid.setReportRate(current.reportRateHz)
                hid.setAutoReconnect(current.autoReconnect)
            }
        }
    }

    fun updateSettings(transform: (YcSettings) -> YcSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun resetSettings() {
        viewModelScope.launch { settingsRepository.reset() }
    }

    fun startHid() {
        viewModelScope.launch {
            val current = settings.value
            hid.start(current.deviceName, current.autoReconnect)
        }
    }

    fun recheckSupport() {
        viewModelScope.launch { hid.checkSupport() }
    }

    fun connectToHost(address: String) {
        viewModelScope.launch { hid.connectTo(address) }
    }

    /** HID kaydını garanti eder, ardından eşleşmiş cihaza bağlanır. */
    fun connectToHostAfterStart(address: String) {
        viewModelScope.launch {
            val current = settings.value
            hid.start(current.deviceName, current.autoReconnect)
            hid.connectTo(address)
        }
    }

    fun refreshHosts() = hid.refreshHosts()

    fun disconnect() = hid.disconnect()

    fun shutdownHid() = hid.shutdown()

    /** HID kaydını kapatıp yeniden açar — takılı kayıtları temizler. */
    fun restartHid() {
        viewModelScope.launch {
            hid.shutdown()
            kotlinx.coroutines.delay(450)
            val current = settings.value
            hid.start(current.deviceName, current.autoReconnect)
        }
    }

    fun requestSend() = hid.requestSend()

    fun releaseAll() = hid.releaseAll()

    // ---------------------------------------------------------------- Yedekleme

    private val _backupResult = MutableStateFlow<BackupResult?>(null)
    val backupResult: StateFlow<BackupResult?> = _backupResult.asStateFlow()

    fun suggestedBackupName(): String = backupManager.suggestedFileName()

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val ok = backupManager.exportTo(uri)
            _backupResult.value = if (ok) BackupResult.EXPORTED else BackupResult.FAILED
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val ok = backupManager.importFrom(uri)
            _backupResult.value = if (ok) BackupResult.IMPORTED else BackupResult.FAILED
        }
    }

    fun clearBackupResult() {
        _backupResult.value = null
    }

    // ---------------------------------------------------------------- Düzenler

    fun saveLayout(profile: LayoutProfile) {
        viewModelScope.launch {
            layoutStore.upsert(profile.copy(updatedAt = System.currentTimeMillis()))
            settingsRepository.update { it.copy(activeLayoutId = profile.id) }
        }
    }

    fun setActiveLayout(id: String) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(activeLayoutId = id) }
        }
    }

    fun duplicateLayout(id: String) {
        viewModelScope.launch {
            val source = layouts.value.firstOrNull { it.id == id } ?: return@launch
            val newId = "custom-${System.currentTimeMillis()}"
            val base = source.name.substringBefore(" Kopya")
            val copyName = "$base Kopya"
            layoutStore.duplicate(id, newId, copyName)
            settingsRepository.update { it.copy(activeLayoutId = newId) }
        }
    }

    fun deleteLayout(id: String) {
        viewModelScope.launch {
            val deleted = layoutStore.delete(id)
            if (deleted) {
                val next = layouts.value.firstOrNull()?.id ?: "fps"
                settingsRepository.update { s ->
                    if (s.activeLayoutId == id) s.copy(activeLayoutId = next) else s
                }
            }
        }
    }
}

/** Yedekleme işleminin sonucu — arayüzde yerelleştirilmiş mesaja çevrilir. */
enum class BackupResult { EXPORTED, IMPORTED, FAILED }
