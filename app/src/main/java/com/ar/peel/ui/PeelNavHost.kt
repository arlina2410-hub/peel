package com.ar.peel.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ar.peel.data.AppSettings
import com.ar.peel.render.NoteRenderer
import com.ar.peel.ui.screens.BinScreen
import com.ar.peel.ui.screens.EditorScreen
import com.ar.peel.ui.screens.HowItWorksScreen
import com.ar.peel.ui.screens.NotesScreen
import com.ar.peel.ui.screens.OnboardingScreen
import com.ar.peel.ui.screens.PreviewScreen
import com.ar.peel.ui.screens.SettingsScreen
import com.ar.peel.ui.screens.WallpaperSetupScreen
import com.ar.peel.ui.screens.WhyNoteScreen
import com.ar.peel.wallpaper.BaseStore
import com.ar.peel.wallpaper.WallpaperCapture

@Composable
fun PeelNavHost(
    vm: PeelViewModel,
    renderer: NoteRenderer,
    baseStore: BaseStore,
    capture: WallpaperCapture,
    settings: AppSettings,
) {
    val nav = rememberNavController()
    val startDestination = if (settings.onboardingDone) "notes" else "onboarding"

    NavHost(
        navController = nav,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable("onboarding") {
            OnboardingScreen(
                renderer = renderer,
                onSetup = { nav.navigate("setup/false") },
            )
        }

        composable(
            route = "setup/{recapture}",
            arguments = listOf(
                navArgument("recapture") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            val recapture = entry.arguments?.getBoolean("recapture") ?: false
            WallpaperSetupScreen(
                vm = vm,
                capture = capture,
                recapture = recapture,
                onBack = { nav.popBackStack() },
                onDone = {
                    if (recapture) {
                        nav.popBackStack()
                    } else {
                        nav.navigate("notes") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                },
            )
        }

        composable("notes") {
            NotesScreen(
                vm = vm,
                renderer = renderer,
                onNewNote = {
                    if (!vm.hasBase) {
                        nav.navigate("setup/true")
                    } else {
                        vm.newDraft()
                        nav.navigate("editor")
                    }
                },
                onEditNote = { note ->
                    vm.editDraft(note)
                    nav.navigate("editor")
                },
                onMoveNote = { note ->
                    vm.moveDraft(note)
                    nav.navigate("preview")
                },
                onBin = { nav.navigate("bin") },
                onSettings = { nav.navigate("settings") },
                onFixWallpaper = { nav.navigate("setup/true") },
            )
        }

        composable("editor") {
            EditorScreen(
                vm = vm,
                renderer = renderer,
                onBack = { nav.popBackStack() },
                onNext = { nav.navigate("preview") },
            )
        }

        composable("preview") {
            PreviewScreen(
                vm = vm,
                renderer = renderer,
                baseStore = baseStore,
                showMockDefault = settings.showMockOverlayDefault,
                onBack = { nav.popBackStack() },
                onSaved = {
                    val popped = nav.popBackStack("notes", inclusive = false)
                    if (!popped) {
                        nav.navigate("notes") {
                            popUpTo(nav.graph.id) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable("bin") {
            BinScreen(vm = vm, renderer = renderer, onBack = { nav.popBackStack() })
        }

        composable("settings") {
            SettingsScreen(
                vm = vm,
                settings = settings,
                baseStore = baseStore,
                onBack = { nav.popBackStack() },
                onRecapture = { nav.navigate("setup/true") },
                onHowItWorks = { nav.navigate("help") },
                onWhyNote = { nav.navigate("why") },
            )
        }

        composable("help") {
            HowItWorksScreen(onBack = { nav.popBackStack() })
        }

        composable("why") {
            WhyNoteScreen(onBack = { nav.popBackStack() })
        }
    }
}
