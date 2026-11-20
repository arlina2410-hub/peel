package com.ar.peel.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ar.peel.data.NoteEntity
import com.ar.peel.data.NoteMode
import com.ar.peel.render.NotePalette
import com.ar.peel.render.NoteRenderer
import com.ar.peel.render.NoteVisual
import com.ar.peel.ui.PeelViewModel
import com.ar.peel.ui.components.ColorSwatchRow
import com.ar.peel.ui.components.ConfirmDialog
import com.ar.peel.ui.components.LargeTitleHeader
import com.ar.peel.ui.components.NotePreview
import com.ar.peel.ui.components.TagChip
import com.ar.peel.ui.theme.PeelColors
import com.ar.peel.ui.theme.PeelDimens
import com.ar.peel.wallpaper.NoteVisuals
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
    vm: PeelViewModel,
    renderer: NoteRenderer,
    onNewNote: () -> Unit,
    onEditNote: (NoteEntity) -> Unit,
    onMoveNote: (NoteEntity) -> Unit,
    onBin: () -> Unit,
    onSettings: () -> Unit,
    onFixWallpaper: () -> Unit,
) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    val deleted by vm.deleted.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var pendingDelete by remember { mutableStateOf<NoteEntity?>(null) }
    var actionNote by remember { mutableStateOf<NoteEntity?>(null) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val busyCount = if (notes.count { it.onLock } > 12 || notes.count { it.onHome } > 12) true else false

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 108.dp + navBottom),
        ) {
            item {
                LargeTitleHeader(title = "Notes") {
                    IconButton(onClick = vm::reapply, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Re-apply notes")
                    }
                    IconButton(onClick = onBin, modifier = Modifier.size(48.dp)) {
                        BadgedBox(
                            badge = {
                                if (deleted.isNotEmpty()) {
                                    Badge(containerColor = PeelColors.Accent, contentColor = PeelColors.Ink) {
                                        Text("${deleted.size}")
                                    }
                                }
                            },
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Recycle Bin")
                        }
                    }
                    IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            }
            if (!vm.hasBase) {
                item {
                    SetupCard(onSetUp = onFixWallpaper)
                }
            }
            if (vm.wallpaperChanged) {
                item {
                    WallpaperChangedCard(
                        onFix = onFixWallpaper,
                        onDismiss = vm::dismissWallpaperBanner,
                    )
                }
            }
            if (busyCount) {
                item {
                    Text(
                        text = "A lot of notes can cover your wallpaper.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = PeelDimens.screenPadding, vertical = 8.dp),
                    )
                }
            }
            if (notes.isEmpty()) {
                item {
                    EmptyNotes(renderer = renderer, onNewNote = onNewNote)
                }
            } else {
                items(notes, key = { it.id }) { note ->
                    SwipeableNoteRow(
                        note = note,
                        renderer = renderer,
                        onClick = { onEditNote(note) },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            actionNote = note
                        },
                        onSwipeDelete = { pendingDelete = note },
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNewNote,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(PeelDimens.screenPadding),
            containerColor = PeelColors.Accent,
            contentColor = PeelColors.Ink,
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("New note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        )
    }

    if (actionNote != null) {
        val note = actionNote!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { actionNote = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = PeelDimens.screenPadding, vertical = 10.dp),
            ) {
                Text(
                    text = note.body.lineSequence().firstOrNull().orEmpty().ifBlank { "Note" },
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(14.dp))
                SheetAction("Edit", Icons.Filled.Edit) {
                    actionNote = null
                    onEditNote(note)
                }
                SheetAction("Move on screen", Icons.Filled.Place) {
                    actionNote = null
                    onMoveNote(note)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                ColorSwatchRow(
                    selectedId = note.colorId,
                    onSelect = { color ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        actionNote = null
                        vm.changeColor(note, color.id)
                    },
                )
                Spacer(Modifier.height(10.dp))
                SheetAction("Delete", Icons.Filled.Delete, destructive = true) {
                    actionNote = null
                    pendingDelete = note
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    pendingDelete?.let { note ->
        ConfirmDialog(
            title = "Move to Recycle Bin?",
            message = "You can restore this note from the bin later.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                pendingDelete = null
                vm.deleteNote(note)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun SwipeableNoteRow(
    note: NoteEntity,
    renderer: NoteRenderer,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onSwipeDelete()
            false
        },
        positionalThreshold = { distance -> distance * 0.35f },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = PeelDimens.screenPadding, vertical = 4.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PeelColors.Danger.copy(alpha = 0.9f)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(end = 24.dp),
                )
            }
        },
    ) {
        NoteRow(
            note = note,
            renderer = renderer,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteRow(
    note: NoteEntity,
    renderer: NoteRenderer,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val visual = remember(note) { NoteVisuals.from(note, context) }
    val preview = if (NoteMode.from(note.mode) == NoteMode.LIST) {
        note.body.split('\n').take(3)
            .joinToString("   ") { line -> "• $line" }
    } else {
        note.body.replace('\n', ' ')
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PeelDimens.screenPadding, vertical = 4.dp)
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            ) {
                NotePreview(
                    renderer = renderer,
                    visual = visual,
                    modifier = Modifier.fillMaxSize(),
                    padding = 5.dp,
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = preview.ifBlank { "Empty note" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (note.onLock && note.onHome) {
                        TagChip("Lock · Home")
                    } else if (note.onLock) {
                        TagChip("Lock")
                    } else {
                        TagChip("Home")
                    }
                    Text(
                        text = NoteVisuals.stamp(note.createdAt, context),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNotes(renderer: NoteRenderer, onNewNote: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PeelDimens.screenPadding, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NotePreview(
            renderer = renderer,
            visual = NoteVisual(
                body = "Stick your\nfirst note",
                listMode = false,
                checked = emptySet(),
                color = NotePalette.byId("yellow"),
                timeLabel = "Fri, 20 Nov 2026 · 6:42 PM",
                rotationDeg = -3.2f,
            ),
            modifier = Modifier.size(190.dp),
            padding = 18.dp,
        )
        Spacer(Modifier.height(24.dp))
        Text("Stick your first note", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Type a list or a message. It will sit on your lock screen until you peel it off.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onNewNote,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PeelColors.Accent, contentColor = PeelColors.Ink),
        ) {
            Text("New note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SetupCard(onSetUp: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PeelDimens.screenPadding, vertical = 6.dp),
        color = PeelColors.Accent.copy(alpha = 0.16f),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Set up your wallpaper", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Peel needs one private copy of your wallpaper before your first note can stick.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSetUp,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PeelColors.Accent, contentColor = PeelColors.Ink),
            ) {
                Text("Set up", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WallpaperChangedCard(onFix: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PeelDimens.screenPadding, vertical = 6.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Your wallpaper changed", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Use the new one as your base and re-apply your notes?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onFix,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PeelColors.Accent, contentColor = PeelColors.Ink),
                ) {
                    Text("Fix it", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss) {
                    Text("Not now", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SheetAction(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (destructive) PeelColors.Danger else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (destructive) PeelColors.Danger else MaterialTheme.colorScheme.onSurface,
        )
    }
}
