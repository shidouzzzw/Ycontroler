package com.ycontroler.gamepad.features.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ycontroler.gamepad.AppViewModel
import com.ycontroler.gamepad.core.design.YcTheme
import com.ycontroler.gamepad.core.i18n.t
import com.ycontroler.gamepad.data.layout.ControlSpec
import com.ycontroler.gamepad.data.layout.ControlType
import com.ycontroler.gamepad.data.layout.LayoutProfile
import com.ycontroler.gamepad.ui.components.ChoiceChip
import com.ycontroler.gamepad.ui.components.GlassCard
import com.ycontroler.gamepad.ui.components.PrimaryButton
import com.ycontroler.gamepad.ui.components.SecondaryButton
import com.ycontroler.gamepad.ui.components.YcConfirmDialog
import com.ycontroler.gamepad.ui.components.YcIcon
import com.ycontroler.gamepad.ui.components.YcScreen
import com.ycontroler.gamepad.ui.components.YcSlider
import com.ycontroler.gamepad.ui.components.YcTextField
import com.ycontroler.gamepad.ui.components.gamepad.GamepadLayout
import com.ycontroler.gamepad.ui.components.gamepad.PadButton
import kotlin.math.min

/**
 * Düzen editörü: kontrol ekle/kaldır, sürükle, boyutlandır, saydamlık ayarla,
 * ızgaraya yapıştır ve birden fazla profili yönet.
 */
@Composable
fun LayoutEditorScreen(
    vm: AppViewModel,
    onBack: () -> Unit
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val layouts by vm.layouts.collectAsStateWithLifecycle()
    val colors = YcTheme.colors
    val type = YcTheme.type
    val configuration = LocalConfiguration.current

    val activeId = settings.activeLayoutId
    val activeLayout = layouts.firstOrNull { it.id == activeId } ?: layouts.firstOrNull()

    var draft by remember { mutableStateOf<LayoutProfile?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var snapToGrid by remember { mutableStateOf(true) }
    var zoomInfo by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    LaunchedEffect(activeLayout?.id) {
        draft = activeLayout
        selectedId = null
    }

    val current = draft

    fun updateControl(id: String, transform: (ControlSpec) -> ControlSpec) {
        draft = draft?.let { layout ->
            layout.copy(controls = layout.controls.map { if (it.id == id) transform(it) else it })
        }
    }

    fun addControl(type: ControlType) {
        val layout = draft ?: return
        val seed = System.currentTimeMillis().toString().takeLast(5)
        val spec = when (type) {
            ControlType.STICK_LEFT -> ControlSpec("stick-l-$seed", type, 0.18f, 0.72f, 0.30f, buttonIndex = PadButton.L3, label = "L3")
            ControlType.STICK_RIGHT -> ControlSpec("stick-r-$seed", type, 0.82f, 0.72f, 0.30f, buttonIndex = PadButton.R3, label = "R3")
            ControlType.DPAD -> ControlSpec("dpad-$seed", type, 0.18f, 0.40f, 0.28f, label = "D-PAD")
            ControlType.TRIGGER_LEFT -> ControlSpec("lt-$seed", type, 0.32f, 0.14f, 0.20f, aspect = 0.62f, label = "LT")
            ControlType.TRIGGER_RIGHT -> ControlSpec("rt-$seed", type, 0.68f, 0.14f, 0.20f, aspect = 0.62f, label = "RT")
            ControlType.FACE_CLUSTER -> ControlSpec("face-$seed", type, 0.82f, 0.40f, 0.32f, label = "ABXY")
            ControlType.BUTTON -> ControlSpec(
                id = "btn-$seed",
                type = type,
                x = 0.5f,
                y = 0.5f,
                size = 0.12f,
                aspect = 1f,
                buttonIndex = PadButton.A,
                label = "A"
            )
        }
        draft = layout.copy(controls = layout.controls + spec)
        selectedId = spec.id
    }

    fun findControl(position: Offset, size: IntSize): ControlSpec? {
        val layout = draft ?: return null
        val unit = min(size.width, size.height).toFloat()
        return layout.controls.lastOrNull { spec ->
            val cx = (if (settings.leftHanded) 1f - spec.x else spec.x) * size.width
            val cy = spec.y * size.height
            val w = unit * spec.size * spec.aspect
            val h = unit * spec.size
            kotlin.math.abs(position.x - cx) <= w / 2f && kotlin.math.abs(position.y - cy) <= h / 2f
        }
    }

    YcScreen(
        title = t("Düzenler", "Layouts"),
        subtitle = t("Sürükle, boyutlandır, kaydet", "Drag, resize, save"),
        onBack = onBack
    ) {
        // Düzen seçici
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            layouts.forEach { layout ->
                ChoiceChip(
                    text = layout.name,
                    selected = layout.id == activeId,
                    onClick = {
                        draft = layout
                        selectedId = null
                        vm.setActiveLayout(layout.id)
                    }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (current == null) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = t("Düzen yükleniyor...", "Loading layout..."),
                    style = type.body,
                    color = colors.textSecondary
                )
            }
            return@YcScreen
        }

        // Önizleme
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
                .pointerInput(current, snapToGrid, settings.leftHanded) {
                    detectDragGestures(
                        onDragStart = { position ->
                            findControl(position, size)?.let { selectedId = it.id }
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            val id = selectedId ?: return@detectDragGestures
                            val dx = delta.x / size.width
                            val dy = delta.y / size.height
                            updateControl(id) { spec ->
                                var nx = (spec.x + dx).coerceIn(0.06f, 0.94f)
                                var ny = (spec.y + dy).coerceIn(0.06f, 0.94f)
                                if (snapToGrid) {
                                    val grid = 1f / 24f
                                    nx = (nx / grid).toInt() * grid + grid / 2f
                                    ny = (ny / grid).toInt() * grid + grid / 2f
                                    nx = nx.coerceIn(0.06f, 0.94f)
                                    ny = ny.coerceIn(0.06f, 0.94f)
                                }
                                spec.copy(x = nx, y = ny)
                            }
                        }
                    )
                }
        ) {
            if (snapToGrid) {
                GridOverlay()
            }
            GamepadLayout(
                layout = current,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                skin = settings.skin,
                leftConfig = settings.leftStick,
                rightConfig = settings.rightStick,
                hapticStrength = 0f,
                interactive = false,
                leftHanded = settings.leftHanded,
                selectedId = selectedId,
                showSelection = !zoomInfo
            )
            Text(
                text = if (zoomInfo) t("Önizleme", "Preview") else t("Düzenleme", "Editing"),
                style = type.micro,
                color = Color.White.copy(alpha = 0.35f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChoiceChip(
                text = t("Izgara", "Grid"),
                selected = snapToGrid,
                onClick = { snapToGrid = !snapToGrid }
            )
            ChoiceChip(
                text = t("Test Et", "Test"),
                selected = zoomInfo,
                onClick = { zoomInfo = !zoomInfo }
            )
            Spacer(Modifier.weight(1f))
            SecondaryButton(
                text = t("Sığdır", "Fit"),
                onClick = {
                    draft = current.copy(
                        controls = current.controls.map {
                            it.copy(x = it.x.coerceIn(0.06f, 0.94f), y = it.y.coerceIn(0.06f, 0.94f))
                        }
                    )
                },
                minHeight = 40.dp
            )
        }

        Spacer(Modifier.height(16.dp))

        // Seçili kontrol paneli
        val selected = current.controls.firstOrNull { it.id == selectedId }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selected?.let { describeControl(it) } ?: t("Kontrol seç", "Select a control"),
                    style = type.title,
                    color = colors.text,
                    modifier = Modifier.weight(1f)
                )
                if (selected != null) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(colors.danger.copy(alpha = 0.14f))
                            .clickable {
                                draft = current.copy(controls = current.controls.filterNot { it.id == selected.id })
                                selectedId = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        YcIcon(YcIcon.Trash, Modifier.size(16.dp), tint = colors.danger)
                    }
                }
            }
            if (selected != null) {
                Spacer(Modifier.height(8.dp))
                SliderRow(
                    label = t("Boyut", "Size"),
                    value = selected.size,
                    range = 0.08f..0.55f,
                    format = { "${(it * 100).toInt()}%" }
                ) { value -> updateControl(selected.id) { it.copy(size = value) } }
                SliderRow(
                    label = t("Saydamlık", "Opacity"),
                    value = selected.opacity,
                    range = 0.25f..1f,
                    format = { "${(it * 100).toInt()}%" }
                ) { value -> updateControl(selected.id) { it.copy(opacity = value) } }
                if (selected.type == ControlType.TRIGGER_LEFT || selected.type == ControlType.TRIGGER_RIGHT) {
                    Text(
                        text = t(
                            "Tetikler dikey oranla çizilir; tutamağı sürükleyerek derinlik hissini ayarlayabilirsin.",
                            "Triggers are drawn with a vertical ratio; drag to change their footprint."
                        ),
                        style = type.micro,
                        color = colors.textTertiary
                    )
                }
            } else {
                Text(
                    text = t(
                        "Önizlemede bir kontrole dokun ve sürükle. Boyut ile saydamlığı buradan ayarla.",
                        "Tap and drag a control in the preview. Adjust size and opacity here."
                    ),
                    style = type.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(t("KONTROL EKLE", "ADD CONTROL"), style = type.label, color = colors.textTertiary)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChoiceChip(text = t("Düğme", "Button"), selected = false, onClick = { addControl(ControlType.BUTTON) })
            ChoiceChip(text = t("Sol Çubuk", "Left Stick"), selected = false, onClick = { addControl(ControlType.STICK_LEFT) })
            ChoiceChip(text = t("Sağ Çubuk", "Right Stick"), selected = false, onClick = { addControl(ControlType.STICK_RIGHT) })
            ChoiceChip(text = t("D-Pad", "D-Pad"), selected = false, onClick = { addControl(ControlType.DPAD) })
            ChoiceChip(text = "LT", selected = false, onClick = { addControl(ControlType.TRIGGER_LEFT) })
            ChoiceChip(text = "RT", selected = false, onClick = { addControl(ControlType.TRIGGER_RIGHT) })
            ChoiceChip(text = "ABXY", selected = false, onClick = { addControl(ControlType.FACE_CLUSTER) })
        }

        Spacer(Modifier.height(16.dp))

        PrimaryButton(
            text = t("Düzeni Kaydet", "Save Layout"),
            onClick = {
                draft?.let { vm.saveLayout(it) }
            },
            modifier = Modifier.fillMaxWidth(),
            icon = { YcIcon(YcIcon.Saved, Modifier.size(18.dp)) },
            shine = false
        )

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(
                text = t("Yeni Kopya", "Duplicate"),
                onClick = { draft?.let { vm.duplicateLayout(it.id) } },
                modifier = Modifier.weight(1f),
                icon = { YcIcon(YcIcon.Plus, Modifier.size(16.dp)) }
            )
            SecondaryButton(
                text = t("Yeniden Adlandır", "Rename"),
                onClick = {
                    newName = current.name
                    showNameDialog = true
                },
                modifier = Modifier.weight(1f),
                icon = { YcIcon(YcIcon.Edit, Modifier.size(16.dp)) }
            )
            SecondaryButton(
                text = t("Sil", "Delete"),
                onClick = { showDeleteDialog = true },
                modifier = Modifier.weight(1f),
                enabled = layouts.size > 1,
                icon = { YcIcon(YcIcon.Trash, Modifier.size(16.dp)) }
            )
        }

        Spacer(Modifier.height(14.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                YcIcon(YcIcon.Screen, Modifier.size(17.dp), tint = colors.accent)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = t(
                        "Ekran: ${configuration.screenWidthDp} × ${configuration.screenHeightDp} dp · Düzen oranlı ölçeklenir",
                        "Screen: ${configuration.screenWidthDp} × ${configuration.screenHeightDp} dp · Layout scales proportionally"
                    ),
                    style = type.micro,
                    color = colors.textTertiary
                )
            }
        }

        Spacer(Modifier.height(26.dp))
    }

    if (showNameDialog) {
        var nameDraft by remember { mutableStateOf(newName) }
        androidx.compose.ui.window.Dialog(onDismissRequest = { showNameDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.surface)
                    .padding(22.dp)
            ) {
                Text(t("Düzeni yeniden adlandır", "Rename layout"), style = type.headline, color = colors.text)
                Spacer(Modifier.height(12.dp))
                YcTextField(
                    value = nameDraft,
                    onValueChange = { if (it.length <= 22) nameDraft = it },
                    placeholder = t("Düzen adı", "Layout name")
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        text = t("Vazgeç", "Cancel"),
                        onClick = { showNameDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = t("Kaydet", "Save"),
                        onClick = {
                            val trimmed = nameDraft.trim()
                            if (trimmed.isNotEmpty()) {
                                draft = draft?.copy(name = trimmed)
                                draft?.let { vm.saveLayout(it) }
                            }
                            showNameDialog = false
                        },
                        modifier = Modifier.weight(1f),
                        shine = false,
                        minHeight = 48.dp
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        YcConfirmDialog(
            title = t("Düzeni sil?", "Delete layout?"),
            message = t(
                "\"${current?.name ?: ""}\" kalıcı olarak silinecek. Bu işlem geri alınamaz.",
                "\"${current?.name ?: ""}\" will be deleted permanently. This cannot be undone."
            ),
            confirmText = t("Sil", "Delete"),
            dismissText = t("Vazgeç", "Cancel"),
            onConfirm = {
                current?.let { vm.deleteLayout(it.id) }
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
            destructive = true
        )
    }
}

@Composable
private fun GridOverlay() {
    val colors = YcTheme.colors
    Canvas(Modifier.fillMaxSize()) {
        val columns = 24
        val rows = 14
        for (i in 1 until columns) {
            val x = size.width * i / columns
            drawLine(
                color = Color.White.copy(alpha = 0.045f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
        }
        for (i in 1 until rows) {
            val y = size.height * i / rows
            drawLine(
                color = Color.White.copy(alpha = 0.045f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }
        drawLine(
            color = colors.accent.copy(alpha = 0.20f),
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = 1f
        )
        drawLine(
            color = colors.accent.copy(alpha = 0.20f),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1f
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = YcTheme.type.bodySmall,
            color = YcTheme.colors.textSecondary,
            modifier = Modifier.width(78.dp)
        )
        Box(Modifier.weight(1f)) {
            YcSlider(value = value, onValueChange = onChange, range = range)
        }
        Text(
            text = format(value),
            style = YcTheme.type.number,
            color = YcTheme.colors.accent,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun describeControl(spec: ControlSpec): String = when (spec.type) {
    ControlType.BUTTON -> t("Tuş · ${spec.label}", "Button · ${spec.label}")
    ControlType.STICK_LEFT -> t("Sol analog çubuk", "Left stick")
    ControlType.STICK_RIGHT -> t("Sağ analog çubuk", "Right stick")
    ControlType.DPAD -> "D-Pad"
    ControlType.TRIGGER_LEFT -> t("LT tetik", "LT trigger")
    ControlType.TRIGGER_RIGHT -> t("RT tetik", "RT trigger")
    ControlType.FACE_CLUSTER -> t("ABXY kümesi", "ABXY cluster")
}
