package com.example.visionsolano.ui.screens

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Inicio")
    object Camera : Screen("camera", "Cámara")
    object Events : Screen("events", "Eventos")
    object Settings : Screen("settings", "Configuración")

    companion object {
        val items: List<Screen>
            get() = listOf(Dashboard, Camera, Events, Settings)
    }
}
