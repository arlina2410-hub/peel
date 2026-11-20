package com.ar.peel.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ar.peel.data.NoteEntity
import com.ar.peel.render.NoteRenderer
import com.ar.peel.ui.PeelViewModel
import com.ar.peel.ui.components.ConfirmDialog
import com.ar.peel.ui.components.NotePreview
import com.ar.peel.ui.components.PeelTopBar
import com.ar.peel.ui.theme.PeelColors
import com.ar.peel.ui.theme.PeelDimens
import com.ar.peel.wallpaper.NoteVisuals

@Composable
fun BinScreen(vm: PeelViewModel, renderer: NoteRenderer, onBack: () -> Unit) {
    val deleted by vm.deleted.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var deleteForever by remember { mutableStateOf<NoteEntity?>(null) }
    var emptyBin by remember { mutableStateOf(false) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PeelTopBar(title = "Recycle Bin", onBack = onBack)
        if (deleted.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(PeelDimens.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text("Your bin is empty", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Deleted notes wait here until you remove them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = 6.dp,
                    bottom = navBottom + 30.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        "Notes stay here until you delete them. Nothing is removed automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = PeelDimens.screenPadding, vertical = 6.dp),
                    )
                }
                items(deleted, key = { it.id }) { note ->
                    BinCard(
                        note = note,
                        renderer = renderer,
                        addedLabel = NoteVisuals.stamp(note.createdAt, context),
                        deletedLabel = note.deletedAt?.let { NoteVisuals.stamp(it, context) }.orEmpty(),
                        onRestore = { vm.restoreNote(note) },
                        onDeleteForever = { deleteForever = note },
                    )
                }
                item {
                    TextButton(
                        onClick = { emptyBin = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = PeelDimens.screenPadding),
                    ) {
                        Text("Empty bin", color = PeelColors.Danger, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    deleteForever?.let { note ->
        ConfirmDialog(
            title = "Delete forever?",
            message = "This note can't be brought back.",
            confirmLabel = "Delete forever",
            destructive = true,
            onConfirm = {
                deleteForever = null
                vm.deleteForever(note)
            },
            onDismiss = { deleteForever = null },
        )
    }
    if (emptyBin) {
        ConfirmDialog(
            title = "Empty the bin?",
            message = "All ${deleted.size} deleted notes will be gone for good.",
            confirmLabel = "Empty bin",
            destructive = true,
            onConfirm = {
                emptyBin = false
                vm.emptyBin()
            },
            onDismiss = { emptyBin = false },
        )
    }
}

@Composable
private fun BinCard(
    note: NoteEntity,
    renderer: NoteRenderer,
    addedLabel: String,
    deletedLabel: String,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
) {
    val context = LocalContext.current
    val visual = remember(note) { NoteVisuals.from(note, context) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PeelDimens.screenPadding),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                ) {
                    NotePreview(
                        renderer = renderer,
                        visual = visual,
                        modifier = Modifier.fillMaxSize(),
                        padding = 4.dp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = note.body.lineSequence().firstOrNull().orEmpty().ifBlank { "Empty note" },
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Added $addedLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Deleted $deletedLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onRestore) {
                    Text("Restore", color = PeelColors.Accent, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = onDeleteForever) {
                    Text("Delete forever", color = PeelColors.Danger)
                }
            }
        }
    }
}
