package com.ycontroler.gamepad.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/** Ortak öğe geçişlerini ekranlara taşımak için kök kapsam. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** Her navigasyon hedefinin kendi görünürlük kapsamı. */
val LocalAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * İki ekran arasında paylaşılan öğe: Home'daki logo ile eşleştirme ekranındaki logo
 * aynı anda büyüyüp küçülerek geçiş yapar.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedElementFor(key: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalAnimatedVisibilityScope.current ?: return this
    return with(sharedScope) {
        this@sharedElementFor.sharedElement(
            state = rememberSharedContentState(key),
            animatedVisibilityScope = animatedScope
        )
    }
}
