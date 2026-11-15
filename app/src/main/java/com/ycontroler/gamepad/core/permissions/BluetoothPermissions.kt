package com.ycontroler.gamepad.core.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** Android 12+ için gereken çalışma zamanı izinleri. */
val bluetoothRuntimePermissions: List<String>
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
    } else {
        emptyList()
    }

fun Context.hasBluetoothPermissions(): Boolean =
    bluetoothRuntimePermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

/** İzin durumunu ve istek başlatıcısını taşıyan küçük denetleyici. */
@Stable
class BluetoothPermissionController internal constructor(
    val allGranted: Boolean,
    val permanentlyDenied: Boolean,
    val requestCount: Int,
    private val launch: () -> Unit
) {
    fun request() = launch()
}

/**
 * İzin durumunu izler; kullanıcı ayarlardan dönünce otomatik yenilenir.
 * İki kez reddedilirse "kalıcı red" kabul edilir ve ayarlara yönlendirilir.
 */
@Composable
fun rememberBluetoothPermissionController(): BluetoothPermissionController {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var granted by remember { mutableStateOf(context.hasBluetoothPermissions()) }
    var attempts by remember { mutableIntStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        attempts += 1
        granted = context.hasBluetoothPermissions()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = context.hasBluetoothPermissions()
    }

    val required = bluetoothRuntimePermissions
    val permanentlyDenied = !granted &&
        attempts >= 2 &&
        required.none { permission ->
            activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }

    return remember(granted, attempts, permanentlyDenied, required) {
        BluetoothPermissionController(
            allGranted = granted,
            permanentlyDenied = permanentlyDenied,
            requestCount = attempts
        ) {
            if (required.isNotEmpty()) launcher.launch(required.toTypedArray())
        }
    }
}
