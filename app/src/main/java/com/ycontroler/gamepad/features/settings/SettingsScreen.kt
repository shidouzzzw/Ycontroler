package com.ycontroler.gamepad.features.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ycontroler.gamepad.AppViewModel
import com.ycontroler.gamepad.BackupResult
import com.ycontroler.gamepad.core.design.AccentTheme
import com.ycontroler.gamepad.core.design.AnimationLevel
import com.ycontroler.gamepad.core.design.YcTheme
import com.ycontroler.gamepad.core.i18n.Language
import com.ycontroler.gamepad.core.i18n.t
import com.ycontroler.gamepad.data.settings.OrientationMode
import com.ycontroler.gamepad.data.settings.TriggerMode
import com.ycontroler.gamepad.ui.components.AccentPicker
import com.ycontroler.gamepad.ui.components.ChoiceChip
import com.ycontroler.gamepad.ui.components.EmptyState
import com.ycontroler.gamepad.ui.components.GlassCard
import com.ycontroler.gamepad.ui.components.SegmentedRow
import com.ycontroler.gamepad.ui.components.SettingRow
import com.ycontroler.gamepad.ui.components.SettingsSection
import com.ycontroler.gamepad.ui.components.YcConfirmDialog
import com.ycontroler.gamepad.ui.components.YcIcon
import com.ycontroler.gamepad.ui.components.YcScreen
import com.ycontroler.gamepad.ui.components.YcSlider
import com.ycontroler.gamepad.ui.components.YcSwitch
import com.ycontroler.gamepad.ui.components.YcTextField
import com.ycontroler.gamepad.ui.components.gamepad.GamepadSkin
import com.ycontroler.gamepad.ui.components.gamepad.StickConfig
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * Ayarlar ekranı: arama kutusu, gruplanmış ve animasyonlu bölümler.
 * Her kontrol anında DataStore'a yazılır; uygulama yeniden başlatmadan uygular.
 */
@Composable
fun SettingsScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenLayouts: () -> Unit
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val backupResult by vm.backupResult.collectAsStateWithLifecycle()
    val colors = YcTheme.colors
    val type = YcTheme.type

    var query by remember { mutableStateOf("") }
    var expandedSections by remember { mutableStateOf(setOf("general")) }
    var showResetDialog by remember { mutableStateOf(false) }
    var deviceNameDraft by remember(settings.deviceName) { mutableStateOf(settings.deviceName) }

    val searchMode = query.isNotBlank()
    fun has(vararg words: String): Boolean {
        if (!searchMode) return true
        val q = query.trim().lowercase(Locale.getDefault())
        return words.any { it.lowercase(Locale.getDefault()).contains(q) }
    }

    fun toggleSection(id: String) {
        expandedSections = if (id in expandedSections) expandedSections - id else expandedSections + id
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportBackup(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importBackup(it) } }

    LaunchedEffect(backupResult) {
        if (backupResult != null) {
            delay(2800)
            vm.clearBackupResult()
        }
    }

    YcScreen(
        title = t("Ayarlar", "Settings"),
        subtitle = t("Her şey senin kontrolünde", "Everything is yours to tune"),
        onBack = onBack
    ) {
        YcTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = t("Ayar ara: analog, tema, tetik...", "Search: analog, theme, trigger..."),
            leading = { YcIcon(YcIcon.Search, Modifier.size(17.dp), tint = colors.textTertiary) },
            trailing = if (query.isNotEmpty()) {
                {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { query = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        YcIcon(YcIcon.Close, Modifier.size(13.dp), tint = colors.textTertiary)
                    }
                }
            } else {
                null
            }
        )

        if (backupResult != null) {
            Spacer(Modifier.height(10.dp))
            val failed = backupResult == BackupResult.FAILED
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    YcIcon(
                        icon = if (failed) YcIcon.Warning else YcIcon.Check,
                        modifier = Modifier.size(18.dp),
                        tint = if (failed) colors.danger else colors.success
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = when (backupResult) {
                            BackupResult.EXPORTED -> t("Yedek kaydedildi.", "Backup saved.")
                            BackupResult.IMPORTED -> t("Yedek geri yüklendi.", "Backup restored.")
                            else -> t("İşlem başarısız oldu.", "The operation failed.")
                        },
                        style = type.bodyStrong,
                        color = colors.text
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ------------------------------------------------------------ GENEL
        if (has("genel", "general", "dil", "language", "tema", "theme", "amoled", "animasyon", "animation", "renk", "color")) {
            SettingsSection(
                title = t("Genel", "General"),
                icon = YcIcon.Palette,
                expanded = searchMode || "general" in expandedSections,
                onToggle = { toggleSection("general") }
            ) {
                FieldLabel(t("DİL", "LANGUAGE"))
                SegmentedRow(
                    options = listOf("Türkçe", "English"),
                    selectedIndex = settings.language.ordinal,
                    onSelect = { index ->
                        vm.updateSettings { s ->
                            s.copy(language = if (index == 0) Language.TR else Language.EN)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                FieldLabel(t("VURGU RENGİ", "ACCENT COLOR"))
                AccentPicker(
                    selectedTheme = settings.accent,
                    customArgb = settings.customAccentArgb,
                    onSelectTheme = { theme ->
                        vm.updateSettings { it.copy(accent = theme, customAccentArgb = null) }
                    },
                    onCustomColor = { color ->
                        vm.updateSettings {
                            it.copy(customAccentArgb = color.toArgb().toLong() and 0xFFFFFFFFL)
                        }
                    }
                )
                Spacer(Modifier.height(14.dp))
                SettingRow(
                    title = t("AMOLED siyah", "AMOLED black"),
                    subtitle = t("Saf siyah arka plan, pil tasarrufu", "True black background, saves battery"),
                    trailing = {
                        YcSwitch(
                            checked = settings.amoled,
                            onCheckedChange = { v -> vm.updateSettings { it.copy(amoled = v) } }
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
                FieldLabel(t("ANİMASYON SEVİYESİ", "ANIMATION LEVEL"))
                SegmentedRow(
                    options = AnimationLevel.entries.map { if (it == AnimationLevel.FULL) t("Tam", "Full") else if (it == AnimationLevel.REDUCED) t("Az", "Low") else t("Kapalı", "Off") },
                    selectedIndex = settings.animationLevel.ordinal,
                    onSelect = { index ->
                        vm.updateSettings { s ->
                            s.copy(animationLevel = AnimationLevel.entries[index])
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ------------------------------------------------------------ KONTROL
        if (has("kontrol", "control", "haptic", "titreşim", "tuş", "button", "sembol", "skin", "ses", "sound")) {
            SettingsSection(
                title = t("Kontrol", "Controls"),
                icon = YcIcon.Gamepad,
                expanded = searchMode || "controls" in expandedSections,
                onToggle = { toggleSection("controls") }
            ) {
                SettingRow(
                    title = t("Dokunsal geri bildirim", "Haptic feedback"),
                    subtitle = t("Tuşa basınca kısa titreşim", "Short tick when pressing a button"),
                    trailing = {
                        YcSwitch(
                            checked = settings.hapticsEnabled,
                            onCheckedChange = { v -> vm.updateSettings { it.copy(hapticsEnabled = v) } }
                        )
                    }
                )
                SliderSetting(
                    title = t("Titreşim şiddeti", "Haptic strength"),
                    value = settings.hapticStrength,
                    range = 0f..1f,
                    enabled = settings.hapticsEnabled
                ) { v -> vm.updateSettings { it.copy(hapticStrength = v) } }
                SliderSetting(
                    title = t("Tuş boyutu", "Button size"),
                    value = settings.buttonScale,
                    range = 0.8f..1.25f
                ) { v -> vm.updateSettings { it.copy(buttonScale = v) } }
                SliderSetting(
                    title = t("Tuş saydamlığı", "Button opacity"),
                    value = settings.buttonOpacity,
                    range = 0.35f..1f
                ) { v -> vm.updateSettings { it.copy(buttonOpacity = v) } }
                Spacer(Modifier.height(10.dp))
                FieldLabel(t("SEMBOL TEMASI", "BUTTON GLYPHS"))
                SegmentedRow(
                    options = listOf("Xbox", "PlayStation", "Nintendo"),
                    selectedIndex = settings.skin.ordinal,
                    onSelect = { index ->
                        vm.updateSettings { s -> s.copy(skin = GamepadSkin.entries[index]) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                SettingRow(
                    title = t("Ses efektleri", "Sound effects"),
                    subtitle = t("Tuşa basınca kısa tık sesi", "Short click sound on press"),
                    trailing = {
                        YcSwitch(
                            checked = settings.soundEnabled,
                            onCheckedChange = { v -> vm.updateSettings { it.copy(soundEnabled = v) } }
                        )
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ------------------------------------------------------------ ANALOG
        if (has("analog", "çubuk", "stick", "deadzone", "ölü bölge", "hassasiyet", "sensitivity", "float")) {
            SettingsSection(
                title = t("Analog Çubuklar", "Analog Sticks"),
                icon = YcIcon.Target,
                expanded = searchMode || "analog" in expandedSections,
                onToggle = { toggleSection("analog") }
            ) {
                FieldLabel(t("SOL ÇUBUK", "LEFT STICK"))
                StickSettingsBlock(
                    config = settings.leftStick,
                    onChange = { newConfig -> vm.updateSettings { it.copy(leftStick = newConfig) } }
                )
                Spacer(Modifier.height(18.dp))
                FieldLabel(t("SAĞ ÇUBUK", "RIGHT STICK"))
                StickSettingsBlock(
                    config = settings.rightStick,
                    onChange = { newConfig -> vm.updateSettings { it.copy(rightStick = newConfig) } }
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ------------------------------------------------------------ TETİKLER
        if (has("tetik", "trigger", "lt", "rt")) {
            SettingsSection(
                title = t("Tetikler", "Triggers"),
                icon = YcIcon.Zap,
                expanded = searchMode || "triggers" in expandedSections,
                onToggle = { toggleSection("triggers") }
            ) {
                FieldLabel(t("MOD", "MODE"))
                SegmentedRow(
                    options = listOf(t("Analog", "Analog"), t("Dijital", "Digital")),
                    selectedIndex = settings.triggerMode.ordinal,
                    onSelect = { index ->
                        vm.updateSettings { s -> s.copy(triggerMode = TriggerMode.entries[index]) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                SliderSetting(
                    title = t("Tetik hassasiyeti", "Trigger sensitivity"),
                    value = settings.triggerSensitivity,
                    range = 0.3f..1.6f,
                    enabled = settings.triggerMode == TriggerMode.ANALOG,
                    format = { String.format(Locale.US, "%.1fx", it) }
                ) { v -> vm.updateSettings { it.copy(triggerSensitivity = v) } }
                Text(
                    text = t(
                        "Dijital modda tetik %35 derinlikte tam basılı sayılır.",
                        "In digital mode a trigger counts as pressed at 35% travel."
                    ),
                    style = type.micro,
                    color = colors.textTertiary
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ------------------------------------------------------------ SENSÖR
        if (has("sensör", "sensor", "jiroskop", "gyro", "hareket", "motion")) {
            SettingsSection(
                title = t("Sensör", "Sensor"),
                icon = YcIcon.Rotate,
                expanded = searchMode || "sensor" in expandedSections,
                onToggle = { toggleSection("sensor") }
            ) {
                SettingRow(
                    title = t("Jiroskop ile sağ çubuk", "Gyro right stick"),
                    subtitle = t("Telefonu çevirerek nişan al", "Aim by turning the phone"),
                    trailing = {
                        YcSwitch(
                            checked = settings.gyroEnabled,
                            onCheckedChange = { v -> vm.updateSettings { it.copy(gyroEnabled = v) } }
                        )
                    }
                )
                SliderSetting(
                    title = t("Jiroskop hassasiyeti", "Gyro sensitivity"),
                    value = settings.gyroSensitivity,
                    range = 0f..1f,
                    enabled = settings.gyroEnabled
                ) { v -> vm.updateSettings { it.copy(gyroSensitivity = v) } }
                Text(
                    text = t(
                        "Jiroskop açıkken sağ çubuğa dokunmadığın sürece hareketle kontrol edilir.",
                        "While gyro is on, the right stick follows motion unless you touch it."
                    ),
                    style = type.micro,
                    color = colors.textTertiary
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ------------------------------------------------------------ BAĞLANTI
        if (has("bağlantı", "connection", "bluetooth", "cihaz", "device", "rapor", "report", "isim", "name")) {
            SettingsSection(
                title = t("Bağlantı", "Connection"),
                icon = YcIcon.Bluetooth,
                expanded = searchMode || "connection" in expandedSections,
                onToggle = { toggleSection("connection") }
            ) {
                SettingRow(
                    title = t("Otomatik yeniden bağlan", "Auto reconnect"),
                    subtitle = t("Bağlantı koparsa yeniden dener", "Retries when the link drops"),
                    trailing = {
                        YcSwitch(
                            checked = settings.autoReconnect,
                            onCheckedChange = { v -> vm.updateSettings { it.copy(autoReconnect = v) } }
                        )
                    }
                )
                Spacer(Modifier.height(6.dp))
                FieldLabel(t("BİLGİSAYARDA GÖRÜNEN AD", "NAME SHOWN ON YOUR PC"))
                YcTextField(
                    value = deviceNameDraft,
                    onValueChange = { if (it.length <= 28) deviceNameDraft = it },
                    placeholder = "Ycontroler",
                    trailing = if (deviceNameDraft.isNotBlank() && deviceNameDraft != settings.deviceName) {
                        {
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.accent.copy(alpha = 0.18f))
                                    .clickable {
                                        val name = deviceNameDraft.trim().take(28)
                                        if (name.isNotEmpty()) {
                                            vm.updateSettings { it.copy(deviceName = name) }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                YcIcon(YcIcon.Check, Modifier.size(15.dp), tint = colors.accent)
                            }
                        }
                    } else {
                        null
                    }
                )
                Spacer(Modifier.height(12.dp))
                FieldLabel(t("RAPOR HIZI", "REPORT RATE"))
                SegmentedRow(
                    options = listOf("60 Hz", "125 Hz", "250 Hz"),
                    selectedIndex = when (settings.reportRateHz) {
                        60 -> 0
                        125 -> 1
                        else -> 2
                    },
                    onSelect = { index ->
                        val rate = when (index) {
                            0 -> 60
                            1 -> 125
                            else -> 250
                        }
                        vm.updateSettings { it.copy(reportRateHz = rate) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = t(
                        "Daha yüksek hız daha akıcı kontrol sağlar, pil tüketimini artırır.",
                        "Higher rates feel smoother and use more battery."
                    ),
                    style = type.micro,
                    color = colors.textTertiary
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ------------------------------------------------------------ EKRAN
        if (has("ekran", "screen", "parlaklık", "brightness", "yön", "orientation", "el", "hand")) {
            SettingsSection(
                title = t("Ekran", "Screen"),
                icon = YcIcon.Screen,
                expanded = searchMode || "screen" in expandedSections,
                onToggle = { toggleSection("screen") }
            ) {
                SettingRow(
                    title = t("Ekranı açık tut", "Keep screen on"),
                    subtitle = t("Kontrolcü açıkken uykuya girmez", "No sleep while the controller is open"),
                    trailing = {
                        YcSwitch(
                            checked = settings.keepScreenOn,
                            onCheckedChange = { v -> vm.updateSettings { it.copy(keepScreenOn = v) } }
                        )
                    }
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = t("Parlaklık", "Brightness"),
                        style = type.bodyStrong,
                        color = colors.text,
                        modifier = Modifier.weight(1f)
                    )
                    ChoiceChip(
                        text = t("Otomatik", "Auto"),
                        selected = settings.brightness == null,
                        onClick = { vm.updateSettings { it.copy(brightness = null) } }
                    )
                }
                YcSlider(
                    value = settings.brightness ?: 0.6f,
                    onValueChange = { v -> vm.updateSettings { it.copy(brightness = v.coerceIn(0.1f, 1f)) } },
                    range = 0.1f..1f,
                    enabled = settings.brightness != null
                )
                Spacer(Modifier.height(8.dp))
                FieldLabel(t("YÖN", "ORIENTATION"))
                SegmentedRow(
                    options = listOf(t("Yatay", "Landscape"), t("Sensör", "Sensor")),
                    selectedIndex = settings.orientation.ordinal,
                    onSelect = { index ->
                        vm.updateSettings { s -> s.copy(orientation = OrientationMode.entries[index]) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                SettingRow(
                    title = t("Sol/sağ el düzeni", "Left-handed layout"),
                    subtitle = t("Çubukların ve tuşların yerini değiştirir", "Mirrors sticks and buttons"),
                    trailing = {
                        YcSwitch(
                            checked = settings.leftHanded,
                            onCheckedChange = { v -> vm.updateSettings { it.copy(leftHanded = v) } }
                        )
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ------------------------------------------------------------ VERİ
        if (has("veri", "data", "profil", "profile", "yedek", "backup", "dışa", "içe", "sıfırla", "reset", "düzen", "layout")) {
            SettingsSection(
                title = t("Veri ve Profiller", "Data & Profiles"),
                icon = YcIcon.Saved,
                expanded = searchMode || "data" in expandedSections,
                onToggle = { toggleSection("data") }
            ) {
                SettingRow(
                    title = t("Düzenler", "Layouts"),
                    subtitle = t("Düzenle, oluştur, sil", "Edit, create, delete"),
                    icon = YcIcon.Grid,
                    trailing = { YcIcon(YcIcon.ChevronRight, Modifier.size(16.dp), tint = colors.textTertiary) },
                    onClick = onOpenLayouts
                )
                SettingRow(
                    title = t("Dışa aktar (JSON)", "Export (JSON)"),
                    subtitle = t("Ayarlarını dosyaya kaydet", "Save your settings to a file"),
                    icon = YcIcon.Upload,
                    onClick = { exportLauncher.launch(vm.suggestedBackupName()) }
                )
                SettingRow(
                    title = t("İçe aktar (JSON)", "Import (JSON)"),
                    subtitle = t("Bir yedek dosyasından geri yükle", "Restore from a backup file"),
                    icon = YcIcon.Download,
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }
                )
                SettingRow(
                    title = t("Ayarları sıfırla", "Reset settings"),
                    subtitle = t("Tüm ayarlar fabrika değerlerine döner", "Everything returns to defaults"),
                    icon = YcIcon.Trash,
                    onClick = { showResetDialog = true }
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        if (searchMode && !has(
                "genel", "general", "dil", "language", "tema", "theme", "amoled", "animasyon", "animation",
                "kontrol", "control", "haptic", "tuş", "button", "sembol", "skin", "ses",
                "analog", "çubuk", "stick", "tetik", "trigger", "sensör", "sensor", "jiroskop",
                "bağlantı", "connection", "bluetooth", "cihaz", "rapor", "ekran", "screen",
                "veri", "data", "profil", "yedek", "düzen"
            )
        ) {
            EmptyState(
                icon = YcIcon.Search,
                title = t("Sonuç bulunamadı", "No results"),
                message = t(
                    "\"$query\" ile eşleşen ayar yok. Farklı bir kelime dene.",
                    "No setting matches \"$query\". Try another word."
                )
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = t(
                "Ycontroler 1.0 · Tüm ayarlar cihazında saklanır.",
                "Ycontroler 1.0 · All settings are stored on your device."
            ),
            style = type.micro,
            color = colors.textTertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showResetDialog) {
        YcConfirmDialog(
            title = t("Ayarları sıfırla?", "Reset settings?"),
            message = t(
                "Tema, kontrol, analog ve bağlantı ayarlarının tamamı varsayılana döner. Düzenler silinmez.",
                "Theme, controls, analog and connection settings return to defaults. Layouts are kept."
            ),
            confirmText = t("Sıfırla", "Reset"),
            dismissText = t("Vazgeç", "Cancel"),
            onConfirm = {
                vm.resetSettings()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false },
            destructive = true
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = YcTheme.type.label,
        color = YcTheme.colors.textTertiary,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SliderSetting(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    format: (Float) -> String = { "${(it * 100).roundToInt()}%" },
    onChange: (Float) -> Unit
) {
    val colors = YcTheme.colors
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = YcTheme.type.bodyStrong,
                color = if (enabled) colors.text else colors.textTertiary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = format(value),
                style = YcTheme.type.number,
                color = if (enabled) colors.accent else colors.textTertiary
            )
        }
        YcSlider(
            value = value,
            onValueChange = onChange,
            range = range,
            enabled = enabled
        )
    }
}

/** Tek bir çubuğun tüm davranış ayarları. */
@Composable
private fun StickSettingsBlock(
    config: StickConfig,
    onChange: (StickConfig) -> Unit
) {
    val colors = YcTheme.colors
    SettingRow(
        title = t("Floating mod", "Floating mode"),
        subtitle = t(
            "Çubuk parmağının bastığı yerde belirir",
            "The stick appears where your thumb lands"
        ),
        trailing = {
            YcSwitch(
                checked = config.floating,
                onCheckedChange = { v -> onChange(config.copy(floating = v)) }
            )
        }
    )
    SliderSetting(
        title = t("Ölü bölge", "Dead zone"),
        value = config.deadZone,
        range = 0f..0.3f,
        format = { "${(it * 100).roundToInt()}%" }
    ) { v -> onChange(config.copy(deadZone = v)) }
    SliderSetting(
        title = t("Hassasiyet", "Sensitivity"),
        value = config.sensitivity,
        range = 0.3f..2f,
        format = { String.format(Locale.US, "%.1fx", it) }
    ) { v -> onChange(config.copy(sensitivity = v)) }
    SliderSetting(
        title = t("Kafa boyutu", "Knob size"),
        value = config.knobScale,
        range = 0.8f..1.3f
    ) { v -> onChange(config.copy(knobScale = v)) }
    Spacer(Modifier.height(8.dp))
    FieldLabel(t("TEPKİ EĞRİSİ", "RESPONSE CURVE"))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceChip(
            text = t("Doğrusal", "Linear"),
            selected = !config.exponential,
            onClick = { onChange(config.copy(exponential = false)) }
        )
        ChoiceChip(
            text = t("Üstel", "Exponential"),
            selected = config.exponential,
            onClick = { onChange(config.copy(exponential = true)) }
        )
    }
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = t("Eksen ters çevir", "Invert axes"),
            style = YcTheme.type.bodyStrong,
            color = colors.text,
            modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChoiceChip(
                text = "X",
                selected = config.invertX,
                onClick = { onChange(config.copy(invertX = !config.invertX)) }
            )
            ChoiceChip(
                text = "Y",
                selected = config.invertY,
                onClick = { onChange(config.copy(invertY = !config.invertY)) }
            )
        }
    }
    SettingRow(
        title = t("Merkeze yapışma", "Snap to center"),
        subtitle = t("Ölü bölge içinde tam sıfır", "Exact zero inside the dead zone"),
        trailing = {
            YcSwitch(
                checked = config.snapToCenter,
                onCheckedChange = { v -> onChange(config.copy(snapToCenter = v)) }
            )
        }
    )
    SettingRow(
        title = t("L3/R3 çift dokunuş", "L3/R3 double tap"),
        subtitle = t("Çubuğa çift dokunarak bas", "Press the stick with a double tap"),
        trailing = {
            YcSwitch(
                checked = config.l3DoubleTap,
                onCheckedChange = { v -> onChange(config.copy(l3DoubleTap = v)) }
            )
        }
    )
}

