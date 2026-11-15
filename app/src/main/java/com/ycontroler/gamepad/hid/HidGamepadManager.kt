package com.ycontroler.gamepad.hid

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Telefonun HID Device profilini destekleyip desteklemediği. */
enum class HidSupport { CHECKING, SUPPORTED, UNSUPPORTED }

/** Bağlantı hatası nedenleri — arayüzde anlaşılır mesaja çevrilir. */
enum class HidFailure {
    PERMISSION, BLUETOOTH_OFF, REGISTER_FAILED, CONNECT_FAILED, UNSUPPORTED, TIMEOUT
}

sealed interface HidConnectionState {
    data object Idle : HidConnectionState
    data object Preparing : HidConnectionState

    /** Uygulama HID aygıtı olarak kayıtlı; bilgisayarın bağlanması bekleniyor. */
    data object Waiting : HidConnectionState

    data class Connected(val deviceName: String, val address: String) : HidConnectionState
    data class Error(val failure: HidFailure, val detail: String? = null) : HidConnectionState
}

/** Eşleşmiş cihaz listesi için sade model. */
data class HidHostDevice(
    val name: String,
    val address: String,
    val isComputer: Boolean
)

/**
 * Bluetooth HID Device katmanı.
 *
 * - HID tanımlayıcısı ile kendini gamepad olarak kaydeder.
 * - UI'dan gelen girişleri [GamepadInputState] üzerinden toplar.
 * - Raporları ayrı bir coroutine'de, CONFLATED kanal + zaman sınırı ile gönderir;
 *   böylece ana iş parçacığı hiçbir zaman bloke olmaz ve maksimum ~250 Hz aşılmaz.
 * - Bağlantı kopunca tüm tuşları serbest bırakır ve otomatik yeniden bağlanmayı dener.
 */
@Singleton
class HidGamepadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val adapter: BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sendSignal = Channel<Unit>(Channel.CONFLATED)

    val input = GamepadInputState()

    private val _support = MutableStateFlow(HidSupport.CHECKING)
    val support: StateFlow<HidSupport> = _support.asStateFlow()

    private val _state = MutableStateFlow<HidConnectionState>(HidConnectionState.Idle)
    val state: StateFlow<HidConnectionState> = _state.asStateFlow()

    private val _hosts = MutableStateFlow<List<HidHostDevice>>(emptyList())
    val hosts: StateFlow<List<HidHostDevice>> = _hosts.asStateFlow()

    private val _latencyMs = MutableStateFlow(0f)
    val latencyMs: StateFlow<Float> = _latencyMs.asStateFlow()

    private val _reportsPerSecond = MutableStateFlow(0)
    val reportsPerSecond: StateFlow<Int> = _reportsPerSecond.asStateFlow()

    private val _batteryPercent = MutableStateFlow(0)
    val batteryPercent: StateFlow<Int> = _batteryPercent.asStateFlow()

    private val _bluetoothEnabled = MutableStateFlow(adapter?.isEnabled == true)
    val bluetoothEnabled: StateFlow<Boolean> = _bluetoothEnabled.asStateFlow()

    @Volatile
    private var hidDevice: BluetoothHidDevice? = null

    @Volatile
    private var registered = false

    @Volatile
    private var connectedDevice: BluetoothDevice? = null

    @Volatile
    private var manualDisconnect = false

    private var registrationWaiter: CompletableDeferred<Boolean>? = null
    private var senderJob: Job? = null
    private var reconnectJob: Job? = null
    private var lastAddress: String? = null
    private var autoReconnect = true
    private var reportIntervalNs = 1_000_000_000L / 250L
    private var lastSentNs = 0L
    private var reportCounter = 0
    private var rateWindowStart = 0L
    private val receiverRegistered = AtomicBoolean(false)

    private val adapterReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    val enabled = newState == BluetoothAdapter.STATE_ON
                    _bluetoothEnabled.value = enabled
                    if (!enabled) {
                        input.releaseAll()
                        stopSender()
                        _state.value = HidConnectionState.Error(HidFailure.BLUETOOTH_OFF)
                    } else {
                        refreshHosts()
                        _state.value = if (registered) HidConnectionState.Waiting else HidConnectionState.Idle
                    }
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    if (level >= 0 && scale > 0) {
                        _batteryPercent.value = (level * 100 / scale)
                    }
                }
            }
        }
    }

    init {
        registerReceivers()
        _batteryPercent.value = readBatteryPercent()
    }

    private fun registerReceivers() {
        if (!receiverRegistered.compareAndSet(false, true)) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            adapterReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun readBatteryPercent(): Int {
        val bm = context.getSystemService(BatteryManager::class.java) ?: return 0
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return level.coerceIn(0, 100)
    }

    fun hasConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    // ---------------------------------------------------------------- Destek kontrolü

    /** Açılışta cihazın HID Device profilini destekleyip desteklemediğini belirler. */
    suspend fun checkSupport() {
        _support.value = HidSupport.CHECKING
        val localAdapter = adapter
        if (localAdapter == null) {
            _support.value = HidSupport.UNSUPPORTED
            return
        }
        // Profil proxy'si alınamıyorsa cihaz HID Device profilini sunmuyordur.
        val proxy = withTimeoutOrNull(3000) { obtainProxy() }
        _support.value = when {
            proxy != null -> HidSupport.SUPPORTED
            // İzin verilmediği için sonuç alınamadıysa iyimser davran; izin ekranından sonra doğrulanır.
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasConnectPermission() -> HidSupport.SUPPORTED
            else -> HidSupport.UNSUPPORTED
        }
    }

    private suspend fun obtainProxy(): BluetoothHidDevice? {
        hidDevice?.let { return it }
        val localAdapter = adapter ?: return null
        return suspendCancellableCoroutine { continuation ->
            val listener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.HID_DEVICE && proxy is BluetoothHidDevice) {
                        hidDevice = proxy
                        if (continuation.isActive) continuation.resume(proxy)
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    if (profile == BluetoothProfile.HID_DEVICE) hidDevice = null
                }
            }
            val requested = runCatching {
                localAdapter.getProfileProxy(context, listener, BluetoothProfile.HID_DEVICE)
            }.getOrDefault(false)
            if (!requested && continuation.isActive) continuation.resume(null)
        }
    }

    // ---------------------------------------------------------------- Kayıt / bağlantı

    /**
     * Uygulamayı HID gamepad olarak kaydeder. Bilgisayarın bağlanması için
     * telefonun görünür moda alınması gerekir (arayüz yönlendirir).
     */
    suspend fun start(deviceName: String, autoReconnectEnabled: Boolean = true) {
        autoReconnect = autoReconnectEnabled
        manualDisconnect = false
        if (!hasConnectPermission()) {
            _state.value = HidConnectionState.Error(HidFailure.PERMISSION)
            return
        }
        if (!isBluetoothEnabled()) {
            _state.value = HidConnectionState.Error(HidFailure.BLUETOOTH_OFF)
            return
        }
        if (registered && connectedDevice == null) {
            _state.value = HidConnectionState.Waiting
            refreshHosts()
            return
        }
        _state.value = HidConnectionState.Preparing
        val proxy = withTimeoutOrNull(5000) { obtainProxy() }
        if (proxy == null) {
            _state.value = HidConnectionState.Error(HidFailure.UNSUPPORTED)
            return
        }
        val sdp = BluetoothHidDeviceAppSdpSettings(
            deviceName.take(28).ifBlank { "Ycontroler" },
            "Ycontroler virtual gamepad",
            "Ycontroler",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            GamepadDescriptor.BYTES
        )
        val waiter = CompletableDeferred<Boolean>()
        registrationWaiter = waiter
        val launched = runCatching {
            proxy.registerApp(
                sdp,
                null,
                null,
                ContextCompat.getMainExecutor(context),
                callback
            )
        }.getOrElse {
            _state.value = HidConnectionState.Error(HidFailure.REGISTER_FAILED, it.message)
            false
        }
        if (!launched) return
        val ok = withTimeoutOrNull(6000) { waiter.await() } == true
        if (ok) {
            registered = true
            _state.value = HidConnectionState.Waiting
            refreshHosts()
        } else {
            _state.value = HidConnectionState.Error(HidFailure.REGISTER_FAILED)
        }
    }

    /** Eşleşmiş bir bilgisayara bağlantı başlatır. */
    suspend fun connectTo(address: String) {
        if (!hasConnectPermission()) {
            _state.value = HidConnectionState.Error(HidFailure.PERMISSION)
            return
        }
        if (!isBluetoothEnabled()) {
            _state.value = HidConnectionState.Error(HidFailure.BLUETOOTH_OFF)
            return
        }
        val localAdapter = adapter ?: return
        val device = runCatching { localAdapter.getRemoteDevice(address) }.getOrNull() ?: return
        val proxy = withTimeoutOrNull(5000) { obtainProxy() }
        if (proxy == null) {
            _state.value = HidConnectionState.Error(HidFailure.UNSUPPORTED)
            return
        }
        manualDisconnect = false
        lastAddress = address
        _state.value = HidConnectionState.Preparing
        val connected = runCatching { proxy.connect(device) }.getOrDefault(false)
        if (!connected) {
            _state.value = HidConnectionState.Error(HidFailure.CONNECT_FAILED)
        }
    }

    /** Bağlantıyı kullanıcı isteğiyle keser (otomatik yeniden bağlanma tetiklenmez). */
    fun disconnect() {
        manualDisconnect = true
        reconnectJob?.cancel()
        val device = connectedDevice
        if (device != null) {
            runCatching { hidDevice?.disconnect(device) }
        }
        input.releaseAll()
        stopSender()
        _state.value = if (registered) HidConnectionState.Waiting else HidConnectionState.Idle
    }

    /** Uygulamayı HID kaydından çıkarır. */
    fun shutdown() {
        manualDisconnect = true
        input.releaseAll()
        stopSender()
        reconnectJob?.cancel()
        runCatching { hidDevice?.unregisterApp() }
        registered = false
        connectedDevice = null
        _state.value = HidConnectionState.Idle
    }

    fun setAutoReconnect(enabled: Boolean) {
        autoReconnect = enabled
    }

    fun setReportRate(hz: Int) {
        val clamped = hz.coerceIn(30, 250)
        reportIntervalNs = 1_000_000_000L / clamped
    }

    /** Eşleşmiş cihazları yeniler (bilgisayarlar listede önce gelir). */
    fun refreshHosts() {
        if (!hasConnectPermission()) {
            _hosts.value = emptyList()
            return
        }
        val devices = runCatching { adapter?.bondedDevices?.toList() }.getOrNull().orEmpty()
        _hosts.value = devices.map { device ->
            val name = runCatching { device.name }.getOrNull()
                ?: runCatching { device.address }.getOrNull()
                ?: "PC"
            val computer = runCatching {
                device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.COMPUTER
            }.getOrDefault(false)
            HidHostDevice(name = name, address = device.address, isComputer = computer)
        }.sortedWith(compareByDescending<HidHostDevice> { it.isComputer }.thenBy { it.name.lowercase() })
    }

    // ---------------------------------------------------------------- Giriş / rapor gönderimi

    fun requestSend() {
        sendSignal.trySend(Unit)
    }

    /** Bağlantı koptuğunda çağrılır: takılı tuş kalmadığından emin olur. */
    fun releaseAll() {
        input.releaseAll()
        requestSend()
    }

    private fun startSender() {
        if (senderJob?.isActive == true) return
        lastSentNs = 0L
        rateWindowStart = System.nanoTime()
        reportCounter = 0
        senderJob = scope.launch {
            val report = GamepadReport()
            val bytes = ByteArray(GamepadDescriptor.REPORT_SIZE_BYTES)
            for (signal in sendSignal) {
                val sinceLast = System.nanoTime() - lastSentNs
                val waitNs = reportIntervalNs - sinceLast
                if (waitNs > 1_000_000L) {
                    delay(waitNs / 1_000_000L)
                }
                // Arada biriken değişiklikler tek raporda birleşir.
                if (!input.consumeSnapshot(report)) continue
                val device = connectedDevice ?: continue
                val proxy = hidDevice ?: continue
                report.writeTo(bytes)
                val started = System.nanoTime()
                val ok = runCatching {
                    proxy.sendReport(device, GamepadDescriptor.REPORT_ID, bytes)
                }.getOrDefault(false)
                val elapsedMs = (System.nanoTime() - started) / 1_000_000f
                if (ok) {
                    lastSentNs = System.nanoTime()
                    _latencyMs.value = if (_latencyMs.value <= 0f) {
                        elapsedMs
                    } else {
                        _latencyMs.value * 0.85f + elapsedMs * 0.15f
                    }
                    reportCounter++
                    val window = System.nanoTime() - rateWindowStart
                    if (window >= 1_000_000_000L) {
                        _reportsPerSecond.value = (reportCounter * 1_000_000_000L / window).toInt()
                        reportCounter = 0
                        rateWindowStart = System.nanoTime()
                    }
                }
            }
        }
    }

    private fun stopSender() {
        senderJob?.cancel()
        senderJob = null
        _reportsPerSecond.value = 0
    }

    private fun onConnected(device: BluetoothDevice) {
        connectedDevice = device
        manualDisconnect = false
        lastAddress = runCatching { device.address }.getOrNull()
        val name = runCatching { device.name }.getOrNull() ?: lastAddress ?: "PC"
        _state.value = HidConnectionState.Connected(name, lastAddress ?: "")
        startSender()
        input.releaseAll()
        requestSend()
        refreshHosts()
    }

    private fun onDisconnected() {
        connectedDevice = null
        stopSender()
        input.releaseAll()
        _state.value = if (registered) HidConnectionState.Waiting else HidConnectionState.Idle
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (!autoReconnect || manualDisconnect) return
        val address = lastAddress ?: return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var attempt = 0
            while (isActive && attempt < 4 && connectedDevice == null && registered && !manualDisconnect) {
                delay(1200L * (attempt + 1))
                if (manualDisconnect) break
                val device = runCatching { adapter?.getRemoteDevice(address) }.getOrNull() ?: break
                runCatching { hidDevice?.connect(device) }
                attempt++
            }
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            this@HidGamepadManager.registered = registered
            registrationWaiter?.let {
                if (!it.isCompleted) it.complete(registered)
            }
            if (!registered && connectedDevice == null) {
                _state.value = HidConnectionState.Idle
            } else if (registered && connectedDevice == null && _state.value !is HidConnectionState.Connected) {
                _state.value = HidConnectionState.Waiting
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> onConnected(device)
                BluetoothProfile.STATE_DISCONNECTED -> onDisconnected()
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            runCatching {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
            }
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            runCatching {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
            }
        }

        override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
            // Protokol değişimi kabul edilir; rapor formatı sabittir.
        }

        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            // Host'tan gelen interrupt verisi kullanılmıyor.
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            input.releaseAll()
            runCatching { hidDevice?.disconnect(device) }
        }
    }
}
