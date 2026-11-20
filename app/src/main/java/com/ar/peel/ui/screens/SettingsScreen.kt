package com.ar.peel.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ar.peel.BuildConfig
import com.ar.peel.data.AppSettings
import com.ar.peel.data.ThemeMode
import com.ar.peel.ui.PeelViewModel
import com.ar.peel.ui.components.ConfirmDialog
import com.ar.peel.ui.components.GroupedCard
import com.ar.peel.ui.components.PeelSwitch
import com.ar.peel.ui.components.PeelTopBar
import com.ar.peel.ui.components.RowDivider
import com.ar.peel.ui.components.SectionHeader
import com.ar.peel.ui.components.SegmentedControl
import com.ar.peel.ui.components.SettingsRow
import com.ar.peel.ui.theme.PeelColors
import com.ar.peel.ui.theme.PeelDimens
import com.ar.peel.wallpaper.BaseStore
import com.ar.peel.wallpaper.WallpaperTarget

@Composable
fun SettingsScreen(
    vm: PeelViewModel,
    settings: AppSettings,
    baseStore: BaseStore,
    onBack: () -> Unit,
    onRecapture: () -> Unit,
    onHowItWorks: () -> Unit,
    onWhyNote: () -> Unit,
) {
    val context = LocalContext.current
    var confirmRestore by remember { mutableStateOf(false) }
    val themeIndex = when (settings.themeMode) {
        ThemeMode.SYSTEM -> 0
        ThemeMode.LIGHT -> 1
        ThemeMode.DARK -> 2
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PeelTopBar(title = "Settings", onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            SectionHeader("Wallpaper")
            GroupedCard(Modifier.padding(horizontal = PeelDimens.screenPadding)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BaseThumb(
                        label = "Lock",
                        target = WallpaperTarget.LOCK,
                        baseStore = baseStore,
                        version = vm.baseVersion,
                        modifier = Modifier.weight(1f),
                    )
                    BaseThumb(
                        label = "Home",
                        target = WallpaperTarget.HOME,
                        baseStore = baseStore,
                        version = vm.baseVersion,
                        modifier = Modifier.weight(1f),
                    )
                }
                RowDivider()
                SettingsRow(
                    title = "Re-capture my wallpaper",
                    subtitle = "Use your current wallpaper as the base",
                    onClick = onRecapture,
                )
                RowDivider()
                SettingsRow(
                    title = "Restore original wallpaper",
                    subtitle = "Puts your wallpaper back without notes",
                    destructive = true,
                    onClick = { confirmRestore = true },
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader("Preview")
            GroupedCard(Modifier.padding(horizontal = PeelDimens.screenPadding)) {
                SettingsRow(
                    title = "Show clock & icons",
                    subtitle = "Default for the placement screen",
                    trailing = {
                        PeelSwitch(
                            checked = settings.showMockOverlayDefault,
                            onCheckedChange = vm::setShowMockOverlay,
                        )
                    },
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader("Appearance")
            GroupedCard(Modifier.padding(horizontal = PeelDimens.screenPadding)) {
                Column(Modifier.padding(14.dp)) {
                    SegmentedControl(
                        options = listOf("System", "Light", "Dark"),
                        selectedIndex = themeIndex,
                        onSelected = { index ->
                            vm.setThemeMode(
                                when (index) {
                                    1 -> ThemeMode.LIGHT
                                    2 -> ThemeMode.DARK
                                    else -> ThemeMode.SYSTEM
                                },
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader("Help")
            GroupedCard(Modifier.padding(horizontal = PeelDimens.screenPadding)) {
                SettingsRow(title = "How it works", onClick = onHowItWorks)
                RowDivider()
                SettingsRow(title = "Why doesn't my note show?", onClick = onWhyNote)
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader("About")
            GroupedCard(Modifier.padding(horizontal = PeelDimens.screenPadding)) {
                SettingsRow(title = "Peel", subtitle = "Version ${BuildConfig.VERSION_NAME}")
                RowDivider()
                SettingsRow(
                    title = "Everything stays on your phone",
                    subtitle = "No accounts, no cloud, no internet permission.",
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Notes that stick.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmRestore) {
        ConfirmDialog(
            title = "Restore your wallpaper?",
            message = "Peel will remove its notes from your wallpaper. Your notes are kept in the app and can be stuck again.",
            confirmLabel = "Restore",
            destructive = true,
            onConfirm = {
                confirmRestore = false
                vm.restoreOriginals { ok ->
                    if (ok) Toast.makeText(context, "Original wallpaper restored.", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { confirmRestore = false },
        )
    }
}

@Composable
private fun BaseThumb(
    label: String,
    target: WallpaperTarget,
    baseStore: BaseStore,
    version: Int,
    modifier: Modifier = Modifier,
) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, target, version) {
        value = baseStore.thumbnail(target, 220)
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(116.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "$label wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                    Text(
                        "Not set",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun HowItWorksScreen(onBack: () -> Unit) {
    InfoPage(
        title = "How it works",
        intro = "Peel puts a picture of your note on top of your own wallpaper. That's why it can show on the lock screen.",
        sections = listOf(
            "1. Your wallpaper" to "Peel keeps a private, untouched copy of your wallpaper on this phone.",
            "2. Your notes" to "Every note is drawn on top of that copy, exactly as it looks in the preview.",
            "3. Your screen" to "Peel sets the finished picture as your wallpaper. The phone's clock, notifications and icons still sit above it.",
            "4. Removing notes" to "Delete every note for a screen and your original wallpaper comes back.",
        ),
        footnote = "Peel never uploads anything. It works the same in airplane mode.",
        onBack = onBack,
    )
}

@Composable
fun WhyNoteScreen(onBack: () -> Unit) {
    InfoPage(
        title = "Why doesn't my note show?",
        intro = "The note is part of the wallpaper picture. If your phone replaces that picture, the note can disappear.",
        sections = listOf(
            "Wallpaper carousel" to "Some phones rotate lock-screen wallpapers automatically (look for \"Wallpaper Carousel\", \"Glance\", \"Lock screen magazine\" or similar). Turn it off.",
            "A new wallpaper" to "If you changed your wallpaper, open Peel and use the banner to make the new picture your base.",
            "Live wallpaper" to "Animated wallpapers can't be copied. Choose a still photo in Peel's setup.",
            "Home screen" to "Launchers can slide or crop the wallpaper. Your note may move a little behind the icons.",
            "Still stuck?" to "Tap the refresh button on the Notes screen, then lock your phone. You can also re-capture your wallpaper in Settings.",
        ),
        footnote = "This is a limit of how Android wallpapers work, not something Peel can tap through.",
        onBack = onBack,
    )
}

@Composable
private fun InfoPage(
    title: String,
    intro: String,
    sections: List<Pair<String, String>>,
    footnote: String,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PeelTopBar(title = title, onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = PeelDimens.screenPadding),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                intro,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            sections.forEach { (heading, body) ->
                Text(
                    heading,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(18.dp))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(PeelColors.Accent.copy(alpha = 0.15f))
                    .padding(16.dp),
            ) {
                Text(
                    footnote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}
