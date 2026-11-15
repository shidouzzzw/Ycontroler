package com.ycontroler.gamepad.features.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ycontroler.gamepad.core.design.YcTheme
import com.ycontroler.gamepad.core.i18n.t
import com.ycontroler.gamepad.data.settings.YcSettings
import com.ycontroler.gamepad.hid.HidConnectionState
import com.ycontroler.gamepad.hid.HidHostDevice
import com.ycontroler.gamepad.ui.components.GamepadMark
import com.ycontroler.gamepad.ui.components.GlassCard
import com.ycontroler.gamepad.ui.components.IconButtonGlass
import com.ycontroler.gamepad.ui.components.sharedElementFor
import com.ycontroler.gamepad.ui.components.PrimaryButton
import com.ycontroler.gamepad.ui.components.SecondaryButton
import com.ycontroler.gamepad.ui.components.SectionHeader
import com.ycontroler.gamepad.ui.components.YcBackground
import com.ycontroler.gamepad.ui.components.YcIcon
import java.util.Locale

/**
 * Ana ekran: bağlantı durumu, eşleşmiş bilgisayarlar ve hızlı erişimler.
 * Durum değişimleri renk, ışıma ve metinle birlikte yumuşak animasyonla yansıtılır.
 */
@Composable
fun HomeScreen(
    settings: YcSettings,
    connection: HidConnectionState,
    hosts: List<HidHostDevice>,
    latencyMs: Float,
    reportsPerSecond: Int,
    batteryPercent: Int,
    onConnect: () -> Unit,
    onOpenPairing: () -> Unit,
    onOpenController: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLayouts: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenComponents: () -> Unit,
    onConnectToHost: (String) -> Unit
) {
    val colors = YcTheme.colors
    val type = YcTheme.type
    val dims = YcTheme.dims
    val connected = connection is HidConnectionState.Connected

    YcBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dims.gutter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dims.l, bottom = dims.xl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GamepadMark(
                    size = 40.dp,
                    glow = false,
                    modifier = Modifier.sharedElementFor("pad-mark")
                )
                Spacer(Modifier.width(dims.m))
                Column(Modifier.weight(1f)) {
                    Text("YCONTROLER", style = type.label, color = colors.textSecondary)
                    Text(
                        text = t("Sanal Gamepad", "Virtual Gamepad"),
                        style = type.title,
                        color = colors.text
                    )
                }
                IconButtonGlass(
                    icon = YcIcon.Gear,
                    onClick = onOpenSettings,
                    contentDescription = t("Ayarlar", "Settings")
                )
            }

            ConnectionHero(
                connection = connection,
                latencyMs = latencyMs,
                reportsPerSecond = reportsPerSecond,
                batteryPercent = batteryPercent
            )

            Spacer(Modifier.height(dims.l))

            val ctaText = when {
                connected -> t("Kontrolcüyü Aç", "Open Controller")
                connection is HidConnectionState.Waiting || connection is HidConnectionState.Preparing ->
                    t("Eşleştirmeye Devam Et", "Continue Pairing")
                else -> t("Bağlan", "Connect")
            }
            val ctaAction = when {
                connected -> onOpenController
                connection is HidConnectionState.Waiting || connection is HidConnectionState.Preparing -> onOpenPairing
                else -> onConnect
            }
            PrimaryButton(
                text = ctaText,
                onClick = ctaAction,
                modifier = Modifier.fillMaxWidth(),
                icon = {
                    YcIcon(
                        if (connected) YcIcon.Gamepad else YcIcon.Link,
                        Modifier.size(19.dp)
                    )
                }
            )

            Spacer(Modifier.height(dims.xxl))

            SectionHeader(
                label = t("Eşleşmiş", "Paired"),
                title = t("Bilgisayarlar", "Computers")
            )
            Spacer(Modifier.height(dims.m))

            if (hosts.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.accent.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            YcIcon(YcIcon.Bluetooth, Modifier.size(20.dp), tint = colors.accent)
                        }
                        Spacer(Modifier.width(dims.m))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = t("Henüz eşleşmiş cihaz yok", "No paired devices yet"),
                                style = type.title,
                                color = colors.text
                            )
                            Text(
                                text = t(
                                    "İlk eşleştirmede bilgisayarın burada görünecek.",
                                    "Your PC will appear here after the first pairing."
                                ),
                                style = type.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            } else {
                hosts.take(3).forEachIndexed { index, host ->
                    if (index > 0) Spacer(Modifier.height(dims.s))
                    HostRow(
                        host = host,
                        connected = connected && connection is HidConnectionState.Connected &&
                            connection.address == host.address,
                        onConnect = { onConnectToHost(host.address) },
                        onOpenPairing = onOpenPairing
                    )
                }
            }

            Spacer(Modifier.height(dims.xxl))

            SectionHeader(
                label = t("Kısayollar", "Shortcuts"),
                title = t("Hızlı Erişim", "Quick Access")
            )
            Spacer(Modifier.height(dims.m))
            Row(horizontalArrangement = Arrangement.spacedBy(dims.s)) {
                ShortcutTile(
                    modifier = Modifier.weight(1f),
                    icon = YcIcon.Grid,
                    label = t("Düzenler", "Layouts"),
                    onClick = onOpenLayouts
                )
                ShortcutTile(
                    modifier = Modifier.weight(1f),
                    icon = YcIcon.Sliders,
                    label = t("Ayarlar", "Settings"),
                    onClick = onOpenSettings
                )
                ShortcutTile(
                    modifier = Modifier.weight(1f),
                    icon = YcIcon.Info,
                    label = t("Yardım", "Help"),
                    onClick = onOpenHelp
                )
            }

            Spacer(Modifier.height(dims.xl))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = t("AKTİF DÜZEN", "ACTIVE LAYOUT"),
                            style = type.label,
                            color = colors.textTertiary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = settings.activeLayoutId.uppercase(Locale.getDefault()),
                            style = type.title,
                            color = colors.text
                        )
                    }
                    SecondaryButton(
                        text = t("Düzenle", "Edit"),
                        onClick = onOpenLayouts,
                        icon = { YcIcon(YcIcon.Edit, Modifier.size(16.dp)) }
                    )
                }
            }

            Spacer(Modifier.height(dims.l))
            Text(
                text = t("Bileşen galerisi", "Component gallery"),
                style = type.bodySmall,
                color = colors.textTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onOpenComponents)
                    .padding(vertical = 8.dp)
            )

            Spacer(Modifier.height(dims.xxl))
        }
    }
}

@Composable
private fun ConnectionHero(
    connection: HidConnectionState,
    latencyMs: Float,
    reportsPerSecond: Int,
    batteryPercent: Int
) {
    val colors = YcTheme.colors
    val type = YcTheme.type
    val connected = connection is HidConnectionState.Connected
    val statusColor by animateColorAsState(
        targetValue = when (connection) {
            is HidConnectionState.Connected -> colors.success
            is HidConnectionState.Waiting, is HidConnectionState.Preparing -> colors.accent
            is HidConnectionState.Error -> colors.danger
            else -> colors.textTertiary
        },
        animationSpec = tween(400),
        label = "statusColor"
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        glow = connected,
        elevation = if (connected) 26.dp else 6.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusOrb(active = connected, waiting = connection is HidConnectionState.Waiting, color = statusColor)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = when (connection) {
                        is HidConnectionState.Connected -> t("BAĞLI", "CONNECTED")
                        is HidConnectionState.Waiting -> t("BEKLENİYOR", "WAITING")
                        is HidConnectionState.Preparing -> t("HAZIRLANIYOR", "PREPARING")
                        is HidConnectionState.Error -> t("HATA", "ERROR")
                        HidConnectionState.Idle -> t("BAĞLI DEĞİL", "NOT CONNECTED")
                    },
                    style = type.label,
                    color = statusColor
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = when (connection) {
                        is HidConnectionState.Connected -> connection.deviceName
                        is HidConnectionState.Waiting -> t("Bilgisayarını beklemede", "Waiting for your PC")
                        is HidConnectionState.Preparing -> t("Bağlantı hazırlanıyor", "Preparing connection")
                        is HidConnectionState.Error -> t("Bağlantı kurulamadı", "Connection failed")
                        HidConnectionState.Idle -> t("Bilgisayarını bağla", "Connect your PC")
                    },
                    style = type.headline,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when (connection) {
                        is HidConnectionState.Connected -> connection.address
                        is HidConnectionState.Error -> t(
                            "Ayrıntılar için eşleştirme ekranını aç.",
                            "Open the pairing screen for details."
                        )
                        else -> t(
                            "Kablosuz, sürücüsüz, düşük gecikme.",
                            "Wireless, driver-free, low latency."
                        )
                    },
                    style = type.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric(
                modifier = Modifier.weight(1f),
                icon = YcIcon.Gauge,
                value = String.format(Locale.US, "%.1f ms", latencyMs),
                label = t("Gecikme", "Latency")
            )
            Metric(
                modifier = Modifier.weight(1f),
                icon = YcIcon.Zap,
                value = if (reportsPerSecond > 0) "$reportsPerSecond Hz" else "—",
                label = t("Rapor", "Report")
            )
            Metric(
                modifier = Modifier.weight(1f),
                icon = YcIcon.Battery,
                value = if (batteryPercent > 0) "%$batteryPercent" else "—",
                label = t("Pil", "Battery")
            )
        }
    }
}

@Composable
private fun Metric(
    modifier: Modifier,
    icon: YcIcon,
    value: String,
    label: String
) {
    val colors = YcTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.035f))
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        YcIcon(icon, Modifier.size(14.dp), tint = colors.textTertiary, strokeWidth = 1.5.dp)
        Spacer(Modifier.height(6.dp))
        Text(value, style = YcTheme.type.number, color = colors.text)
        Text(label, style = YcTheme.type.micro, color = colors.textTertiary)
    }
}

/** Nabız atan durum göstergesi. Bağlıyken genişleyen halkalar çizer. */
@Composable
private fun StatusOrb(active: Boolean, waiting: Boolean, color: Color) {
    val pulse = rememberInfiniteTransition(label = "statusPulse")
    val phase by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing),
            RepeatMode.Restart
        ),
        label = "phase"
    )
    Canvas(Modifier.size(58.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val base = size.minDimension / 2f
        drawCircle(
            Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.30f), Color.Transparent),
                center = center,
                radius = base
            ),
            radius = base,
            center = center
        )
        if (active || waiting) {
            val ringRadius = base * (0.55f + phase * 0.45f)
            drawCircle(
                color = color.copy(alpha = (1f - phase) * 0.5f),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 1.4.dp.toPx())
            )
        }
        drawCircle(
            color = color.copy(alpha = 0.22f),
            radius = base * 0.46f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(color = color, radius = base * 0.22f, center = center)
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = base * 0.08f,
            center = center - Offset(base * 0.05f, base * 0.07f)
        )
    }
}

@Composable
private fun HostRow(
    host: HidHostDevice,
    connected: Boolean,
    onConnect: () -> Unit,
    onOpenPairing: () -> Unit
) {
    val colors = YcTheme.colors
    val type = YcTheme.type
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (connected) colors.success.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                YcIcon(
                    icon = if (host.isComputer) YcIcon.Screen else YcIcon.Bluetooth,
                    modifier = Modifier.size(19.dp),
                    tint = if (connected) colors.success else colors.textSecondary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(host.name, style = type.title, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = if (connected) {
                        t("Bağlı", "Connected")
                    } else if (host.isComputer) {
                        t("Bilgisayar", "Computer")
                    } else {
                        host.address
                    },
                    style = type.bodySmall,
                    color = if (connected) colors.success else colors.textTertiary
                )
            }
            Spacer(Modifier.width(8.dp))
            if (connected) {
                SecondaryButton(text = t("Aç", "Open"), onClick = onOpenPairing)
            } else {
                SecondaryButton(text = t("Bağlan", "Connect"), onClick = onConnect)
            }
        }
    }
}

@Composable
private fun ShortcutTile(
    modifier: Modifier,
    icon: YcIcon,
    label: String,
    onClick: () -> Unit
) {
    val colors = YcTheme.colors
    GlassCard(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            YcIcon(icon, Modifier.size(18.dp), tint = colors.accent)
        }
        Spacer(Modifier.height(12.dp))
        Text(label, style = YcTheme.type.bodyStrong, color = colors.text)
    }
}
