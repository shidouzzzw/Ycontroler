package com.ycontroler.gamepad.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ycontroler.gamepad.core.design.YcTheme

/**
 * Tüm ekranlar için ortak iskelet: arka plan, durum çubuğu boşluğu,
 * animasyonlu başlık ve isteğe bağlı kaydırılabilir içerik.
 */
@Composable
fun YcScreen(
    title: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    scroll: Boolean = true,
    horizontalPadding: androidx.compose.ui.unit.Dp = YcTheme.dims.gutter,
    content: @Composable ColumnScope.() -> Unit
) {
    val dims = YcTheme.dims
    YcBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (title != null || onBack != null || trailing != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dims.gutter, vertical = dims.m),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButtonGlass(icon = YcIcon.Back, onClick = onBack, size = 42.dp)
                        Spacer(Modifier.width(dims.m))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (title != null) {
                            Text(
                                text = title,
                                style = YcTheme.type.headline,
                                color = YcTheme.colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = YcTheme.type.bodySmall,
                                color = YcTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (trailing != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) { trailing() }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = horizontalPadding)
            ) {
                content()
                if (scroll) Spacer(Modifier.size(dims.xxl))
            }
        }
    }
}
