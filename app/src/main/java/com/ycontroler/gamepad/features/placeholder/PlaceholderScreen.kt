package com.ycontroler.gamepad.features.placeholder

import androidx.compose.runtime.Composable
import com.ycontroler.gamepad.core.i18n.t
import com.ycontroler.gamepad.ui.components.EmptyState
import com.ycontroler.gamepad.ui.components.YcIcon
import com.ycontroler.gamepad.ui.components.YcScreen

/** Aşama tamamlanana kadar kullanılan geçici ekran. */
@Composable
fun PlaceholderScreen(
    title: String,
    onBack: () -> Unit
) {
    YcScreen(title = title, onBack = onBack) {
        EmptyState(
            icon = YcIcon.Sparkle,
            title = t("Hazırlanıyor", "In progress"),
            message = t(
                "Bu bölüm bir sonraki aşamada tamamlanacak.",
                "This section will be completed in the next stage."
            )
        )
    }
}
