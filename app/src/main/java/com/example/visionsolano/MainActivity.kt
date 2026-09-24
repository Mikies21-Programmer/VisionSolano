package com.example.visionsolano

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.visionsolano.data.model.SystemMode
import com.example.visionsolano.ui.components.VisionSolanoBottomNav
import com.example.visionsolano.ui.components.VisionSolanoTopBar
import com.example.visionsolano.ui.screens.Screen
import com.example.visionsolano.ui.screens.camera.CameraScreen
import com.example.visionsolano.ui.screens.dashboard.DashboardScreen
import com.example.visionsolano.ui.screens.events.EventsScreen
import com.example.visionsolano.ui.screens.settings.SettingsScreen
import com.example.visionsolano.ui.screens.splash.SplashScreen
import com.example.visionsolano.ui.theme.DarkBackground
import com.example.visionsolano.ui.theme.VisionSolanoTheme
import com.example.visionsolano.viewmodel.SurveillanceViewModel

class MainActivity : ComponentActivity() {

    private val surveillanceViewModel: SurveillanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VisionSolanoTheme {
                VisionSolanoApp(viewModel = surveillanceViewModel)
            }
        }
    }
}

@Composable
fun VisionSolanoApp(viewModel: SurveillanceViewModel) {
    var showSplash by remember { mutableStateOf(true) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    val systemStatus by viewModel.systemStatus.collectAsState()

    AnimatedContent(
        targetState = showSplash,
        transitionSpec = {
            fadeIn(tween(500)) togetherWith fadeOut(tween(500))
        },
        label = "SplashToMainTransition"
    ) { isSplashScreen ->
        if (isSplashScreen) {
            SplashScreen(
                onSplashFinished = {
                    showSplash = false
                }
            )
        } else {
            // Handle back button to return to Dashboard if on other tabs
            BackHandler(enabled = currentScreen != Screen.Dashboard) {
                currentScreen = Screen.Dashboard
            }

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground),
                topBar = {
                    VisionSolanoTopBar(
                        onSettingsClick = { currentScreen = Screen.Settings },
                        isSystemActive = systemStatus.systemMode == SystemMode.ACTIVO
                    )
                },
                bottomBar = {
                    VisionSolanoBottomNav(
                        currentScreen = currentScreen,
                        onScreenSelected = { screen ->
                            currentScreen = screen
                        }
                    )
                },
                containerColor = DarkBackground
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            Screen.Dashboard -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToCamera = { currentScreen = Screen.Camera },
                                onNavigateToEvents = { currentScreen = Screen.Events },
                                onNavigateToSettings = { currentScreen = Screen.Settings }
                            )

                            Screen.Camera -> CameraScreen(
                                viewModel = viewModel
                            )

                            Screen.Events -> EventsScreen(
                                viewModel = viewModel
                            )

                            Screen.Settings -> SettingsScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}