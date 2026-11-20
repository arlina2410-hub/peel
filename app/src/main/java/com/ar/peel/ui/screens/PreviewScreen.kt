package com.ar.peel.ui.screens

import android.graphics.Bitmap
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ar.peel.data.NoteMode
import com.ar.peel.render.NotePalette
import com.ar.peel.render.NoteRenderer
import com.ar.peel.render.NoteVisual
import com.ar.peel.ui.PeelViewModel
import com.ar.peel.ui.components.LabeledSlider
import com.ar.peel.ui.components.PeelSwitch
import com.ar.peel.ui.components.PeelTopBar
import com.ar.peel.ui.components.PrimaryButton
import com.ar.peel.ui.components.SegmentedControl
import com.ar.peel.ui.theme.PeelColors
import com.ar.peel.util.NoteGeometry
import com.ar.peel.wallpaper.BaseStore
import com.ar.peel.wallpaper.NoteVisuals
import com.ar.peel.wallpaper.WallpaperTarget
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PreviewScreen(
    vm: PeelViewModel,
    renderer: NoteRenderer,
    baseStore: BaseStore,
    showMockDefault: Boolean,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val draft = vm.draft
    if (draft == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val notes by vm.notes.collectAsStateWithLifecycle()
    var lockTab by remember { mutableStateOf(draft.onLock) }
    val activeLock = if (draft.onLock && draft.onHome) lockTab else draft.onLock
    var showMock by remember { mutableStateOf(showMockDefault) }
    var saving by remember { mutableStateOf(false) }
    var snapX by remember { mutableStateOf(false) }
    var snapY by remember { mutableStateOf(false) }

    val tabs = buildList {
        if (draft.onLock) add("Lock")
        if (draft.onHome) add("Home")
    }
    val tabIndex = if (activeLock) tabs.indexOf("Lock").coerceAtLeast(0) else tabs.indexOf("Home").coerceAtLeast(0)

    val draftTime = remember(draft.createdAt) {
        NoteVisuals.stamp(draft.createdAt ?: System.currentTimeMillis(), context)
    }
    val existing = remember(notes, activeLock, context) {
        notes.filter { if (activeLock) it.onLock else it.onHome }
            .sortedBy { it.createdAt }
            .filter { it.id != draft.id }
            .map { it to NoteVisuals.from(it, context) }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val target = if (activeLock) WallpaperTarget.LOCK else WallpaperTarget.HOME

        val baseKey = remember(target, widthPx, heightPx, vm.baseVersion) {
            listOf(target, widthPx, heightPx, vm.baseVersion)
        }
        val base by produceState<Bitmap?>(initialValue = null, baseKey) {
            value = baseStore.loadForScreen(target, widthPx.roundToInt(), heightPx.roundToInt())
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeLock, draft.id) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        val current = vm.draft ?: return@detectTransformGestures
                        if (!current.isOn(activeLock)) return@detectTransformGestures
                        val rawX = current.centerX(activeLock) + pan.x / size.width
                        val rawY = current.centerY(activeLock) + pan.y / size.height
                        val nearCenterX = abs(rawX - 0.5f) < 0.02f
                        val nearCenterY = abs(rawY - 0.5f) < 0.02f
                        if ((nearCenterX && !snapX) || (nearCenterY && !snapY)) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        snapX = nearCenterX
                        snapY = nearCenterY
                        val x = NoteGeometry.clampX(if (nearCenterX) 0.5f else rawX)
                        val y = NoteGeometry.clampY(if (nearCenterY) 0.5f else rawY)
                        val newWidth = NoteGeometry.clampWidth(current.widthFraction * zoom)
                        val newRotation = NoteGeometry.clampRotation(current.rotationDeg + rotation)
                        vm.updateDraft {
                            it.withCenter(activeLock, x, y).copy(
                                widthFraction = newWidth,
                                rotationDeg = newRotation,
                            )
                        }
                    }
                },
        ) {
            val bitmap = base
            if (bitmap != null) {
                drawContext.canvas.nativeCanvas.drawBitmap(bitmap, 0f, 0f, null)
            } else {
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color(0xFF7C9DB8), Color(0xFFE0C4A6), Color(0xFF5F6F7C)),
                    ),
                )
            }

            existing.forEach { (source, visual) ->
                val x = (if (activeLock) source.lockX else source.homeX) * size.width
                val y = (if (activeLock) source.lockY else source.homeY) * size.height
                val noteWidth = NoteGeometry.noteWidthPx(
                    size.width.roundToInt(),
                    size.height.roundToInt(),
                    source.widthFraction,
                )
                renderer.draw(
                    canvas = drawContext.canvas.nativeCanvas,
                    visual = visual,
                    centerX = x,
                    centerY = y,
                    widthPx = noteWidth,
                )
            }

            val visual = NoteVisual(
                body = if (draft.mode == NoteMode.LIST) {
                    draft.items.joinToString("\n") { it.text }
                } else {
                    draft.body
                },
                listMode = draft.mode == NoteMode.LIST,
                checked = draft.checkedIndexes(),
                color = NotePalette.byId(draft.colorId),
                timeLabel = draftTime,
                rotationDeg = draft.rotationDeg,
            )
            val draftX = draft.centerX(activeLock) * size.width
            val draftY = draft.centerY(activeLock) * size.height
            val draftWidth = NoteGeometry.noteWidthPx(
                size.width.roundToInt(),
                size.height.roundToInt(),
                draft.widthFraction,
            )
            renderer.draw(
                canvas = drawContext.canvas.nativeCanvas,
                visual = visual,
                centerX = draftX,
                centerY = draftY,
                widthPx = draftWidth,
                highlight = true,
            )

            if (snapX) {
                drawLine(
                    color = PeelColors.Guide,
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
            if (snapY) {
                drawLine(
                    color = PeelColors.Guide,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
        }

        if (showMock) {
            MockOverlay(lock = activeLock)
        }

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        ) {
            Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f)) {
                Column {
                    PeelTopBar(title = "Place your note", onBack = onBack)
                    if (tabs.size > 1) {
                        SegmentedControl(
                            options = tabs,
                            selectedIndex = tabIndex,
                            onSelected = { index ->
                                lockTab = tabs.getOrNull(index) == "Lock"
                                snapX = false
                                snapY = false
                            },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    } else {
                        Text(
                            text = if (activeLock) "Lock screen" else "Home screen",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Show clock & icons", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Your phone's clock and icons sit on top of the note. Position can vary slightly by phone.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    PeelSwitch(checked = showMock, onCheckedChange = { showMock = it })
                }
                Spacer(Modifier.height(6.dp))
                LabeledSlider(
                    label = "Size",
                    valueText = "${(draft.widthFraction * 100).roundToInt()}%",
                    value = draft.widthFraction,
                    range = NoteGeometry.MIN_WIDTH_FRACTION..NoteGeometry.MAX_WIDTH_FRACTION,
                    onValueChange = { value ->
                        vm.updateDraft { it.copy(widthFraction = NoteGeometry.clampWidth(value)) }
                    },
                )
                LabeledSlider(
                    label = "Rotation",
                    valueText = "${draft.rotationDeg.roundToInt()}°",
                    value = draft.rotationDeg,
                    range = NoteGeometry.MIN_ROTATION..NoteGeometry.MAX_ROTATION,
                    onValueChange = { value ->
                        vm.updateDraft { it.copy(rotationDeg = NoteGeometry.clampRotation(value)) }
                    },
                )
                Spacer(Modifier.height(4.dp))
                PrimaryButton(
                    text = if (saving) "Sticking…" else "Save & Stick",
                    enabled = !saving && draft.savedBody().isNotBlank(),
                    onClick = {
                        if (!saving) {
                            saving = true
                            vm.saveDraft(onSaved = onSaved)
                        }
                    },
                )
            }
        }
    }

    LaunchedEffect(snapX, snapY) {
        if (snapX || snapY) {
            delay(700)
            snapX = false
            snapY = false
        }
    }
}

@Composable
private fun MockOverlay(lock: Boolean) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val is24 = DateFormat.is24HourFormat(context)
    val now = remember { LocalDateTime.now() }
    val clock = remember {
        now.format(DateTimeFormatter.ofPattern(if (is24) "H:mm" else "h:mm", locale))
    }
    val date = remember { now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)) }

    if (lock) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 122.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    clock,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.92f),
                )
                Text(date, fontSize = 15.sp, color = Color.White.copy(alpha = 0.78f))
            }
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(2) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .background(Color.White.copy(alpha = 0.24f), CircleShape),
                            )
                            Spacer(Modifier.size(10.dp))
                            Column {
                                Box(
                                    Modifier
                                        .size(width = 90.dp, height = 9.dp)
                                        .background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(50)),
                                )
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    Modifier
                                        .size(width = 180.dp, height = 7.dp)
                                        .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(50)),
                                )
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 286.dp, start = 34.dp, end = 34.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                )
                Box(
                    Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                )
            }
        }
    } else {
        Box(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 118.dp, start = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(clock, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                Text("Fri 20", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
            }
            Canvas(
                Modifier
                    .fillMaxSize()
                    .padding(top = 160.dp, bottom = 280.dp, start = 26.dp, end = 26.dp),
            ) {
                val columns = 4
                val rows = 6
                val gap = size.width * 0.06f
                val cell = (size.width - gap * (columns - 1)) / columns
                val icon = cell * 0.62f
                repeat(rows) { row ->
                    repeat(columns) { column ->
                        val left = column * (cell + gap) + (cell - icon) / 2f
                        val top = row * (cell + gap) + (cell - icon) / 2f
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.17f),
                            topLeft = Offset(left, top),
                            size = Size(icon, icon),
                            cornerRadius = CornerRadius(icon * 0.26f),
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 272.dp)
                    .size(width = 250.dp, height = 74.dp),
                shape = RoundedCornerShape(26.dp),
                color = Color.White.copy(alpha = 0.15f),
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    repeat(4) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(13.dp)),
                        )
                    }
                }
            }
        }
    }
}
