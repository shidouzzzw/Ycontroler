package com.ycontroler.gamepad.features.controller

import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ycontroler.gamepad.AppViewModel
import com.ycontroler.gamepad.core.audio.KeySoundPlayer
import com.ycontroler.gamepad.core.design.YcTheme
import com.ycontroler.gamepad.core.i18n.t
import com.ycontroler.gamepad.core.permissions.findActivity
import com.ycontroler.gamepad.data.layout.DefaultLayouts
import com.ycontroler.gamepad.data.settings.OrientationMode
import com.ycontroler.gamepad.data.settings.TriggerMode
import com.ycontroler.gamepad.hid.HidConnectionState
import com.ycontroler.gamepad.hid.HidFailure
import com.ycontroler.gamepad.ui.components.SecondaryButton
import com.ycontroler.gamepad.ui.components.YcIcon
import com.ycontroler.gamepad.ui.components.gamepad.GamepadLayout
import com.ycontroler.gamepad.ui.components.gamepad.PadButton
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Tam ekran kontrolcü.
 *
 * Aktif düzen profili [GamepadLayout] ile çizilir; böylece editörde yapılan değişiklikler
 * anında oyuna yansır. Tüm dokunuşlar doğrudan HID giriş durumuna yazılır, rapor gönderimi
 * ayrı bir coroutine'de hız sınırıyla yapılır.
 */
@Composable
fun ControllerScreen(
    vm: AppViewModel,
    onExit: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLayouts: () -> Unit
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val layouts by vm.layouts.collectAsStateWithLifecycle()
    val connection by vm.connection.collectAsStateWithLifecycle()
    val latency by vm.latencyMs.collectAsStateWithLifecycle()
    val reportsPerSecond by vm.reportsPerSecond.collectAsStateWithLifecycle()
    val battery by vm.batteryPercent.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val colors = YcTheme.colors
    val type = YcTheme.type

    var menuOpen by remember { mutableStateOf(false) }
    var hudVisible by remember { mutableStateOf(settings.hudVisible) }
    var rightStickActive by remember { mutableStateOf(false) }

    val hapticStrength = if (settings.hapticsEnabled) settings.hapticStrength else 0f
    val activeLayout = layouts.firstOrNull { it.id == settings.activeLayoutId }
        ?: layouts.firstOrNull()
        ?: DefaultLayouts.fps

    val hid = vm.hid
    val soundPlayer = remember { KeySoundPlayer() }
    val gyro = remember { GyroStickController(context) }

    DisposableEffect(settings.orientation, settings.keepScreenOn) {
        val window = activity?.window
        val originalOrientation = activity?.requestedOrientation
        if (settings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        activity?.requestedOrientation = when (settings.orientation) {
            OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            OrientationMode.SENSOR -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }
        onDispose {
            vm.releaseAll()
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation =
                originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(settings.brightness) {
        val window = activity?.window ?: return@DisposableEffect onDispose { }
        val previous = window.attributes.screenBrightness
        val params = window.attributes
        params.screenBrightness = settings.brightness
            ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = params
        onDispose {
            val restore = window.attributes
            restore.screenBrightness = previous
            window.attributes = restore
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            soundPlayer.release()
            gyro.stop()
        }
    }

    DisposableEffect(settings.gyroEnabled, settings.gyroSensitivity) {
        if (settings.gyroEnabled) {
            gyro.start(settings.gyroSensitivity) { value ->
                if (!rightStickActive) {
                    hid.input.setStick(false, value)
                    hid.requestSend()
                }
            }
            onDispose { gyro.stop() }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(Unit) {
        if (connection !is HidConnectionState.Connected) vm.startHid()
    }

    LaunchedEffect(hudVisible) {
        if (hudVisible) {
            delay(4800)
            hudVisible = false
        }
    }

    // Uygulama arka plana geçerse takılı tuş kalmmasın.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.releaseAll() }

    BackHandler { if (menuOpen) menuOpen = false else onExit() }

    fun setButton(index: Int, pressed: Boolean) {
        hid.input.setButton(index, pressed)
        hid.requestSend()
        if (pressed && settings.soundEnabled) soundPlayer.tick()
    }

    fun setHat(direction: Int) {
        hid.input.setHat(direction)
        hid.requestSend()
    }

    fun setStick(left: Boolean, value: Offset) {
        if (!left) rightStickActive = value.getDistance() > 0.001f
        hid.input.setStick(left, value)
        hid.requestSend()
    }

    fun setTrigger(left: Boolean, raw: Float) {
        if (settings.triggerMode == TriggerMode.ANALOG) {
            val value = (raw * settings.triggerSensitivity).coerceIn(0f, 1f)
            hid.input.setTrigger(left, value)
            hid.input.setButton(if (left) PadButton.LT else PadButton.RT, value > 0.02f)
        } else {
            val on = raw > 0.35f
            hid.input.setTrigger(left, if (on) 1f else 0f)
            hid.input.setButton(if (left) PadButton.LT else PadButton.RT, on)
        }
        hid.requestSend()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        ControllerBackdrop()

        GamepadLayout(
            layout = activeLayout,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            skin = settings.skin,
            leftConfig = settings.leftStick,
            rightConfig = settings.rightStick,
            hapticStrength = hapticStrength,
            interactive = true,
            opacity = settings.buttonOpacity,
            leftHanded = settings.leftHanded,
            onButton = { index, pressed -> setButton(index, pressed) },
            onHat = { setHat(it) },
            onStick = { left, value -> setStick(left, value) },
            onTrigger = { left, value -> setTrigger(left, value) },
            onL3 = { left, pressed -> setButton(if (left) PadButton.L3 else PadButton.R3, pressed) }
        )

        AnimatedVisibility(
            visible = hudVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { -it / 2 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(200)) { -it / 2 }
        ) {
            Hud(
                connection = connection,
                latencyMs = latency,
                reportsPerSecond = reportsPerSecond,
                batteryPercent = battery,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 4.dp)
            )
        }

        if (!hudVisible) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.42f)
                    .height(34.dp)
                    .statusBarsPadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { hudVisible = true }
            )
        }

        EdgeSwipeHandle(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(26.dp),
            onOpen = { menuOpen = true }
        )

        AnimatedVisibility(
            visible = menuOpen,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(160))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { menuOpen = false }
            )
        }

        AnimatedVisibility(
            visible = menuOpen,
            modifier = Modifier.align(Alignment.CenterStart),
            enter = slideInHorizontally(tween(280)) { -it } + fadeIn(tween(220)),
            exit = slideOutHorizontally(tween(240)) { -it } + fadeOut(tween(180))
        ) {
            ControllerMenu(
                connection = connection,
                layoutName = activeLayout.name,
                hudVisible = hudVisible,
                onToggleHud = {
                    hudVisible = !hudVisible
                    menuOpen = false
                },
                onResume = { menuOpen = false },
                onSettings = {
                    menuOpen = false
                    onOpenSettings()
                },
                onLayouts = {
                    menuOpen = false
                    onOpenLayouts()
                },
                onReconnect = { vm.startHid() },
                onDisconnect = {
                    menuOpen = false
                    vm.disconnect()
                },
                onExit = {
                    menuOpen = false
                    onExit()
                }
            )
        }

        AnimatedVisibility(
            visible = connection !is HidConnectionState.Connected,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(260)) + scaleIn(initialScale = 0.92f, animationSpec = tween(320)),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.96f, animationSpec = tween(180))
        ) {
            DisconnectedOverlay(
                connection = connection,
                onReconnect = { vm.startHid() },
                onExit = onExit
            )
        }

        Text(
            text = activeLayout.name.uppercase(Locale.getDefault()),
            style = type.micro,
            color = colors.text.copy(alpha = 0.28f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 10.dp, end = 16.dp)
        )
    }
}

@Composable
private fun ControllerBackdrop() {
    val colors = YcTheme.colors
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Color.Black)
        drawCircle(
            Brush.radialGradient(
                listOf(colors.accent.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(0f, size.height),
                radius = size.width * 0.7f
            ),
            radius = size.width * 0.7f,
            center = Offset(0f, size.height)
        )
        drawCircle(
            Brush.radialGradient(
                listOf(colors.accentSecondary.copy(alpha = 0.06f), Color.Transparent),
                center = Offset(size.width, 0f),
                radius = size.width * 0.6f
            ),
            radius = size.width * 0.6f,
            center = Offset(size.width, 0f)
        )
        val step = 28.dp.toPx()
        var y = step
        while (y < size.height) {
            var x = step
            while (x < size.width) {
                drawCircle(Color.White.copy(alpha = 0.022f), radius = 1f, center = Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

@Composable
private fun Hud(
    connection: HidConnectionState,
    latencyMs: Float,
    reportsPerSecond: Int,
    batteryPercent: Int,
    modifier: Modifier = Modifier
) {
    val colors = YcTheme.colors
    val type = YcTheme.type
    val connected = connection is HidConnectionState.Connected
    val statusColor = if (connected) colors.success else colors.warning
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(statusColor)
        )
        Text(
            text = (connection as? HidConnectionState.Connected)?.deviceName
                ?: t("Bağlantı yok", "No connection"),
            style = type.micro,
            color = colors.text.copy(alpha = 0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = String.format(Locale.US, "%.1f ms", latencyMs),
            style = type.micro,
            color = colors.textSecondary
        )
        Text(
            text = if (reportsPerSecond > 0) "$reportsPerSecond Hz" else "—",
            style = type.micro,
            color = colors.textTertiary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            YcIcon(YcIcon.Battery, Modifier.size(12.dp), tint = colors.textTertiary, strokeWidth = 1.5.dp)
            Spacer(Modifier.width(3.dp))
            Text(
                text = if (batteryPercent > 0) "$batteryPercent" else "—",
                style = type.micro,
                color = colors.textTertiary
            )
        }
    }
}

@Composable
private fun EdgeSwipeHandle(
    modifier: Modifier,
    onOpen: () -> Unit
) {
    val colors = YcTheme.colors
    var accumulated by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                accumulated = 0f
                down.consume()
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    accumulated += change.positionChange().x
                    if (accumulated > 42f) {
                        onOpen()
                        accumulated = 0f
                        break
                    }
                    change.consume()
                }
            }
        },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(colors.accent.copy(alpha = 0.35f))
        )
    }
}

@Composable
private fun ControllerMenu(
    connection: HidConnectionState,
    layoutName: String,
    hudVisible: Boolean,
    onToggleHud: () -> Unit,
    onResume: () -> Unit,
    onSettings: () -> Unit,
    onLayouts: () -> Unit,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    onExit: () -> Unit
) {
    val colors = YcTheme.colors
    val type = YcTheme.type
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(292.dp)
            .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
            .background(Brush.horizontalGradient(listOf(colors.surfaceHigh, colors.surface)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(18.dp)
    ) {
        Text("YCONTROLER", style = type.label, color = colors.accent)
        Spacer(Modifier.height(4.dp))
        Text(
            text = (connection as? HidConnectionState.Connected)?.deviceName
                ?: t("Bağlı değil", "Not connected"),
            style = type.headline,
            color = colors.text
        )
        Text(
            text = t("Düzen: $layoutName", "Layout: $layoutName"),
            style = type.micro,
            color = colors.textTertiary
        )
        Spacer(Modifier.height(18.dp))

        MenuRow(YcIcon.Gamepad, t("Devam Et", "Resume"), onResume)
        MenuRow(
            YcIcon.Gauge,
            if (hudVisible) t("Göstergeyi Gizle", "Hide HUD") else t("Göstergeyi Göster", "Show HUD"),
            onToggleHud
        )
        MenuRow(YcIcon.Grid, t("Düzenler", "Layouts"), onLayouts)
        MenuRow(YcIcon.Sliders, t("Ayarlar", "Settings"), onSettings)
        MenuRow(YcIcon.Refresh, t("Yeniden Bağlan", "Reconnect"), onReconnect)
        MenuRow(YcIcon.Link, t("Bağlantıyı Kes", "Disconnect"), onDisconnect)
        Spacer(Modifier.height(10.dp))
        MenuRow(YcIcon.Back, t("Kontrolcüden Çık", "Exit Controller"), onExit, danger = true)
    }
}

@Composable
private fun MenuRow(
    icon: YcIcon,
    label: String,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    val colors = YcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        YcIcon(
            icon = icon,
            modifier = Modifier.size(18.dp),
            tint = if (danger) colors.danger else colors.textSecondary
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = YcTheme.type.bodyStrong,
            color = if (danger) colors.danger else colors.text
        )
    }
}

@Composable
private fun DisconnectedOverlay(
    connection: HidConnectionState,
    onReconnect: () -> Unit,
    onExit: () -> Unit
) {
    val colors = YcTheme.colors
    val type = YcTheme.type
    val error = connection as? HidConnectionState.Error
    Column(
        modifier = Modifier
            .width(360.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(colors.surface.copy(alpha = 0.96f))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.warning.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            YcIcon(YcIcon.Warning, Modifier.size(24.dp), tint = colors.warning)
        }
        Spacer(Modifier.height(14.dp))
        Text(t("Bağlantı koptu", "Connection lost"), style = type.headline, color = colors.text)
        Spacer(Modifier.height(6.dp))
        Text(
            text = t(
                "Tüm tuşlar serbest bırakıldı. Bilgisayar bağlantıyı yeniden kurmayı deniyor.",
                "All buttons were released. Your PC is attempting to reconnect."
            ),
            style = type.bodySmall,
            color = colors.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = when (error.failure) {
                    HidFailure.BLUETOOTH_OFF -> t("Bluetooth kapalı.", "Bluetooth is off.")
                    HidFailure.PERMISSION -> t("Bluetooth izni verilmedi.", "Bluetooth permission missing.")
                    else -> t("Yeniden bağlanmayı deneyebilirsin.", "You can try reconnecting.")
                },
                style = type.micro,
                color = colors.textTertiary
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(
                text = t("Yeniden Bağlan", "Reconnect"),
                onClick = onReconnect,
                icon = { YcIcon(YcIcon.Refresh, Modifier.size(15.dp)) }
            )
            SecondaryButton(text = t("Geri Dön", "Go Back"), onClick = onExit)
        }
    }
}
