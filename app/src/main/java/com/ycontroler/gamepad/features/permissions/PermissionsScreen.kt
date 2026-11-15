package com.ycontroler.gamepad.features.permissions

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ycontroler.gamepad.core.design.YcTheme
import com.ycontroler.gamepad.core.i18n.t
import com.ycontroler.gamepad.core.permissions.BluetoothPermissionController
import com.ycontroler.gamepad.core.permissions.bluetoothRuntimePermissions
import com.ycontroler.gamepad.core.permissions.openAppSettings
import com.ycontroler.gamepad.ui.components.GlassCard
import com.ycontroler.gamepad.ui.components.PrimaryButton
import com.ycontroler.gamepad.ui.components.SecondaryButton
import com.ycontroler.gamepad.ui.components.YcIcon
import com.ycontroler.gamepad.ui.components.YcScreen

/**
 * Bluetooth izinleri için tasarlanmış ekran.
 * Her izin ayrı kartta, neden gerekli olduğu açıkça anlatılır.
 * Reddedilirse ne kaybedileceği ve nasıl düzeltileceği gösterilir.
 */
@Composable
fun PermissionsScreen(
    permissions: BluetoothPermissionController,
    onBack: () -> Unit,
    onGranted: () -> Unit
) {
    val context = LocalContext.current
    val colors = YcTheme.colors
    val type = YcTheme.type

    // Android 11 ve altında çalışma zamanı izni gerekmez.
    LaunchedEffect(permissions.allGranted, bluetoothRuntimePermissions) {
        if (permissions.allGranted) onGranted()
    }

    val pulse = rememberInfiniteTransition(label = "permissionPulse")
    val pulseValue by pulse.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    YcScreen(
        title = t("Bağlantı İzinleri", "Connection Permissions"),
        onBack = onBack
    ) {
        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(190.dp)) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    Brush.radialGradient(
                        listOf(colors.accent.copy(alpha = 0.28f * pulseValue), colors.accent.copy(alpha = 0f)),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    color = colors.accent.copy(alpha = 0.22f),
                    radius = radius * 0.62f * pulseValue,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = colors.accent.copy(alpha = 0.12f),
                    radius = radius * 0.82f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            Box(
                Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(listOf(colors.accent.copy(alpha = 0.30f), colors.accent.copy(alpha = 0.10f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                YcIcon(
                    icon = YcIcon.Bluetooth,
                    modifier = Modifier.size(38.dp),
                    tint = colors.accent,
                    strokeWidth = 1.8.dp
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = t(
                "Gamepad olarak bağlanmak için iki izin gerekiyor",
                "Two permissions are required to connect as a gamepad"
            ),
            style = type.headline,
            color = colors.text,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = t(
                "Bu izinler yalnızca Bluetooth bağlantısı için kullanılır. Konum, kişiler veya başka bir veri istenmez.",
                "These permissions are used only for the Bluetooth connection. No location, contacts or other data is requested."
            ),
            style = type.body,
            color = colors.textSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(26.dp))

        PermissionCard(
            icon = YcIcon.Link,
            title = t("Yakındaki cihazlar", "Nearby devices"),
            description = t(
                "Bilgisayarını bulmak, bağlanmak ve kopan bağlantıyı yeniden kurmak için.",
                "To find your PC, connect to it and restore a dropped connection."
            ),
            granted = permissions.allGranted
        )
        Spacer(Modifier.height(12.dp))
        PermissionCard(
            icon = YcIcon.Signal,
            title = t("Görünür olma", "Be discoverable"),
            description = t(
                "Telefonunu bilgisayarın Bluetooth listesinde gösterebilmek için.",
                "To show your phone in your PC's Bluetooth list."
            ),
            granted = permissions.allGranted
        )

        if (permissions.permanentlyDenied) {
            Spacer(Modifier.height(20.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    YcIcon(YcIcon.Warning, Modifier.size(20.dp), tint = colors.warning)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = t("İzin kalıcı olarak reddedildi", "Permission permanently denied"),
                        style = type.title,
                        color = colors.warning
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = t(
                        "Android, iki redden sonra izin penceresini göstermez. Ayarlar > Uygulamalar > Ycontroler > İzinler yolundan Bluetooth iznini açabilirsin.",
                        "After two denials Android stops showing the prompt. Enable Bluetooth under Settings > Apps > Ycontroler > Permissions."
                    ),
                    style = type.body,
                    color = colors.textSecondary
                )
            }
        } else if (!permissions.allGranted && permissions.requestCount > 0) {
            Spacer(Modifier.height(20.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    YcIcon(YcIcon.Info, Modifier.size(20.dp), tint = colors.textSecondary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = t("İzin verilmedi", "Permission not granted"),
                        style = type.title,
                        color = colors.text
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = t(
                        "İzin vermeden kontrolcü bağlanamaz. Dilersen tekrar deneyebilir ya da izni sistem ayarlarından açabilirsin.",
                        "The controller cannot connect without permission. You can try again or enable it from system settings."
                    ),
                    style = type.body,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        if (bluetoothRuntimePermissions.isEmpty()) {
            Text(
                text = t(
                    "Bu Android sürümünde ek izin gerekmiyor.",
                    "This Android version does not require runtime permissions."
                ),
                style = type.bodySmall,
                color = colors.textTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            PrimaryButton(
                text = if (permissions.permanentlyDenied) {
                    t("Ayarları Aç", "Open Settings")
                } else {
                    t("İzin Ver", "Allow")
                },
                onClick = {
                    if (permissions.permanentlyDenied) context.openAppSettings() else permissions.request()
                },
                modifier = Modifier.fillMaxWidth(),
                icon = { YcIcon(YcIcon.Lock, Modifier.size(18.dp)) },
                shine = false
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton(
                text = t("Şimdi Değil", "Not Now"),
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            YcIcon(YcIcon.Lock, Modifier.size(14.dp), tint = colors.textTertiary)
            Spacer(Modifier.width(6.dp))
            Text(
                text = t("Veriler cihazda kalır, hiçbir yere gönderilmez.", "Data stays on device, never uploaded."),
                style = type.micro,
                color = colors.textTertiary
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: YcIcon,
    title: String,
    description: String,
    granted: Boolean
) {
    val colors = YcTheme.colors
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                YcIcon(icon, Modifier.size(20.dp), tint = colors.accent)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = YcTheme.type.title, color = colors.text)
                Spacer(Modifier.height(3.dp))
                Text(description, style = YcTheme.type.bodySmall, color = colors.textSecondary)
            }
            Spacer(Modifier.width(8.dp))
            YcIcon(
                icon = if (granted) YcIcon.Check else YcIcon.Close,
                modifier = Modifier.size(18.dp),
                tint = if (granted) colors.success else colors.textTertiary
            )
        }
    }
}
