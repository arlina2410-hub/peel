package com.ar.peel

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ar.peel.data.AppSettings
import com.ar.peel.data.ThemeMode
import com.ar.peel.render.NoteRenderer
import com.ar.peel.ui.PeelNavHost
import com.ar.peel.ui.PeelViewModel
import com.ar.peel.ui.theme.PeelTheme
import com.ar.peel.wallpaper.ApplyStatus
import com.ar.peel.wallpaper.BaseStore
import com.ar.peel.wallpaper.WallpaperCapture

class MainActivity : ComponentActivity() {
    private val viewModel: PeelViewModel by viewModels {
        PeelViewModel.Factory((application as PeelApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PeelApp).container
        setContent {
            PeelAppRoot(
                vm = viewModel,
                renderer = container.renderer,
                baseStore = container.baseStore,
                capture = container.capture,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkWallpaperChanged()
    }
}

@Composable
private fun PeelAppRoot(
    vm: PeelViewModel,
    renderer: NoteRenderer,
    baseStore: BaseStore,
    capture: WallpaperCapture,
) {
    val settingsState by vm.settings.collectAsStateWithLifecycle()
    val settings: AppSettings = settingsState ?: run {
        PeelTheme(ThemeMode.SYSTEM) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            )
        }
        return
    }

    PeelTheme(settings.themeMode) {
        val status by vm.applyStatus.collectAsStateWithLifecycle()
        val context = LocalContext.current

        LaunchedEffect(status) {
            if (status == ApplyStatus.Success) {
                Toast.makeText(context, "Stuck ✓. Lock your phone to see it.", Toast.LENGTH_SHORT).show()
                vm.acknowledgeApplySuccess()
            }
        }

        Box(Modifier.fillMaxSize()) {
            PeelNavHost(
                vm = vm,
                renderer = renderer,
                baseStore = baseStore,
                capture = capture,
                settings = settings,
            )

            if (status == ApplyStatus.Working) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 6.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Sticking your note…", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            val error = status as? ApplyStatus.Error
            if (error != null) {
                AlertDialog(
                    onDismissRequest = vm::clearApplyError,
                    title = { Text("Couldn't update your wallpaper", style = MaterialTheme.typography.titleLarge) },
                    text = { Text(error.message, style = MaterialTheme.typography.bodyMedium) },
                    confirmButton = {
                        TextButton(onClick = {
                            vm.clearApplyError()
                            vm.reapply()
                        }) {
                            Text("Try again", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = vm::clearApplyError) {
                            Text("Not now", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                )
            }
        }
    }
}
