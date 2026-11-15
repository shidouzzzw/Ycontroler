package com.ycontroler.gamepad.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ycontroler.gamepad.core.design.YcTheme

/** Seçilebilir cam rozet. Seçiliyken vurgu rengiyle dolar. */
@Composable
fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = YcTheme.colors
    val shape = RoundedCornerShape(100.dp)
    val bg by animateColorAsState(
        targetValue = if (selected) colors.accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.045f),
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "chipBg"
    )
    val border by animateColorAsState(
        targetValue = if (selected) colors.accent.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.12f),
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "chipBorder"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.98f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "chipScale"
    )
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .heightIn(min = 38.dp)
            .background(bg, shape)
            .border(1.dp, border, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = YcTheme.type.bodyStrong,
            color = if (selected) colors.text else colors.textSecondary
        )
    }
}

/** Yatay segment seçici; seçili öğe vurgu gradyanıyla işaretlenir. */
@Composable
fun SegmentedRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val colors = YcTheme.colors
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
                    .background(
                        brush = if (selected) {
                            Brush.horizontalGradient(colors.accentGradient)
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(role = Role.RadioButton, onClick = { onSelect(index) })
                    .semantics { this.selected = selected }
                    .padding(horizontal = 8.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = YcTheme.type.bodyStrong,
                    color = if (selected) colors.onAccent else colors.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

/** Küçük durum rozeti (HUD vb.). */
@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    filled: Boolean = false
) {
    Row(
        modifier = modifier
            .background(
                color = if (filled) color.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(100.dp)
            )
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Box(Modifier.size(12.dp), contentAlignment = Alignment.Center) { icon() }
        }
        Text(text = text, style = YcTheme.type.micro, color = color)
    }
}
