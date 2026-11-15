package com.ycontroler.gamepad.features.onboarding

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ycontroler.gamepad.core.design.YcTheme
import com.ycontroler.gamepad.core.i18n.t
import com.ycontroler.gamepad.data.layout.DefaultLayouts
import com.ycontroler.gamepad.ui.components.GamepadMark
import com.ycontroler.gamepad.ui.components.GlassCard
import com.ycontroler.gamepad.ui.components.PrimaryButton
import com.ycontroler.gamepad.ui.components.SecondaryButton
import com.ycontroler.gamepad.ui.components.YcBackground
import com.ycontroler.gamepad.ui.components.YcIcon
import com.ycontroler.gamepad.ui.components.gamepad.GamepadButton
import com.ycontroler.gamepad.ui.components.gamepad.GamepadLayout
import com.ycontroler.gamepad.ui.components.gamepad.GamepadSkin
import kotlin.math.abs
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val eyebrow: String,
    val title: String,
    val description: String
)

private fun pages(): List<OnboardingPage> = listOf(
    OnboardingPage(
        eyebrow = "YCONTROLER",
        title = "Cebindeki profesyonel gamepad",
        description = "Telefonunu bilgisayarına Bluetooth HID gamepad olarak bağla. Sürücü yok, kablo yok, gecikme yok."
    ),
    OnboardingPage(
        eyebrow = "3.5D",
        title = "Basınca gerçekten inen tuşlar",
        description = "Katmanlı gölge, speküler parlama ve yay animasyonu. Her tuş fiziksel bir derinlik hissi verir."
    ),
    OnboardingPage(
        eyebrow = "EŞLEŞTİRME",
        title = "PC ile 3 adımda eşleş",
        description = "Telefonu görünür yap, bilgisayarın Bluetooth menüsünden Ycontroler'ı seç, bağlantı hazır."
    ),
    OnboardingPage(
        eyebrow = "DÜZENLER",
        title = "Kendi düzenini kur",
        description = "Tuşları sürükle, boyutlandır, saydamlığını ayarla. FPS, yarış ve emülatör düzenleri hazır."
    )
)

/**
 * İlk açılış tanıtımı: parallax geçişli 4 sayfa, atlanabilir, animasyonlu logo ve
 * PC eşleştirme adımlarını içerir.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val colors = YcTheme.colors
    val type = YcTheme.type
    val pageList = pages()
    val pagerState = rememberPagerState(pageCount = { pageList.size })
    val scope = rememberCoroutineScope()

    YcBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GamepadMark(size = 30.dp, glow = false)
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "YCONTROLER",
                    style = type.label,
                    color = colors.textSecondary,
                    letterSpacing = 3.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = t("Atla", "Skip"),
                    style = type.bodyStrong,
                    color = colors.textSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClick = onFinish)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                    .coerceIn(-1f, 1f)
                OnboardingPageContent(
                    page = page,
                    data = pageList[page],
                    parallax = offset,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Sayfa göstergesi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pageList.indices.forEach { index ->
                    val selected = pagerState.currentPage == index
                    val width = if (selected) 26.dp else 8.dp
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .height(7.dp)
                            .width(width)
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (selected) colors.accent else Color.White.copy(alpha = 0.16f)
                            )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    SecondaryButton(
                        text = t("Geri", "Back"),
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                        modifier = Modifier.weight(0.42f)
                    )
                }
                PrimaryButton(
                    text = if (pagerState.currentPage == pageList.lastIndex) {
                        t("Başla", "Get Started")
                    } else {
                        t("Devam", "Continue")
                    },
                    onClick = {
                        if (pagerState.currentPage == pageList.lastIndex) {
                            onFinish()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    icon = { YcIcon(YcIcon.ChevronRight, Modifier.size(17.dp)) },
                    shine = false
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: Int,
    data: OnboardingPage,
    parallax: Float,
    modifier: Modifier = Modifier
) {
    val colors = YcTheme.colors
    val type = YcTheme.type
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(268.dp)
                .graphicsLayer {
                    translationX = parallax * 70f
                    alpha = 1f - abs(parallax) * 0.35f
                },
            contentAlignment = Alignment.Center
        ) {
            when (page) {
                0 -> FirstIllustration()
                1 -> SecondIllustration()
                2 -> ThirdIllustration()
                else -> FourthIllustration()
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = data.eyebrow.uppercase(),
            style = type.label,
            color = colors.accent
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = t(data.title, data.title),
            style = type.display,
            color = colors.text,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = t(data.description, data.description),
            style = type.body,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

@Composable
private fun FirstIllustration() {
    val colors = YcTheme.colors
    val pulse = rememberInfiniteTransition(label = "bob")
    val bob by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "bob"
    )
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(250.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f
            drawCircle(
                Brush.radialGradient(
                    listOf(colors.accent.copy(alpha = 0.30f), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                color = colors.accent.copy(alpha = 0.18f),
                radius = radius * 0.72f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = colors.accentSecondary.copy(alpha = 0.10f),
                radius = radius * 0.90f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        GamepadMark(
            size = 184.dp,
            glow = true,
            glowStrength = 0.85f + bob * 0.15f,
            modifier = Modifier.graphicsLayer { translationY = bob * 16f - 8f }
        )
    }
}

@Composable
private fun SecondIllustration() {
    val colors = YcTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GamepadButton(
                label = "Y",
                onPress = {},
                modifier = Modifier.size(62.dp),
                faceColor = Color(0xFFFFC93C),
                glyphColor = Color(0xFF080A12),
                hapticStrength = 0.5f
            )
            GamepadButton(
                label = "X",
                onPress = {},
                modifier = Modifier.size(62.dp),
                faceColor = Color(0xFF4D9BFF),
                glyphColor = Color(0xFF080A12),
                hapticStrength = 0.5f
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GamepadButton(
                label = "B",
                onPress = {},
                modifier = Modifier.size(62.dp),
                faceColor = Color(0xFFFF5D6C),
                glyphColor = Color(0xFF080A12),
                hapticStrength = 0.5f
            )
            GamepadButton(
                label = "A",
                onPress = {},
                modifier = Modifier.size(62.dp),
                faceColor = Color(0xFF62D26F),
                glyphColor = Color(0xFF080A12),
                hapticStrength = 0.5f
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = t("Dokun ve hisset", "Touch and feel"),
            style = YcTheme.type.micro,
            color = colors.textTertiary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = t("Semboller Xbox · PlayStation · Nintendo", "Glyphs Xbox · PlayStation · Nintendo"),
            style = YcTheme.type.micro,
            color = colors.textTertiary
        )
    }
}

@Composable
private fun ThirdIllustration() {
    val colors = YcTheme.colors
    val type = YcTheme.type
    val pulse = rememberInfiniteTransition(label = "signal")
    val phase by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing)),
        label = "phase"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(120.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val base = size.minDimension / 2f
                for (i in 0..2) {
                    val progress = (phase + i * 0.33f) % 1f
                    drawCircle(
                        color = colors.accent.copy(alpha = (1f - progress) * 0.45f),
                        radius = base * (0.35f + progress * 0.6f),
                        center = center,
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                }
            }
            Box(
                Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(colors.accent.copy(alpha = 0.30f), colors.accent.copy(alpha = 0.08f)))),
                contentAlignment = Alignment.Center
            ) {
                YcIcon(YcIcon.Bluetooth, Modifier.size(26.dp), tint = colors.accent, strokeWidth = 1.8.dp)
            }
        }
        Spacer(Modifier.height(14.dp))
        listOf(
            "Telefonu 5 dk görünür yap" to "Make the phone visible for 5 min",
            "\"Ycontroler\" cihazını seç" to "Pick the \"Ycontroler\" device",
            "Kontrolcüyü aç ve oyna" to "Open the controller and play"
        ).forEachIndexed { index, pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${index + 1}", style = type.micro, color = colors.accent, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Text(t(pair.first, pair.second), style = type.bodySmall, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun FourthIllustration() {
    val colors = YcTheme.colors
    Box(
        Modifier
            .fillMaxWidth(0.94f)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black)
            .padding(6.dp)
    ) {
        GamepadLayout(
            layout = DefaultLayouts.fps,
            modifier = Modifier.fillMaxSize(),
            skin = GamepadSkin.XBOX,
            hapticStrength = 0f,
            interactive = false
        )
        Text(
            text = "FPS",
            style = YcTheme.type.micro,
            color = colors.text.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        )
    }
}
