package com.ar.peel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ar.peel.data.DraftItem
import com.ar.peel.data.NoteMode
import com.ar.peel.render.NoteRenderer
import com.ar.peel.ui.PeelViewModel
import com.ar.peel.ui.components.ColorSwatchRow
import com.ar.peel.ui.components.ConfirmDialog
import com.ar.peel.ui.components.NotePreview
import com.ar.peel.ui.components.PeelTopBar
import com.ar.peel.ui.components.PrimaryButton
import com.ar.peel.ui.components.SegmentedControl
import com.ar.peel.ui.theme.PeelColors
import com.ar.peel.ui.theme.PeelDimens
import com.ar.peel.wallpaper.NoteVisuals

private const val MAX_CHARS = 2000

@Composable
fun EditorScreen(
    vm: PeelViewModel,
    renderer: NoteRenderer,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val draft = vm.draft
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var showDelete by remember { mutableStateOf(false) }

    if (draft == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val valid = draft.savedBody().isNotBlank()
    val visual = NoteVisuals.from(draft, context)
    val overflowing = renderer.isOverflowing(visual)
    val listCharCount = draft.items.sumOf { it.text.length }

    fun changeMode(mode: NoteMode) {
        if (mode == draft.mode) return
        vm.updateDraft { current ->
            if (mode == NoteMode.LIST) {
                val lines = current.body.split('\n')
                current.copy(
                    mode = NoteMode.LIST,
                    items = if (lines.size == 1 && lines.first().isBlank()) {
                        listOf(DraftItem())
                    } else {
                        lines.map { DraftItem(it) }
                    },
                )
            } else {
                current.copy(
                    mode = NoteMode.TEXT,
                    body = current.items.joinToString("\n") { it.text },
                )
            }
        }
    }

    fun updateItem(index: Int, text: String) {
        if (text.contains('\n')) return
        vm.updateDraft { current ->
            val otherChars = current.items.sumOf { it.text.length } - current.items[index].text.length
            val trimmed = text.take((MAX_CHARS - otherChars).coerceAtLeast(1))
            current.copy(
                items = current.items.mapIndexed { i, item ->
                    if (i == index) item.copy(text = trimmed) else item
                },
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        PeelTopBar(
            title = if (draft.id == null) "New note" else "Edit note",
            onBack = {
                vm.discardDraft()
                onBack()
            },
            actions = {
                if (draft.id != null) {
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete note")
                    }
                }
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PeelDimens.screenPadding),
        ) {
            Spacer(Modifier.height(6.dp))
            SegmentedControl(
                options = listOf("Text", "List"),
                selectedIndex = if (draft.mode == NoteMode.TEXT) 0 else 1,
                onSelected = { changeMode(if (it == 0) NoteMode.TEXT else NoteMode.LIST) },
            )
            Spacer(Modifier.height(16.dp))

            if (draft.mode == NoteMode.TEXT) {
                Surface(
                    shape = RoundedCornerShape(PeelDimens.cardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Box(Modifier.fillMaxWidth().heightIn(min = 170.dp)) {
                            if (draft.body.isBlank()) {
                                Text(
                                    "Write anything…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                )
                            }
                            BasicTextField(
                                value = draft.body,
                                onValueChange = { text ->
                                    vm.updateDraft { it.copy(body = text.take(MAX_CHARS)) }
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                cursorBrush = SolidColor(PeelColors.Accent),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 170.dp),
                            )
                        }
                        if (draft.body.length >= 1800) {
                            Text(
                                "${draft.body.length} / $MAX_CHARS",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (draft.body.length >= 1960) PeelColors.Danger else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.End),
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(PeelDimens.cardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(vertical = 6.dp, horizontal = 8.dp)) {
                        draft.items.forEachIndexed { index, item ->
                            ListItemRow(
                                index = index,
                                item = item,
                                itemCount = draft.items.size,
                                onTextChange = { updateItem(index, it) },
                                onToggle = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    vm.updateDraft { current ->
                                        current.copy(
                                            items = current.items.mapIndexed { i, old ->
                                                if (i == index) old.copy(checked = !old.checked) else old
                                            },
                                        )
                                    }
                                },
                                onMoveUp = {
                                    if (index > 0) {
                                        vm.updateDraft { current ->
                                            val items = current.items.toMutableList()
                                            val moved = items.removeAt(index)
                                            items.add(index - 1, moved)
                                            current.copy(items = items)
                                        }
                                    }
                                },
                                onMoveDown = {
                                    if (index < draft.items.lastIndex) {
                                        vm.updateDraft { current ->
                                            val items = current.items.toMutableList()
                                            val moved = items.removeAt(index)
                                            items.add(index + 1, moved)
                                            current.copy(items = items)
                                        }
                                    }
                                },
                                onRemove = {
                                    vm.updateDraft { current ->
                                        val items = current.items.toMutableList()
                                        items.removeAt(index)
                                        if (items.isEmpty()) items += DraftItem()
                                        current.copy(items = items)
                                    }
                                },
                            )
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = {
                                    if (listCharCount + 1 <= MAX_CHARS) {
                                        vm.updateDraft { it.copy(items = it.items + DraftItem()) }
                                    }
                                },
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = PeelColors.Accent)
                                Spacer(Modifier.width(4.dp))
                                Text("Add item", color = PeelColors.Accent, fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.weight(1f))
                            if (listCharCount >= 1800) {
                                Text(
                                    "$listCharCount / $MAX_CHARS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (listCharCount >= 1960) PeelColors.Danger else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Color", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            ColorSwatchRow(
                selectedId = draft.colorId,
                onSelect = { color ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.updateDraft { it.copy(colorId = color.id) }
                },
            )

            Spacer(Modifier.height(22.dp))
            Text("Where it shows", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val targetIndex = when {
                draft.onLock && draft.onHome -> 2
                draft.onHome -> 1
                else -> 0
            }
            SegmentedControl(
                options = listOf("Lock", "Home", "Both"),
                selectedIndex = targetIndex,
                onSelected = { selected ->
                    vm.updateDraft {
                        when (selected) {
                            0 -> it.copy(onLock = true, onHome = false)
                            1 -> it.copy(onLock = false, onHome = true)
                            else -> it.copy(onLock = true, onHome = true)
                        }
                    }
                },
            )

            Spacer(Modifier.height(24.dp))
            Text("Preview", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(PeelDimens.cardRadius))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                NotePreview(
                    renderer = renderer,
                    visual = visual,
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                )
            }
            if (overflowing) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Text is long. Shorten it or make the note bigger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PeelColors.Danger,
                )
            }
            Spacer(Modifier.height(26.dp))
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = PeelDimens.screenPadding, vertical = 10.dp),
            ) {
                PrimaryButton(text = "Next", onClick = onNext, enabled = valid)
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete note?",
            message = "It will go to the Recycle Bin, where you can restore it.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                showDelete = false
                vm.deleteDraft(onDone = onBack)
            },
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun ListItemRow(
    index: Int,
    item: DraftItem,
    itemCount: Int,
    onTextChange: (String) -> Unit,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val ink = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (item.checked) PeelColors.Accent else MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.5.dp,
                        color = if (item.checked) PeelColors.Accent else ink.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(7.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (item.checked) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = PeelColors.Ink,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        BasicTextField(
            value = item.text,
            onValueChange = onTextChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = if (item.checked) ink.copy(alpha = 0.5f) else ink,
                textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
            ),
            cursorBrush = SolidColor(PeelColors.Accent),
            singleLine = true,
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Item options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Move up") },
                    onClick = { menuOpen = false; onMoveUp() },
                    enabled = index > 0,
                    leadingIcon = { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null) },
                )
                DropdownMenuItem(
                    text = { Text("Move down") },
                    onClick = { menuOpen = false; onMoveDown() },
                    enabled = index < itemCount - 1,
                    leadingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                )
                DropdownMenuItem(
                    text = { Text("Remove", color = PeelColors.Danger) },
                    onClick = { menuOpen = false; onRemove() },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = PeelColors.Danger) },
                )
            }
        }
    }
}
