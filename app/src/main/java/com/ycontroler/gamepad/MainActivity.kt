package com.ycontroler.gamepad

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ycontroler.gamepad.core.haptics.AndroidHaptics
import com.ycontroler.gamepad.core.haptics.LocalHaptics
import com.ycontroler.gamepad.navigation.YcApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            val haptics = remember { AndroidHaptics(applicationContext) }
            CompositionLocalProvider(LocalHaptics provides haptics) {
                YcApp()
            }
        }
    }
}
