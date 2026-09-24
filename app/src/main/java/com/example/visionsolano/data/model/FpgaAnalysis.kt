package com.example.visionsolano.data.model

import androidx.compose.ui.graphics.Color
import com.example.visionsolano.ui.theme.AlertRed
import com.example.visionsolano.ui.theme.CyberCyan
import com.example.visionsolano.ui.theme.StatusGreen
import com.example.visionsolano.ui.theme.WarningAmber

enum class ThreatLevel(val label: String, val levelIndex: Int) {
    BAJO("BAJO", 1),
    MEDIO("MEDIO", 2),
    ALTO("ALTO", 3),
    CRITICO("CRÍTICO", 4);

    fun getColor(): Color = when (this) {
        BAJO -> StatusGreen
        MEDIO -> WarningAmber
        ALTO -> AlertRed
        CRITICO -> AlertRed
    }
}

data class FpgaAnalysis(
    val currentState: String = "SIN AMENAZA",
    val movementDetected: Boolean = false,
    val eventDetected: String = "NINGUNO",
    val threatLevel: ThreatLevel = ThreatLevel.BAJO,
    val threatPercentage: Float = 0.08f, // 0.0 to 1.0
    val confidenceScore: Float = 0.96f,
    val processingTimeMs: Float = 1.4f,
    val activeSensorsCount: Int = 4,
    val lastAnalysisTimestamp: String = "10:25:00"
)
