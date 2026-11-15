package com.ycontroler.gamepad.features.pairing

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ycontroler.gamepad.AppViewModel
import com.ycontroler.gamepad.core.design.YcTheme
import com.ycontroler.gamepad.core.i18n.t
import com.ycontroler.gamepad.core.permissions.BluetoothPermissionController
import com.ycontroler.gamepad.hid.HidConnectionState
import com.ycontroler.gamepad.hid.HidFailure
import com.ycontroler.gamepad.hid.HidHostDevice
import com.ycontroler.gamepad.ui.components.GamepadMark
import com.ycontroler.gamepad.ui.components.GlassCard
import com.ycontroler.gamepad.ui.components.PrimaryButton
import com.ycontroler.gamepad.ui.components.SecondaryButton
import com.ycontroler.gamepad.ui.components.YcIcon
import com.ycontroler.gamepad.ui.components.YcScreen
import com.ycontroler.gamepad.ui.components.sharedElementFor
import kotlinx.coroutines.delay

/**
 * Eşleştirme akışı: HID kaydı, telefonu görünür yapma ve bilgisayardan eşleştirme adımları.
 * Her adım durumla birlikte canlı güncellenir; hata durumlarında çözüm önerisi gösterilir.
 */
@Composable
fun PairingScreen(
    vm: AppViewModel,
    permissions: BluetoothPermissionController,
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenController: () -> Unit
) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val connection by vm.connection.collectAsStateWithLifecycle()
    val hosts by vm.hosts.collectAsStateWithLifecycle()
    val colors = YcTheme.colors
    val type = YcTheme.type

    var discoverableUntil by remember { mutableLongStateOf(0L) }
    var remainingSeconds by remember { mutableIntStateOf(0) }

    val discoverableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val seconds = if (result.resultCode > 0) result.resultCode else 0
        discoverableUntil = if (seconds > 0) System.currentTimeMillis() + seconds * 1000L else 0L
    }
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    LaunchedEffect(Unit) {
        if (permissions.allGranted) vm.startHid()
        vm.refreshHosts()
    }

    LaunchedEffect(discoverableUntil) {
        while (discoverableUntil > System.currentTimeMillis()) {
            remainingSeconds = ((discoverableUntil - System.currentTimeMillis()) / 1000L).toInt()
            delay(1000L)
        }
        remainingSeconds = 0
    }

    fun requestDiscoverable() {
        if (!permissions.allGranted) {
            onOpenPermissions()
            return
        }
        runCatching {
            discoverableLauncher.launch(
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                }
            )
        }
    }

    fun requestEnableBluetooth() {
        runCatching {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    val stepOneDone = connection is HidConnectionState.Waiting || connection is HidConnectionState.Connected
    val stepTwoDone = remainingSeconds > 0 || connection is HidConnectionState.Connected
    val stepThreeDone = connection is HidConnectionState.Connected

    YcScreen(
        title = t("Eşleştirme", "Pairing"),
        subtitle = settings.deviceName,
        onBack = onBack
    ) {
        PairingHero(
            connection = connection,
            onOpenController = onOpenController,
            onDisconnect = { vm.disconnect() }
        )

        val error = connection as? HidConnectionState.Error
        if (error != null) {
            Spacer(Modifier.height(14.dp))
            ErrorCard(
                failure = error.failure,
                detail = error.detail,
                onRequestPermissions = onOpenPermissions,
                onEnableBluetooth = { requestEnableBluetooth() },
                onRetry = { vm.startHid() }
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = t("ADIMLAR", "STEPS"),
            style = type.label,
            color = colors.textTertiary
        )
        Spacer(Modifier.height(10.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            StepRow(
                index = 1,
                title = t("Gamepad hazır", "Gamepad ready"),
                description = if (stepOneDone) {
                    t("Telefon HID gamepad olarak kayıtlı.", "Phone is registered as a HID gamepad.")
                } else {
                    t("HID aygıtı kaydediliyor...", "Registering HID device...")
                },
                done = stepOneDone,
                active = connection is HidConnectionState.Preparing
            )
            StepDivider()
            StepRow(
                index = 2,
                title = t("Telefonu görünür yap", "Make phone discoverable"),
                description = if (remainingSeconds > 0) {
                    t(
                        "Yaklaşık $remainingSeconds saniye boyunca görünür.",
                        "Visible for about $remainingSeconds seconds."
                    )
                } else {
                    t(
                        "Bilgisayarın telefonu bulabilmesi için 5 dakika görünür kal.",
                        "Stay visible for 5 minutes so your PC can find the phone."
                    )
                },
                done = stepTwoDone,
                active = false
            ) {
                SecondaryButton(
                    text = if (remainingSeconds > 0) {
                        t("Görünürlüğü Yenile", "Refresh Visibility")
                    } else {
                        t("Görünür Yap", "Make Discoverable")
                    },
                    onClick = { requestDiscoverable() },
                    icon = { YcIcon(YcIcon.Signal, Modifier.size(16.dp)) }
                )
            }
            StepDivider()
            StepRow(
                index = 3,
                title = t("Bilgisayardan eşleştir", "Pair from your PC"),
                description = t(
                    "Windows: Ayarlar > Bluetooth ve cihazlar > Cihaz ekle > Bluetooth. Listeden \"${settings.deviceName}\" seç.",
                    "Windows: Settings > Bluetooth & devices > Add device > Bluetooth. Pick \"${settings.deviceName}\" from the list."
                ),
                done = stepThreeDone,
                active = stepOneDone && !stepThreeDone
            )
        }

        Spacer(Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                YcIcon(YcIcon.Info, Modifier.size(18.dp), tint = colors.accent)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = t("macOS / Linux", "macOS / Linux"),
                    style = type.title,
                    color = colors.text
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = t(
                    "macOS: Sistem Ayarları > Bluetooth > \"${settings.deviceName}\" > Bağlan.\nLinux: bluetoothctl > scan on > pair <MAC> > connect <MAC>.",
                    "macOS: System Settings > Bluetooth > \"${settings.deviceName}\" > Connect.\nLinux: bluetoothctl > scan on > pair <MAC> > connect <MAC>."
                ),
                style = type.bodySmall,
                color = colors.textSecondary
            )
        }

        if (hosts.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = t("EŞLEŞMİŞ CİHAZLAR", "PAIRED DEVICES"),
                style = type.label,
                color = colors.textTertiary
            )
            Spacer(Modifier.height(10.dp))
            hosts.take(4).forEachIndexed { index, host ->
                if (index > 0) Spacer(Modifier.height(8.dp))
                PairedHostRow(
                    host = host,
                    connected = (connection as? HidConnectionState.Connected)?.address == host.address,
                    onConnect = { vm.connectToHostAfterStart(host.address) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        if (connection is HidConnectionState.Connected) {
            PrimaryButton(
                text = t("Kontrolcüyü Aç", "Open Controller"),
                onClick = onOpenController,
                modifier = Modifier.fillMaxWidth(),
                icon = { YcIcon(YcIcon.Gamepad, Modifier.size(18.dp)) }
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton(
                text = t("Bağlantıyı Kes", "Disconnect"),
                onClick = { vm.disconnect() },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            PrimaryButton(
                text = t("Telefonu Görünür Yap", "Make Phone Discoverable"),
                onClick = { requestDiscoverable() },
                modifier = Modifier.fillMaxWidth(),
                icon = { YcIcon(YcIcon.Signal, Modifier.size(18.dp)) }
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton(
                text = t("HID Aygıtını Yeniden Başlat", "Restart HID Device"),
                onClick = { vm.restartHid() },
                modifier = Modifier.fillMaxWidth(),
                icon = { YcIcon(YcIcon.Refresh, Modifier.size(16.dp)) }
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = t(
                "İpucu: Eşleştirme sırasında bilgisayarda Bluetooth ayarları açık kalsın. Kurumsal bilgisayarlarda Bluetooth kısıtlanmış olabilir.",
                "Tip: Keep Bluetooth settings open on your PC while pairing. Corporate machines may restrict Bluetooth."
            ),
            style = type.micro,
            color = colors.textTertiary
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PairingHero(
    connection: HidConnectionState,
    onOpenController: () -> Unit,
    onDisconnect: () -> Unit
) {
    val colors = YcTheme.colors
    val type = YcTheme.type
    val current = connection
    val connected = current is HidConnectionState.Connected
    val waiting = current is HidConnectionState.Waiting || current is HidConnectionState.Preparing
    val pulse = rememberInfiniteTransition(label = "pairPulse")
    val phase by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2400, easing = FastOutSlowInEasing),
            RepeatMode.Restart
        ),
        label = "phase"
    )
    val statusColor = when {
        connected -> colors.success
        waiting -> colors.accent
        connection is HidConnectionState.Error -> colors.danger
        else -> colors.textTertiary
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        glow = connected || waiting,
        elevation = if (connected) 30.dp else 10.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(168.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val base = size.minDimension / 2f
                drawCircle(
                    Brush.radialGradient(
                        listOf(statusColor.copy(alpha = 0.24f), Color.Transparent),
                        center = center,
                        radius = base
                    ),
                    radius = base,
                    center = center
                )
                if (waiting || connected) {
                    for (i in 0..2) {
                        val progress = (phase + i * 0.33f) % 1f
                        drawCircle(
                            color = statusColor.copy(alpha = (1f - progress) * 0.35f),
                            radius = base * (0.45f + progress * 0.55f),
                            center = center,
                            style = Stroke(width = 1.3.dp.toPx())
                        )
                    }
                }
            }
            GamepadMark(
                size = 104.dp,
                glow = false,
                modifier = Modifier.sharedElementFor("pad-mark"),
                tint = if (waiting || connected) colors.accent else colors.surfaceHigher,
                tint2 = if (waiting || connected) colors.accentSecondary else colors.surfaceHigh
            )
        }

        Text(
            text = when (current) {
                is HidConnectionState.Connected -> t("Bağlandı", "Connected")
                is HidConnectionState.Waiting -> t("Bilgisayar bekleniyor", "Waiting for your PC")
                is HidConnectionState.Preparing -> t("Hazırlanıyor", "Preparing")
                is HidConnectionState.Error -> t("Bağlantı kurulamadı", "Connection failed")
                HidConnectionState.Idle -> t("Başlamaya hazır", "Ready to start")
            },
            style = type.headline,
            color = colors.text,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = when (current) {
                is HidConnectionState.Connected ->
                    t("${current.deviceName} ile bağlantı aktif.", "Connected to ${current.deviceName}.")
                is HidConnectionState.Waiting ->
                    t("Telefon görünür moda alındığında bilgisayarından eşleştir.", "Make the phone discoverable, then pair from your PC.")
                is HidConnectionState.Preparing ->
                    t("HID aygıtı kaydediliyor, birkaç saniye sürer.", "Registering the HID device, this takes a few seconds.")
                is HidConnectionState.Error ->
                    t("Aşağıdaki önerileri dene.", "Try the suggestions below.")
                HidConnectionState.Idle ->
                    t("HID aygıtı henüz başlatılmadı.", "The HID device hasn't started yet.")
            },
            style = type.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        if (connected) {
            Spacer(Modifier.height(14.dp))
            PrimaryButton(
                text = t("Kontrolcüyü Aç", "Open Controller"),
                onClick = onOpenController,
                modifier = Modifier.fillMaxWidth(),
                icon = { YcIcon(YcIcon.Gamepad, Modifier.size(18.dp)) }
            )
        }
    }
}

@Composable
private fun StepRow(
    index: Int,
    title: String,
    description: String,
    done: Boolean,
    active: Boolean,
    action: (@Composable () -> Unit)? = null
) {
    val colors = YcTheme.colors
    val type = YcTheme.type
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    when {
                        done -> colors.success.copy(alpha = 0.18f)
                        active -> colors.accent.copy(alpha = 0.18f)
                        else -> Color.White.copy(alpha = 0.05f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                YcIcon(YcIcon.Check, Modifier.size(15.dp), tint = colors.success, strokeWidth = 2.dp)
            } else {
                Text(
                    text = index.toString(),
                    style = type.label,
                    color = if (active) colors.accent else colors.textTertiary
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = type.title,
                color = if (done || active) colors.text else colors.textSecondary
            )
            Spacer(Modifier.height(3.dp))
            Text(text = description, style = type.bodySmall, color = colors.textTertiary)
            if (action != null) {
                Spacer(Modifier.height(10.dp))
                action()
            }
        }
    }
}

@Composable
private fun StepDivider() {
    Box(
        Modifier
            .padding(start = 42.dp, top = 12.dp, bottom = 12.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.06f))
    )
}

@Composable
private fun ErrorCard(
    failure: HidFailure,
    detail: String?,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onRetry: () -> Unit
) {
    val colors = YcTheme.colors
    val type = YcTheme.type
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            YcIcon(YcIcon.Warning, Modifier.size(20.dp), tint = colors.danger)
            Spacer(Modifier.width(10.dp))
            Text(
                text = when (failure) {
                    HidFailure.PERMISSION -> t("Bluetooth izni gerekli", "Bluetooth permission required")
                    HidFailure.BLUETOOTH_OFF -> t("Bluetooth kapalı", "Bluetooth is off")
                    HidFailure.REGISTER_FAILED -> t("HID aygıtı kaydedilemedi", "Could not register HID device")
                    HidFailure.CONNECT_FAILED -> t("Bağlantı kurulamadı", "Could not connect")
                    HidFailure.UNSUPPORTED -> t("Bu cihaz desteklenmiyor", "This device is not supported")
                    HidFailure.TIMEOUT -> t("Zaman aşımı", "Timed out")
                },
                style = type.title,
                color = colors.text
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (failure) {
                HidFailure.PERMISSION -> t(
                    "Eşleştirme için Bluetooth izinleri gerekiyor.",
                    "Bluetooth permissions are required for pairing."
                )
                HidFailure.BLUETOOTH_OFF -> t(
                    "Bluetooth'u açıp tekrar dene.",
                    "Turn on Bluetooth and try again."
                )
                HidFailure.REGISTER_FAILED -> t(
                    "Bluetooth'u kapatıp açmak genellikle çözer. Sorun sürerse telefonu yeniden başlat.",
                    "Toggling Bluetooth usually fixes this. If it persists, restart the phone."
                )
                HidFailure.CONNECT_FAILED -> t(
                    "Bilgisayarı telefonla yeniden eşleştirmen gerekebilir.",
                    "You may need to pair the computer with the phone again."
                )
                HidFailure.UNSUPPORTED -> t(
                    "Telefon HID Device profilini sunmuyor.",
                    "The phone does not expose the HID Device profile."
                )
                HidFailure.TIMEOUT -> t(
                    "İşlem zaman aşımına uğradı. Tekrar dene.",
                    "The operation timed out. Try again."
                )
            } + (detail?.let { " ($it)" } ?: ""),
            style = type.bodySmall,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (failure) {
                HidFailure.PERMISSION -> SecondaryButton(
                    text = t("İzin Ver", "Allow"),
                    onClick = onRequestPermissions
                )
                HidFailure.BLUETOOTH_OFF -> SecondaryButton(
                    text = t("Bluetooth'u Aç", "Turn On Bluetooth"),
                    onClick = onEnableBluetooth
                )
                else -> SecondaryButton(
                    text = t("Tekrar Dene", "Retry"),
                    onClick = onRetry,
                    icon = { YcIcon(YcIcon.Refresh, Modifier.size(15.dp)) }
                )
            }
            SecondaryButton(
                text = t("Yeniden Başlat", "Restart"),
                onClick = onRetry,
                icon = { YcIcon(YcIcon.Rotate, Modifier.size(15.dp)) }
            )
        }
    }
}

@Composable
private fun PairedHostRow(
    host: HidHostDevice,
    connected: Boolean,
    onConnect: () -> Unit
) {
    val colors = YcTheme.colors
    val type = YcTheme.type
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (connected) colors.success.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                YcIcon(
                    icon = if (host.isComputer) YcIcon.Screen else YcIcon.Bluetooth,
                    modifier = Modifier.size(17.dp),
                    tint = if (connected) colors.success else colors.textSecondary
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(host.name, style = type.title, color = colors.text)
                Text(
                    text = if (connected) t("Bağlı", "Connected") else host.address,
                    style = type.micro,
                    color = if (connected) colors.success else colors.textTertiary
                )
            }
            SecondaryButton(
                text = if (connected) t("Bağlı", "Linked") else t("Bağlan", "Connect"),
                onClick = onConnect,
                enabled = !connected,
                minHeight = 40.dp
            )
        }
    }
}
