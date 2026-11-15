package com.ycontroler.gamepad.features.unsupported

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ycontroler.gamepad.core.design.YcTheme
import com.ycontroler.gamepad.core.i18n.t
import com.ycontroler.gamepad.ui.components.GlassCard
import com.ycontroler.gamepad.ui.components.GamepadMark
import com.ycontroler.gamepad.ui.components.PrimaryButton
import com.ycontroler.gamepad.ui.components.SecondaryButton
import com.ycontroler.gamepad.ui.components.YcBackground
import com.ycontroler.gamepad.ui.components.YcIcon
import com.ycontroler.gamepad.ui.components.YcIcon.Warning

/**
 * Telefon HID Device profilini desteklemiyorsa gösterilen ekran.
 * Neden desteklenmediğini ve nelerin denenebileceğini anlatır.
 */
@Composable
fun UnsupportedScreen(
    onRetry: () -> Unit
) {
    val colors = YcTheme.colors
    val type = YcTheme.type
    val context = LocalContext.current
    val pulse = rememberInfiniteTransition(label = "unsupportedPulse")
    val glow by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "glow"
    )

    YcBackground(intensity = 0.8f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(210.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f
                    drawCircle(
                        Brush.radialGradient(
                            listOf(colors.danger.copy(alpha = 0.20f * glow), Color.Transparent),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = colors.danger.copy(alpha = 0.18f),
                        radius = radius * 0.72f,
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
                GamepadMark(size = 128.dp, glow = false, tint = colors.surfaceHigher, tint2 = colors.surfaceHigh)
                Box(
                    Modifier
                        .size(44.dp)
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    YcIcon(Warning, Modifier.size(34.dp), tint = colors.danger, strokeWidth = 1.8.dp)
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = t(
                    "Telefonun bu özelliği desteklemiyor",
                    "Your phone doesn't support this feature"
                ),
                style = type.display,
                color = colors.text,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = t(
                    "Ycontroler, telefonun Bluetooth HID Device profilini kullanarak kendini gamepad olarak tanıtır. Bu telefonda profil bulunamadı.",
                    "Ycontroler presents itself as a gamepad using your phone's Bluetooth HID Device profile, which was not found on this device."
                ),
                style = type.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(26.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = t("Neler gerekli?", "What's required?"),
                    style = type.title,
                    color = colors.text
                )
                Spacer(Modifier.height(12.dp))
                Requirement(
                    title = t("Android 9 veya üzeri", "Android 9 or newer"),
                    detail = t("HID Device API'si Android 9 ile geldi.", "The HID Device API arrived with Android 9.")
                )
                Requirement(
                    title = t("Üretici desteği", "Manufacturer support"),
                    detail = t(
                        "Bazı üreticiler Bluetooth profilini kısıtlar. Stok Android ve çoğu amiral gemisi modeli destekler.",
                        "Some manufacturers restrict Bluetooth profiles. Stock Android and most flagship models support it."
                    )
                )
                Requirement(
                    title = t("Kapalı geliştirici seçenekleri gerekmez", "No developer options needed"),
                    detail = t(
                        "Sadece Bluetooth açık olmalı; root veya özel yazılım gerekmez.",
                        "Bluetooth just needs to be on; no root or custom software required."
                    )
                )
            }

            Spacer(Modifier.height(14.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    YcIcon(YcIcon.Info, Modifier.size(18.dp), tint = colors.accent)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = t("Ne yapabilirim?", "What can I do?"),
                        style = type.title,
                        color = colors.text
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = t(
                        "Bluetooth'u kapatıp açmayı, telefonu yeniden başlatmayı veya sistem güncellemesini yüklemeyi dene. Hâlâ görünmüyorsa cihazın HID profilini sunmuyor demektir.",
                        "Try toggling Bluetooth, restarting the phone or installing a system update. If it still isn't available, this device doesn't expose the HID profile."
                    ),
                    style = type.bodySmall,
                    color = colors.textSecondary
                )
            }

            Spacer(Modifier.height(26.dp))

            PrimaryButton(
                text = t("Tekrar Kontrol Et", "Check Again"),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                icon = { YcIcon(YcIcon.Refresh, Modifier.size(18.dp)) },
                shine = false
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton(
                text = t("Uygulamayı Kapat", "Close App"),
                onClick = { (context.findActivityOrNull())?.finish() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
            Text(
                text = t(
                    "Ycontroler 1.0 · Bluetooth HID Device",
                    "Ycontroler 1.0 · Bluetooth HID Device"
                ),
                style = type.micro,
                color = colors.textTertiary,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun Requirement(title: String, detail: String) {
    Row(
        modifier = Modifier.padding(bottom = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        YcIcon(
            YcIcon.Check,
            Modifier
                .padding(top = 2.dp)
                .size(14.dp),
            tint = YcTheme.colors.accent,
            strokeWidth = 2.dp
        )
        Column {
            Text(title, style = YcTheme.type.bodyStrong, color = YcTheme.colors.text)
            Text(detail, style = YcTheme.type.bodySmall, color = YcTheme.colors.textSecondary)
        }
    }
}

private fun android.content.Context.findActivityOrNull(): android.app.Activity? {
    var current: android.content.Context? = this
    while (current is android.content.ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}
