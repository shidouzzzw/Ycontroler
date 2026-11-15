package com.ycontroler.gamepad.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ycontroler.gamepad.AppViewModel
import com.ycontroler.gamepad.core.design.YcMotion
import com.ycontroler.gamepad.core.design.YcTheme
import com.ycontroler.gamepad.core.design.YcontrolerTheme
import com.ycontroler.gamepad.core.i18n.t
import com.ycontroler.gamepad.core.permissions.rememberBluetoothPermissionController
import com.ycontroler.gamepad.features.about.AboutScreen
import com.ycontroler.gamepad.features.components.ComponentPreviewScreen
import com.ycontroler.gamepad.features.controller.ControllerScreen
import com.ycontroler.gamepad.features.editor.LayoutEditorScreen
import com.ycontroler.gamepad.features.home.HomeScreen
import com.ycontroler.gamepad.features.onboarding.OnboardingScreen
import com.ycontroler.gamepad.features.pairing.PairingScreen
import com.ycontroler.gamepad.features.permissions.PermissionsScreen
import com.ycontroler.gamepad.features.placeholder.PlaceholderScreen
import com.ycontroler.gamepad.features.settings.SettingsScreen
import com.ycontroler.gamepad.features.splash.SplashScreen
import com.ycontroler.gamepad.features.unsupported.UnsupportedScreen
import com.ycontroler.gamepad.hid.HidSupport
import com.ycontroler.gamepad.ui.components.LocalAnimatedVisibilityScope
import com.ycontroler.gamepad.ui.components.LocalSharedTransitionScope

/** Uygulama rotaları. */
object Routes {
    const val Splash = "splash"
    const val Home = "home"
    const val Permissions = "permissions"
    const val Pairing = "pairing"
    const val Controller = "controller"
    const val Settings = "settings"
    const val Editor = "editor"
    const val About = "about"
    const val Components = "components"
}

/**
 * Uygulama kökü: ayarlara göre tema ve dil, HID destek kontrolü ve navigasyon.
 */
@Composable
fun YcApp(vm: AppViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    YcontrolerTheme(
        amoled = settings.amoled,
        accent = settings.accent,
        customAccent = settings.customAccentColor,
        animationLevel = settings.animationLevel,
        language = settings.language
    ) {
        AppGate(vm)
    }
}

@Composable
private fun AppGate(vm: AppViewModel) {
    val support by vm.support.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var splashFinished by rememberSaveable { mutableStateOf(false) }

    when {
        !splashFinished -> SplashScreen(
            ready = support != HidSupport.CHECKING,
            onFinished = { splashFinished = true }
        )
        support == HidSupport.UNSUPPORTED -> UnsupportedScreen(
            onRetry = { vm.recheckSupport() }
        )
        !settings.onboardingDone -> OnboardingScreen(
            onFinish = { vm.updateSettings { it.copy(onboardingDone = true) } }
        )
        else -> MainNav(vm)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MainNav(
    vm: AppViewModel,
    navController: NavHostController = rememberNavController()
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val connection by vm.connection.collectAsStateWithLifecycle()
    val hosts by vm.hosts.collectAsStateWithLifecycle()
    val latency by vm.latencyMs.collectAsStateWithLifecycle()
    val reportsPerSecond by vm.reportsPerSecond.collectAsStateWithLifecycle()
    val battery by vm.batteryPercent.collectAsStateWithLifecycle()
    val permissions = rememberBluetoothPermissionController()

    val scale = YcTheme.animScale
    val enterMs = YcMotion.dur(380, scale)
    val exitMs = YcMotion.dur(220, scale)
    val ease = CubicBezierEasing(0.16f, 0.9f, 0.2f, 1f)

    SharedTransitionLayout {
    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
    NavHost(
        navController = navController,
        startDestination = Routes.Home,
        modifier = Modifier,
        enterTransition = {
            fadeIn(tween(enterMs, easing = ease)) +
                scaleIn(initialScale = 0.93f, animationSpec = tween(enterMs, easing = ease)) +
                slideInHorizontally(
                    initialOffsetX = { (it * 0.06f).toInt() },
                    animationSpec = tween(enterMs, easing = ease)
                )
        },
        exitTransition = {
            fadeOut(tween(exitMs)) +
                scaleOut(targetScale = 1.04f, animationSpec = tween(exitMs)) +
                slideOutHorizontally(
                    targetOffsetX = { -(it * 0.03f).toInt() },
                    animationSpec = tween(exitMs)
                )
        },
        popEnterTransition = {
            fadeIn(tween(enterMs, easing = ease)) +
                scaleIn(initialScale = 1.04f, animationSpec = tween(enterMs, easing = ease))
        },
        popExitTransition = {
            fadeOut(tween(exitMs)) +
                scaleOut(targetScale = 0.95f, animationSpec = tween(exitMs)) +
                slideOutHorizontally(
                    targetOffsetX = { (it * 0.06f).toInt() },
                    animationSpec = tween(exitMs)
                )
        }
    ) {
        composable(Routes.Home) {
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
            HomeScreen(
                settings = settings,
                connection = connection,
                hosts = hosts,
                latencyMs = latency,
                reportsPerSecond = reportsPerSecond,
                batteryPercent = battery,
                onConnect = {
                    navController.navigate(
                        if (permissions.allGranted) Routes.Pairing else Routes.Permissions
                    )
                },
                onOpenPairing = { navController.navigate(Routes.Pairing) },
                onOpenController = { navController.navigate(Routes.Controller) },
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onOpenLayouts = { navController.navigate(Routes.Editor) },
                onOpenHelp = { navController.navigate(Routes.About) },
                onOpenComponents = { navController.navigate(Routes.Components) },
                onConnectToHost = { vm.connectToHostAfterStart(it) }
            )
            }
        }

        composable(Routes.Permissions) {
            PermissionsScreen(
                permissions = permissions,
                onBack = { navController.popBackStack() },
                onGranted = {
                    vm.recheckSupport()
                    vm.startHid()
                    navController.navigate(Routes.Pairing) {
                        popUpTo(Routes.Permissions) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Pairing) {
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
            PairingScreen(
                vm = vm,
                permissions = permissions,
                onBack = { navController.popBackStack() },
                onOpenPermissions = { navController.navigate(Routes.Permissions) },
                onOpenController = { navController.navigate(Routes.Controller) }
            )
            }
        }

        composable(Routes.Controller) {
            ControllerScreen(
                vm = vm,
                onExit = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onOpenLayouts = { navController.navigate(Routes.Editor) }
            )
        }

        composable(Routes.Settings) {
            SettingsScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onOpenLayouts = { navController.navigate(Routes.Editor) }
            )
        }

        composable(Routes.Editor) {
            LayoutEditorScreen(
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.About) {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onOpenPairing = { navController.navigate(Routes.Pairing) },
                onOpenSettings = { navController.navigate(Routes.Settings) }
            )
        }

        composable(Routes.Components) {
            ComponentPreviewScreen(onBack = { navController.popBackStack() })
        }
    }
    }
    }
}
