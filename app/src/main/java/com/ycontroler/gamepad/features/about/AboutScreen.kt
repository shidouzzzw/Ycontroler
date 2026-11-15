package com.ycontroler.gamepad.features.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.ycontroler.gamepad.BuildConfig
import com.ycontroler.gamepad.core.design.YcTheme
import com.ycontroler.gamepad.core.i18n.t
import com.ycontroler.gamepad.ui.components.GamepadMark
import com.ycontroler.gamepad.ui.components.GlassCard
import com.ycontroler.gamepad.ui.components.PrimaryButton
import com.ycontroler.gamepad.ui.components.SecondaryButton
import com.ycontroler.gamepad.ui.components.SectionHeader
import com.ycontroler.gamepad.ui.components.YcIcon
import com.ycontroler.gamepad.ui.components.YcScreen

private data class FaqItem(val question: String, val answer: String)

private fun faqItems(): List<FaqItem> = listOf(
    FaqItem(
        "Telefonum neden desteklenmiyor?",
        "Ycontroler, Bluetooth HID Device profilini kullanır. Android 9 ile gelen bu profil bazı üreticilerde kapalıdır. Stok Android ve çoğu amiral gemisi modelde çalışır."
    ),
    FaqItem(
        "Bilgisayar telefonu görmüyor",
        "Telefonu 5 dakika görünür moda al, bilgisayarda Bluetooth ayarlarını açık tut. Eski eşleşmeyi kaldırıp yeniden dene. Wi-Fi 2.4 GHz yoğunsa Bluetooth'u etkileyebilir."
    ),
    FaqItem(
        "Bağlandı ama tuşlar çalışmıyor",
        "Kontrolcü ekranının açık olduğundan emin ol. Oyun penceresi odaktayken bir gamepad test sitesinde tuşları doğrula. Gerekirse HID aygıtını yeniden başlat."
    ),
    FaqItem(
        "Tuş takılı kaldı",
        "Bağlantı koptuğunda uygulama tüm tuşları otomatik serbest bırakır. Kontrolcüden çıkıp yeniden açmak durumu sıfırlar."
    ),
    FaqItem(
        "Gecikme yüksek",
        "Ayarlardan rapor hızını 250 Hz yap. Telefon ile bilgisayar arasında engel olmasın, arka planda ağır uygulamaları kapat."
    ),
    FaqItem(
        "Pil neden hızlı bitiyor?",
        "AMOLED siyah temayı, daha düşük rapor hızını ve kapalı titreşimi dene. Ekran parlaklığını düşürmek de belirgin fark yaratır."
    ),
    FaqItem(
        "Kendi düzenimi nasıl yaparım?",
        "Düzenler ekranında tuşları sürükleyebilir, boyut ve saydamlık ayarlayabilir, yeni kontrol ekleyebilirsin. Kaydettiğin düzen kontrolcü ekranına anında yansır."
    ),
    FaqItem(
        "Ayarlarımı yedekleyebilir miyim?",
        "Ayarlar > Veri ve Profiller bölümünden JSON olarak dışa aktarabilir, başka bir cihazda içe aktarabilirsin. Düzenler de yedeğe dahildir."
    ),
    FaqItem(
        "Jiroskop nasıl çalışır?",
        "Sensör bölümünden jiroskobu açtığında sağ çubuk telefonun hareketini takip eder. Çubuğa dokunduğunda jiroskop devre dışı kalır."
    )
)

/**
 * Yardım, SSS ve sorun giderme ekranı. Cevaplar animasyonlu akordeon içinde açılır.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenPairing: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = YcTheme.colors
    val type = YcTheme.type
    val items = faqItems()
    var expanded by remember { mutableStateOf<Int?>(0) }

    YcScreen(
        title = t("Yardım & SSS", "Help & FAQ"),
        onBack = onBack
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth(), glow = true, elevation = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GamepadMark(size = 64.dp, glow = false)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("YCONTROLER", style = type.label, color = colors.accent)
                    Text(
                        text = t("Sanal Bluetooth Gamepad", "Virtual Bluetooth Gamepad"),
                        style = type.title,
                        color = colors.text
                    )
                    Text(
                        text = t("Sürüm ${BuildConfig.VERSION_NAME}", "Version ${BuildConfig.VERSION_NAME}"),
                        style = type.micro,
                        color = colors.textTertiary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = t(
                    "Telefonunu bilgisayarına sürücü kurmadan gamepad olarak bağlar. Tüm veriler cihazında kalır.",
                    "Connects your phone to your PC as a gamepad without drivers. All data stays on your device."
                ),
                style = type.bodySmall,
                color = colors.textSecondary
            )
        }

        Spacer(Modifier.height(26.dp))
        SectionHeader(
            label = t("Hızlı Başlangıç", "Quick Start"),
            title = t("3 Adımda Eşleştir", "Pair in 3 Steps")
        )
        Spacer(Modifier.height(12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            StepRow(1, t("Telefonu görünür yap", "Make the phone discoverable"), t("Eşleştirme ekranındaki düğmeyi kullan.", "Use the button on the pairing screen."))
            StepRow(2, t("Bilgisayardan eşleştir", "Pair from your PC"), t("Bluetooth > Cihaz ekle > Ycontroler.", "Bluetooth > Add device > Ycontroler."))
            StepRow(3, t("Kontrolcüyü aç", "Open the controller"), t("Bağlantı kurulunca kontrolcü ekranına geç.", "Switch to the controller once connected."), last = true)
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                text = t("Eşleştirme", "Pairing"),
                onClick = onOpenPairing,
                modifier = Modifier.weight(1f),
                icon = { YcIcon(YcIcon.Bluetooth, Modifier.size(17.dp)) },
                shine = false
            )
            SecondaryButton(
                text = t("Ayarlar", "Settings"),
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f),
                icon = { YcIcon(YcIcon.Sliders, Modifier.size(16.dp)) }
            )
        }

        Spacer(Modifier.height(28.dp))
        SectionHeader(
            label = t("SSS", "FAQ"),
            title = t("Sık Sorulan Sorular", "Frequently Asked")
        )
        Spacer(Modifier.height(12.dp))
        items.forEachIndexed { index, item ->
            FaqRow(
                item = item,
                expanded = expanded == index,
                onToggle = { expanded = if (expanded == index) null else index }
            )
            if (index != items.lastIndex) Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(28.dp))
        SectionHeader(
            label = t("Sorun Giderme", "Troubleshooting"),
            title = t("Hâlâ Çalışmıyorsa", "If It Still Doesn't Work")
        )
        Spacer(Modifier.height(12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Bullet(t("Bluetooth'u kapatıp aç", "Toggle Bluetooth off and on"), YcIcon.Refresh)
            Bullet(t("Telefonu yeniden başlat", "Restart the phone"), YcIcon.Rotate)
            Bullet(t("Eski eşleşmeyi bilgisayardan kaldır", "Remove the old pairing on the PC"), YcIcon.Trash)
            Bullet(t("HID aygıtını yeniden başlat (Eşleştirme ekranı)", "Restart the HID device (Pairing screen)"), YcIcon.Zap)
            Bullet(t("Farklı bir Bluetooth adaptörü dene", "Try a different Bluetooth adapter"), YcIcon.Bluetooth)
            Spacer(Modifier.height(4.dp))
            Text(
                text = t(
                    "Sorun devam ederse cihazın HID Device profilini sunmuyor olabilir.",
                    "If it persists, your device may not expose the HID Device profile."
                ),
                style = type.bodySmall,
                color = colors.textSecondary
            )
        }

        Spacer(Modifier.height(28.dp))
        SectionHeader(
            label = t("Hakkında", "About"),
            title = t("Uygulama", "Application")
        )
        Spacer(Modifier.height(12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            InfoRow(t("Sürüm", "Version"), BuildConfig.VERSION_NAME)
            InfoRow(t("Paket", "Package"), BuildConfig.APPLICATION_ID)
            InfoRow(t("Min. Android", "Min. Android"), "9.0 (API 28)")
            InfoRow(t("Hedef Android", "Target Android"), "15 (API 35)")
            Spacer(Modifier.height(10.dp))
            Text(
                text = t(
                    "Jetpack Compose, Hilt ve kotlinx.serialization ile geliştirildi. Arayüz yazı tipi Sora (SIL Open Font License 1.1).",
                    "Built with Jetpack Compose, Hilt and kotlinx.serialization. UI typeface Sora (SIL Open Font License 1.1)."
                ),
                style = type.micro,
                color = colors.textTertiary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = t(
                    "Gizlilik: Hiçbir veri toplanmaz, hiçbir ağ isteği yapılmaz. Tüm ayarlar ve düzenler cihazda saklanır.",
                    "Privacy: No data is collected and no network requests are made. Settings and layouts stay on device."
                ),
                style = type.micro,
                color = colors.textTertiary
            )
        }

        Spacer(Modifier.height(26.dp))
        Text(
            text = "Ycontroler · Bluetooth HID Device",
            style = type.micro,
            color = colors.textTertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun StepRow(index: Int, title: String, detail: String, last: Boolean = false) {
    val colors = YcTheme.colors
    Row(modifier = Modifier.padding(bottom = if (last) 0.dp else 12.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(colors.accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text("$index", style = YcTheme.type.micro, color = colors.accent)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = YcTheme.type.bodyStrong, color = colors.text)
            Text(detail, style = YcTheme.type.bodySmall, color = colors.textTertiary)
        }
    }
}

@Composable
private fun FaqRow(item: FaqItem, expanded: Boolean, onToggle: () -> Unit) {
    val colors = YcTheme.colors
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f),
        label = "faqChevron"
    )
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.question,
                style = YcTheme.type.bodyStrong,
                color = colors.text,
                modifier = Modifier.weight(1f)
            )
            YcIcon(
                icon = YcIcon.ChevronDown,
                modifier = Modifier
                    .size(17.dp)
                    .rotate(rotation),
                tint = colors.textTertiary
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(220)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
        ) {
            Text(
                text = item.answer,
                style = YcTheme.type.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun Bullet(text: String, icon: YcIcon) {
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        YcIcon(icon, Modifier.size(15.dp), tint = YcTheme.colors.accent, strokeWidth = 1.6.dp)
        Spacer(Modifier.width(10.dp))
        Text(text, style = YcTheme.type.bodySmall, color = YcTheme.colors.textSecondary)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(label, style = YcTheme.type.bodySmall, color = YcTheme.colors.textTertiary, modifier = Modifier.weight(1f))
        Text(value, style = YcTheme.type.bodySmall, color = YcTheme.colors.textSecondary)
    }
}
