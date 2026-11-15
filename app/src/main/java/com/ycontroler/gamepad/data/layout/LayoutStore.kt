package com.ycontroler.gamepad.data.layout

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Düzen profillerini JSON dosyasında saklar. Dosya küçük olduğu için tüm liste
 * bellekte tutulur; yazma işlemleri IO dispatcher'ında ve mutex ile serileştirilir.
 */
@Singleton
class LayoutStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }
    private val serializer = ListSerializer(LayoutProfile.serializer())
    private val file = File(context.filesDir, "layouts.json")
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _layouts = MutableStateFlow(DefaultLayouts.all)
    val layouts: StateFlow<List<LayoutProfile>> = _layouts.asStateFlow()

    init {
        scope.launch { readFromDisk() }
    }

    private suspend fun readFromDisk() = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                if (file.exists()) {
                    val parsed = json.decodeFromString(serializer, file.readText())
                    if (parsed.isNotEmpty()) _layouts.value = parsed
                }
            }
        }
    }

    /** Kilit tutulurken çağrılır; IO zaten IO dispatcher'ında çalışır. */
    private fun writeLocked(list: List<LayoutProfile>) {
        runCatching { file.writeText(json.encodeToString(serializer, list)) }
    }

    suspend fun upsert(profile: LayoutProfile) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _layouts.value.toMutableList()
            val index = current.indexOfFirst { it.id == profile.id }
            if (index >= 0) current[index] = profile else current.add(profile)
            _layouts.value = current
            writeLocked(current)
        }
    }

    /** Silinen düzen aktifse çağıran taraf yeni aktif kimliği belirlemelidir. */
    suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _layouts.value
            if (current.size <= 1) return@withLock false
            val filtered = current.filterNot { it.id == id }
            if (filtered.size == current.size) return@withLock false
            _layouts.value = filtered
            writeLocked(filtered)
            true
        }
    }

    suspend fun duplicate(id: String, newId: String, newName: String): LayoutProfile? {
        val source = _layouts.value.firstOrNull { it.id == id } ?: return null
        val copy = source.copy(
            id = newId,
            name = newName,
            builtIn = false,
            updatedAt = System.currentTimeMillis(),
            controls = source.controls.map { it.copy(id = it.id + "-" + newId.take(6)) }
        )
        upsert(copy)
        return copy
    }

    suspend fun replaceAll(list: List<LayoutProfile>) {
        if (list.isEmpty()) return
        withContext(Dispatchers.IO) {
            mutex.withLock {
                _layouts.value = list
                writeLocked(list)
            }
        }
    }
}
