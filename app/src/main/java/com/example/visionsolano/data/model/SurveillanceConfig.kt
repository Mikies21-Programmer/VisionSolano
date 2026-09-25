package com.example.visionsolano.data.model

data class SurveillanceConfig(
    val espCamIp: String = "192.168.1.50",
    val espCamPort: Int = 80,
    val fpgaBridgeIp: String = "192.168.1.100",
    val fpgaPort: Int = 5000,
    val connectionProtocol: String = "TCP Raw",
    val surveillanceActive: Boolean = true,
    val motionDetectionActive: Boolean = true,
    val fpgaSensitivity: Float = 0.75f, // 0.0 to 1.0
    val samplingFrequencyHz: Int = 20,
    val immediateNotifications: Boolean = true,
    val audioAlarmOnHighThreat: Boolean = false,
    val autoSaveSnapshots: Boolean = true,
    val firmwareVersion: String = "v1.0-alpha",
    val hardwareTarget: String = "ESP32-S3 + OV2640 + Nexys 4"
)
