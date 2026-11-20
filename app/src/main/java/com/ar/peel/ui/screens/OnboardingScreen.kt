package com.ar.peel.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ar.peel.render.NotePalette
import com.ar.peel.render.NoteRenderer
import com.ar.peel.render.NoteVisual
import com.ar.peel.ui.PeelViewModel
import com.ar.peel.ui.components.NotePreview
import com.ar.peel.ui.components.PeelTopBar
import com.ar.peel.ui.components.PrimaryButton
import com.ar.peel.ui.theme.PeelColors
import com.ar.peel.ui.theme.PeelDimens
import com.ar.peel.wallpaper.CaptureResult
import com.ar.peel.wallpaper.WallpaperCapture
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val body: String,
)

private val pages = listOf(
    OnboardingPage(
        title = "A note you can't miss",
        body = "Peel puts a real-looking sticky note on your lock screen, so your list is the first thing you see.",
    ),
    OnboardingPage(
        title = "Your wallpaper stays the same",
        body = "Peel keeps a private copy of your wallpaper and draws your notes on top. Nothing else changes.",
    ),
    OnboardingPage(
        title = "Set up your wallpaper",
        body = "Peel needs one copy of your wallpaper. It never leaves your phone.",
    ),
)

@Composable
fun OnboardingScreen(renderer: NoteRenderer, onSetup: () -> Unit) {
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Spacer(Modifier.weight(1f))
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxWidth()
                .weight(7f),
        ) { page ->
            OnboardingPageContent(page = page, renderer = renderer)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PeelDimens.screenPadding),
            horizontalArrangement = Arrangement.Center,
        ) {
            pages.indices.forEach { index ->
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (index == pager.currentPage) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pager.currentPage) {
                                PeelColors.Accent
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            },
                        ),
                )
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(PeelDimens.screenPadding),
        ) {
            PrimaryButton(
                text = if (pager.currentPage == pages.lastIndex) "Set up my wallpaper" else "Next",
                onClick = {
                    if (pager.currentPage == pages.lastIndex) {
                        onSetup()
                    } else {
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    }
                },
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: Int, renderer: NoteRenderer) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PeelDimens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PhoneIllustration(renderer, page)
        Spacer(Modifier.height(32.dp))
        Text(
            text = pages[page].title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = pages[page].body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun PhoneIllustration(renderer: NoteRenderer, page: Int) {
    val visual = remember(page) {
        NoteVisual(
            body = when (page) {
                0 -> "I have to go\nfor workout"
                1 -> "Milk\nEggs\nBread\nCoffee"
                else -> "Notes that\nstick."
            },
            listMode = page == 1,
            checked = if (page == 1) setOf(0) else emptySet(),
            color = NotePalette.all[if (page == 1) 5 else 0],
            timeLabel = "Fri, 20 Nov 2026 · 6:42 PM",
            rotationDeg = -2.1f,
        )
    }
    Box(
        modifier = Modifier
            .width(190.dp)
            .aspectRatio(0.52f)
            .clip(RoundedCornerShape(34.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF8FB7D9), Color(0xFFD9C6B0), Color(0xFF6D7E8E)),
                ),
            )
            .border(7.dp, Color(0xFF1C1C1E), RoundedCornerShape(34.dp)),
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .width(46.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.4f)),
        )
        NotePreview(
            renderer = renderer,
            visual = visual,
            modifier = Modifier
                .align(if (page == 3) Alignment.Center else Alignment.Center)
                .padding(top = if (page == 0) 48.dp else 90.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.62f),
        )
        if (page == 2) {
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 54.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("9:41", color = Color.White.copy(alpha = 0.92f), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text("Friday, 20 November", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun WallpaperSetupScreen(
    vm: PeelViewModel,
    capture: WallpaperCapture,
    recapture: Boolean,
    onDone: () -> Unit,
    onBack: (() -> Unit)?,
) {
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showLiveWarning by remember { mutableStateOf(capture.isLiveWallpaper()) }
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            message = null
            when (val result = capture.captureFromUri(uri)) {
                CaptureResult.Success -> {
                    vm.finishSetup(recapture)
                    onDone()
                }
                is CaptureResult.Failure -> message = result.message
            }
            busy = false
        }
    }

    fun useCurrentWallpaper() {
        scope.launch {
            busy = true
            message = null
            when (val result = capture.captureCurrentBoth()) {
                CaptureResult.Success -> {
                    vm.finishSetup(recapture)
                    onDone()
                }
                is CaptureResult.Failure -> {
                    message = result.message
                    showLiveWarning = true
                }
            }
            busy = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (onBack != null) {
            PeelTopBar(title = "Wallpaper", onBack = onBack)
        } else {
            Spacer(Modifier.statusBarsPadding().height(12.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PeelDimens.screenPadding),
        ) {
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(PeelDimens.cardRadius))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFB8D4EA), Color(0xFFF3D9B7), Color(0xFF7A8794)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = PeelColors.Accent,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(0.6f),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "Your wallpaper,\nwith a note on top.",
                            style = MaterialTheme.typography.titleMedium,
                            color = PeelColors.Ink,
                        )
                        Spacer(Modifier.height(22.dp))
                        Text(
                            "Fri, 20 Nov 2026 · 6:42 PM",
                            style = MaterialTheme.typography.labelSmall,
                            color = PeelColors.Ink.copy(alpha = 0.6f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
            Text(
                if (recapture) "Update your wallpaper" else "One quick setup",
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Peel keeps a private copy of your wallpaper and draws notes on top of it. Your wallpaper itself stays the same.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(26.dp))
            PrimaryButton(
                text = if (busy) "Reading your wallpaper…" else "Use my current wallpaper",
                onClick = { if (!busy) useCurrentWallpaper() },
                enabled = !busy,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Choose a photo instead",
                    color = PeelColors.Accent,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (busy) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = PeelColors.Accent,
                    )
                }
            }
            if (message != null || showLiveWarning) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = message
                            ?: "Your wallpaper is animated, so Peel can't copy it. Pick a still image to use as your base.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "Nothing leaves your phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!recapture) {
                TextButton(
                    onClick = {
                        vm.skipSetup()
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
