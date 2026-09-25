package com.example.visionsolano.data.model

import com.example.visionsolano.data.network.NetworkConfig

/**
 * Modelo de datos que representa un dispositivo VisionSolano descubierto mediante mDNS/NSD.
 */
data class VisionSolanoDevice(
    val deviceId: String,
    val hostName: String,
    val ipAddress: String,
    val port: Int = NetworkConfig.HTTP_PORT,
    val role: String = "esp32",
    val protocolVersion: Int = 1,
    val apiVersion: Int = 1,
    val camera: String = "ov2640",
    val discoveredAt: Long = System.currentTimeMillis()
) {
    /**
     * Host destino preferente para comunicaciones de red.
     * Prioriza el hostname mDNS estable (.local) evitando depender de una IP DHCP volátil.
     * Si el hostname está ausente, recurre a la ipAddress resuelta.
     */
    val targetHost: String
        get() = hostName.ifBlank { ipAddress }

    /**
     * Retorna la URL de stream asociada a este dispositivo.
     */
    val streamUrl: String
        get() = NetworkConfig.buildStreamUrl(targetHost, port)

    /**
     * Retorna la URL del endpoint de telemetría y estado.
     */
    val statusUrl: String
        get() = NetworkConfig.buildStatusUrl(targetHost, port)

    /**
     * Retorna la URL del endpoint de comandos de control.
     */
    val controlUrl: String
        get() = NetworkConfig.buildControlUrl(targetHost, port)

    companion object {
        /**
         * Dispositivo Mock predeterminado para pruebas sin hardware real conectado.
         * NOTA: 192.168.1.50 es únicamente un dato de prueba/mock, NO una IP fija del producto.
         */
        fun createMockDevice(): VisionSolanoDevice = VisionSolanoDevice(
            deviceId = "1",
            hostName = NetworkConfig.EXPECTED_HOSTNAME,
            ipAddress = "192.168.1.50",
            port = NetworkConfig.HTTP_PORT,
            role = NetworkConfig.EXPECTED_ROLE,
            protocolVersion = NetworkConfig.EXPECTED_PROTOCOL_VERSION,
            apiVersion = NetworkConfig.EXPECTED_API_VERSION,
            camera = NetworkConfig.EXPECTED_CAMERA,
            discoveredAt = System.currentTimeMillis()
        )
    }
}
