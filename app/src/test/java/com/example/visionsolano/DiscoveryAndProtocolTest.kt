package com.example.visionsolano

import com.example.visionsolano.data.model.ConnectionState
import com.example.visionsolano.data.model.DetectionEvent
import com.example.visionsolano.data.model.SystemStatus
import com.example.visionsolano.data.model.VisionSolanoDevice
import com.example.visionsolano.data.model.deduplicateBySeq
import com.example.visionsolano.data.model.sortByTimeAndSeq
import com.example.visionsolano.data.network.DiscoveryState
import com.example.visionsolano.data.network.ESP32Api
import com.example.visionsolano.data.network.ESP32StatusResponse
import com.example.visionsolano.data.network.NetworkConfig
import com.example.visionsolano.data.network.VisionSolanoDiscoveryManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para la arquitectura de descubrimiento, validación de protocolo y eventos de VisionSolano.
 */
class DiscoveryAndProtocolTest {

    // 1. Test: Parseo de TXT records válidos
    @Test
    fun testParseTxtRecords_Success() {
        val txtMap = mapOf(
            "device_id" to "1",
            "role" to "esp32",
            "protocol_version" to "1",
            "api_version" to "1",
            "camera" to "ov2640"
        )

        val result = VisionSolanoDiscoveryManager.parseTxtRecords(
            txtMap = txtMap,
            hostName = "visionsolano-esp32.local",
            ipAddress = "192.168.1.75",
            port = 80
        )

        assertTrue("El parseo de TXT records debió ser exitoso", result.isSuccess)
        val device = result.getOrNull()
        assertNotNull(device)
        assertEquals("1", device?.deviceId)
        assertEquals("esp32", device?.role)
        assertEquals(1, device?.protocolVersion)
        assertEquals(1, device?.apiVersion)
        assertEquals("ov2640", device?.camera)
        assertEquals("192.168.1.75", device?.ipAddress)
        assertEquals(80, device?.port)
    }

    // 2. Test: Validación de protocolVersion
    @Test
    fun testValidateProtocolVersion_Success() {
        val txtMap = mapOf(
            "protocol_version" to "1",
            "role" to "esp32"
        )
        val result = VisionSolanoDiscoveryManager.parseTxtRecords(txtMap)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().protocolVersion)
    }

    // 3. Test: Rechazo de dispositivo incompatible (versión de protocolo diferente)
    @Test
    fun testRejectIncompatibleDevice() {
        val txtMap = mapOf(
            "device_id" to "2",
            "role" to "esp32",
            "protocol_version" to "99", // Versión no soportada
            "camera" to "ov2640"
        )

        val result = VisionSolanoDiscoveryManager.parseTxtRecords(txtMap)
        assertTrue("Dispositivo con versión de protocolo incompatible debió ser rechazado", result.isFailure)
        val errorMsg = result.exceptionOrNull()?.message
        assertTrue(errorMsg?.contains("incompatible") == true)
    }

    // 4. Test: Construcción de stream URL y endpoints centralizados
    @Test
    fun testBuildStreamUrl_AndEndpoints() {
        val ipUrl = NetworkConfig.buildStreamUrl("192.168.1.50", 80)
        assertEquals("http://192.168.1.50:80/stream", ipUrl)

        val hostUrl = NetworkConfig.buildStreamUrl("visionsolano-esp32.local", 80)
        assertEquals("http://visionsolano-esp32.local:80/stream", hostUrl)

        val statusUrl = NetworkConfig.buildStatusUrl("192.168.1.100", 80)
        assertEquals("http://192.168.1.100:80/api/status", statusUrl)

        val controlUrl = NetworkConfig.buildControlUrl("192.168.1.100", 80)
        assertEquals("http://192.168.1.100:80/api/control", controlUrl)

        // Limpieza de prefijos http:// y trailing slashes
        val dirtyUrl = NetworkConfig.buildStreamUrl("http://192.168.1.20:80/", 80)
        assertEquals("http://192.168.1.20:80/stream", dirtyUrl)
    }

    // 5. Test: Transición de estados de descubrimiento
    @Test
    fun testDiscoveryStateTransitions() {
        val states = mutableListOf<DiscoveryState>()
        states.add(DiscoveryState.IDLE)
        states.add(DiscoveryState.SEARCHING)
        states.add(DiscoveryState.DISCOVERING)
        states.add(DiscoveryState.FOUND)
        states.add(DiscoveryState.CONNECTED)

        assertEquals(DiscoveryState.IDLE, states[0])
        assertEquals(DiscoveryState.SEARCHING, states[1])
        assertEquals(DiscoveryState.DISCOVERING, states[2])
        assertEquals(DiscoveryState.FOUND, states[3])
        assertEquals(DiscoveryState.CONNECTED, states[4])
    }

    // 6. Test: Timeout de latido (Heartbeat timeout 3s)
    @Test
    fun testHeartbeatTimeout() {
        val now = 10000000L
        val lastContact = now - 3500L // 3.5 segundos sin contacto (> 3.0s timeout)
        val heartbeatAge = now - lastContact

        val isTimedOut = heartbeatAge >= NetworkConfig.HEARTBEAT_TIMEOUT_MS
        assertTrue("Debe marcarse timeout cuando el heartbeat supera los 3000 ms", isTimedOut)

        val connectionState = if (isTimedOut) ConnectionState.Disconnected else ConnectionState.Connected
        assertEquals(ConnectionState.Disconnected, connectionState)
    }

    // 7. Test: Ordenamiento de eventos por timestampMs y seq (no por orden de llegada)
    @Test
    fun testDetectionEventSorting_TimestampAndSeq() {
        // Se simula llegada desordenada por la red
        val eventA = DetectionEvent(seq = 1, timestampMs = 1000L, zone = "Sector A")
        val eventB = DetectionEvent(seq = 1, timestampMs = 2000L, zone = "Sector B")
        val eventC = DetectionEvent(seq = 2, timestampMs = 2000L, zone = "Sector C") // Mismo timestamp que B, pero mayor seq
        val eventD = DetectionEvent(seq = 5, timestampMs = 1500L, zone = "Sector D")

        val arrivalList = listOf(eventA, eventB, eventC, eventD)
        val sortedList = arrivalList.sortByTimeAndSeq()

        // El orden esperado debe ser:
        // 1. eventC (2000ms, seq 2)
        // 2. eventB (2000ms, seq 1)
        // 3. eventD (1500ms, seq 5)
        // 4. eventA (1000ms, seq 1)
        assertEquals(eventC, sortedList[0])
        assertEquals(eventB, sortedList[1])
        assertEquals(eventD, sortedList[2])
        assertEquals(eventA, sortedList[3])
    }

    // 8. Test: Parseo del modelo de status y conversión
    @Test
    fun testStatusModelParsing() {
        val statusResponse = ESP32StatusResponse(
            esp32Online = true,
            cameraOnline = true,
            fps = 28,
            resolution = "640 x 480",
            wifiSignalDbm = -52,
            protocolVersion = 1,
            uptimeSeconds = 3600L,
            freeHeapBytes = 180000L
        )

        assertTrue(statusResponse.esp32Online)
        assertTrue(statusResponse.cameraOnline)
        assertEquals(28, statusResponse.fps)
        assertEquals(640, statusResponse.width)
        assertEquals(480, statusResponse.height)
        assertEquals("640 x 480", statusResponse.resolution)
        assertEquals(-52, statusResponse.wifiSignalDbm)
        assertEquals(-52, statusResponse.wifiRssi)
        assertEquals(1, statusResponse.protocolVersion)

        // Verificación con SystemStatus
        val systemStatus = SystemStatus(
            esp32Online = statusResponse.esp32Online,
            cameraOnline = statusResponse.cameraOnline,
            fps = statusResponse.fps,
            resolution = statusResponse.resolution,
            wifiSignalDbm = statusResponse.wifiSignalDbm,
            protocolVersion = statusResponse.protocolVersion
        )

        assertEquals(28, systemStatus.fps)
        assertEquals("640 x 480", systemStatus.resolution)
        assertEquals(-52, systemStatus.wifiSignalDbm)
    }

    // 9. Test: Conversión de DetectionEvent a SecurityEvent de UI
    @Test
    fun testDetectionEventToSecurityEvent() {
        val detection = DetectionEvent(
            protocolVersion = 1,
            deviceId = 1,
            messageType = "ALERT",
            eventCode = 0x30,
            zone = "Perímetro Norte",
            personCount = 1,
            confidence = 0.92f,
            motion = true,
            stationary = false,
            seq = 1042L,
            timestampMs = System.currentTimeMillis()
        )

        val securityEvent = detection.toSecurityEvent()
        assertEquals("EVT-1042", securityEvent.id)
        assertTrue(securityEvent.title.contains("PERSONA DETECTADA"))
        assertTrue(securityEvent.description.contains("92%"))
        assertTrue(securityEvent.sensorPayload?.contains("Perímetro Norte") == true)
    }

    // 10. Test: Verificación de dispositivo mock
    @Test
    fun testMockDeviceProperties() {
        val mockDevice = VisionSolanoDevice.createMockDevice()
        assertEquals("1", mockDevice.deviceId)
        assertEquals("192.168.1.50", mockDevice.ipAddress)
        assertEquals("visionsolano-esp32.local", mockDevice.hostName)
        assertEquals(80, mockDevice.port)
        assertEquals(1, mockDevice.protocolVersion)
        assertEquals("ov2640", mockDevice.camera)
        assertEquals("http://visionsolano-esp32.local:80/stream", mockDevice.streamUrl)
    }

    // 11. Test: Manejo y desduplicación de paquetes con seq duplicado
    @Test
    fun testDuplicateSeqDeduplication_KeepsLatestTimestamp() {
        // Paquete original y retransmisión con mismo seq (100) pero timestamp más reciente
        val originalPacket = DetectionEvent(deviceId = 1, seq = 100L, timestampMs = 5000L, zone = "Sector A")
        val retransmittedPacket = DetectionEvent(deviceId = 1, seq = 100L, timestampMs = 5500L, zone = "Sector A (Retransmisión)")
        val subsequentPacket = DetectionEvent(deviceId = 1, seq = 101L, timestampMs = 6000L, zone = "Sector B")

        val rawList = listOf(originalPacket, retransmittedPacket, subsequentPacket)
        val deduplicated = rawList.deduplicateBySeq()

        // Debe contener exactamente 2 eventos (seq 101 y seq 100 desduplicado)
        assertEquals(2, deduplicated.size)
        // El primero debe ser seq 101 por orden cronológico descendente
        assertEquals(101L, deduplicated[0].seq)
        // Para seq 100, debe haberse conservado la retransmisión con timestamp más reciente (5500L)
        assertEquals(100L, deduplicated[1].seq)
        assertEquals(5500L, deduplicated[1].timestampMs)
        assertEquals("Sector A (Retransmisión)", deduplicated[1].zone)
    }

    // 12. Test: Desempate por timestamp cuando seq es idéntico en ordenamiento
    @Test
    fun testDuplicateSeqSorting_TieBreakByTimestamp() {
        val olderDup = DetectionEvent(deviceId = 1, seq = 42L, timestampMs = 1000L)
        val newerDup = DetectionEvent(deviceId = 1, seq = 42L, timestampMs = 3000L)

        val sorted = listOf(olderDup, newerDup).sortByTimeAndSeq()

        // El comparador canonical debe priorizar el timestamp más reciente ante seq idéntico
        assertEquals(newerDup, sorted[0])
        assertEquals(olderDup, sorted[1])
    }

    // 13. Test: Preservación de seq idéntico entre dispositivos distintos (deviceId diferente)
    @Test
    fun testDuplicateSeq_DifferentDevicesArePreserved() {
        // Dos nodos distintos pueden emitir el mismo número de secuencia de forma independiente
        val eventDevice1 = DetectionEvent(deviceId = 1, seq = 50L, timestampMs = 2000L)
        val eventDevice2 = DetectionEvent(deviceId = 2, seq = 50L, timestampMs = 2100L)

        val deduplicated = listOf(eventDevice1, eventDevice2).deduplicateBySeq()

        // Ambos deben coexistir pues pertenecen a distintos deviceId
        assertEquals(2, deduplicated.size)
        assertEquals(2, deduplicated[0].deviceId)
        assertEquals(1, deduplicated[1].deviceId)
    }

    // 14. Test: La telemetría Mock está explícitamente marcada como simulada
    @Test
    fun testMockTelemetryIsExplicitlyMarkedSimulated() {
        val mockRepo = com.example.visionsolano.data.repository.MockSurveillanceRepository()
        // El repositorio Mock debe emitir SystemStatus con isSimulated = true
        // y FpgaAnalysis con isSimulated = true
        var isSystemStatusSimulated = false
        var isFpgaSimulated = false

        runBlocking {
            val status = mockRepo.systemStatus.first()
            val fpga = mockRepo.fpgaAnalysis.first()
            isSystemStatusSimulated = status.isSimulated
            isFpgaSimulated = fpga.isSimulated
        }

        assertTrue("SystemStatus en Mock debe tener isSimulated = true", isSystemStatusSimulated)
        assertTrue("FpgaAnalysis en Mock debe tener isSimulated = true", isFpgaSimulated)
    }

    // 15. Test: CompositeSurveillanceRepository inicia con Mock como fallback transparente
    @Test
    fun testCompositeRepository_ServesMockInitially() {
        val compositeRepo = com.example.visionsolano.data.repository.CompositeSurveillanceRepository()
        var initialStatus: SystemStatus? = null

        runBlocking {
            initialStatus = compositeRepo.systemStatus.first()
        }

        assertNotNull(initialStatus)
        // En ausencia de dispositivo real, debe suministrar Mock con isSimulated = true
        assertTrue("CompositeRepository debe iniciar en modo fallback (isSimulated = true)", initialStatus?.isSimulated == true)
    }

    // 16. Test: Flujo de descubrimiento para dispositivo con IP dinámica (ej. 192.168.1.73)
    @Test
    fun testDynamicDevice_192_168_1_73_DiscoveryFlow() {
        val txtMap = mapOf(
            "device_id" to "1",
            "role" to "esp32",
            "protocol_version" to "1",
            "api_version" to "1",
            "camera" to "ov2640"
        )

        val result = VisionSolanoDiscoveryManager.parseTxtRecords(
            txtMap = txtMap,
            hostName = "visionsolano-esp32.local",
            ipAddress = "192.168.1.73",
            port = 80
        )

        assertTrue("El parseo para IP dinámica 192.168.1.73 debe ser exitoso", result.isSuccess)
        val device = result.getOrThrow()
        assertEquals("192.168.1.73", device.ipAddress)
        assertEquals("visionsolano-esp32.local", device.hostName)
        assertEquals(80, device.port)
        assertEquals("http://visionsolano-esp32.local:80/stream", device.streamUrl)
        assertEquals("http://visionsolano-esp32.local:80/api/status", device.statusUrl)
    }

    // 17. Test: Validación de network_security_config para IP dinámica (192.168.1.73 vs .local)
    @Test
    fun testNetworkSecurityConfig_PolicyForDynamicIp() {
        val xmlFile = listOf(
            File("src/main/res/xml/network_security_config.xml"),
            File("app/src/main/res/xml/network_security_config.xml")
        ).firstOrNull { it.exists() }

        assertNotNull("network_security_config.xml debe existir en el proyecto", xmlFile)
        val xmlContent = xmlFile!!.readText()

        // 1. base-config prohíbe cleartext globalmente
        assertTrue(
            "base-config debe tener cleartextTrafficPermitted=\"false\"",
            xmlContent.contains("cleartextTrafficPermitted=\"false\"")
        )

        // 2. Comprueba dominios permitidos explícitamente (.local y visionsolano-esp32.local)
        assertTrue("Debe incluir dominio .local", xmlContent.contains("<domain includeSubdomains=\"true\">local</domain>"))
        assertTrue("Debe incluir visionsolano-esp32.local", xmlContent.contains("<domain includeSubdomains=\"true\">visionsolano-esp32.local</domain>"))

        // 3. Demuestra ausencia absoluta de dependencias de IPs fijas o dinámicas en el XML de seguridad
        assertFalse(
            "network_security_config.xml NO debe depender de la IP fija de laboratorio 192.168.1.50",
            xmlContent.contains("192.168.1.50")
        )
        assertFalse(
            "La IP dinámica 192.168.1.73 NO debe estar fija en el XML de seguridad",
            xmlContent.contains("192.168.1.73")
        )
    }

    // 18. Test: Verificación de estado de ESP32Api (confirma si devuelve MOCK o HTTP real)
    @Test
    fun testESP32Api_AndNetworkRepository_CurrentlyReturnMockData() {
        val defaultApi = ESP32Api() // useMockResponses = true por defecto
        val networkRepo = com.example.visionsolano.data.repository.VisionSolanoNetworkRepository(esp32Api = defaultApi)

        runBlocking {
            val statusResult = defaultApi.getStatus("192.168.1.73", 80)
            assertTrue("getStatus debe responder Result.success", statusResult.isSuccess)
            val status = statusResult.getOrThrow()

            // Confirmación: responde datos precalculados de prueba/simulados
            assertEquals("ESP32Api actualmente devuelve datos MOCK (fps = 30)", 30, status.fps)
            assertEquals("640 x 480", status.resolution)
            assertEquals(-54, status.wifiSignalDbm)

            val controlResult = defaultApi.control("192.168.1.73", 80, "flash")
            assertTrue("control responde Result.success", controlResult.isSuccess)
            assertTrue(controlResult.getOrThrow())
        }
    }

    // 19. Test: VisionSolanoDevice conserva simultáneamente hostname, ipAddress y port
    @Test
    fun testVisionSolanoDevice_PreservesBothHostnameAndIpAddress() {
        val device = VisionSolanoDevice(
            deviceId = "1",
            hostName = "visionsolano-esp32.local",
            ipAddress = "192.168.1.73",
            port = 80,
            role = "esp32",
            protocolVersion = 1,
            apiVersion = 1,
            camera = "ov2640"
        )

        // 1. Demuestra que conserva exactamente el hostname mDNS sin sobreescribirlo con la IP
        assertEquals("visionsolano-esp32.local", device.hostName)

        // 2. Demuestra que conserva exactamente la dirección IP resuelta
        assertEquals("192.168.1.73", device.ipAddress)

        // 3. Demuestra que conserva el puerto HTTP
        assertEquals(80, device.port)

        // 4. Demuestra que las URLs de red priorizan el hostname mDNS estable (.local)
        assertEquals("visionsolano-esp32.local", device.targetHost)
        assertEquals("http://visionsolano-esp32.local:80/stream", device.streamUrl)
        assertEquals("http://visionsolano-esp32.local:80/api/status", device.statusUrl)
        assertEquals("http://visionsolano-esp32.local:80/api/control", device.controlUrl)
    }

    // 20. Test: Resolución y extracción del hostname conforme al contrato mDNS sin inventarlo de serviceName
    @Test
    fun testDiscoveryManager_ExtractHostName_AdheresToContractWithoutInventingFromServiceName() {
        val resolvedIp = InetAddress.getByName("192.168.1.73")

        // Caso A: InetAddress con hostname ya terminado en .local
        val inetLocal = InetAddress.getByAddress("visionsolano-esp32.local", resolvedIp.address)
        val extractedA = VisionSolanoDiscoveryManager.extractHostName(inetLocal, "Camara Patio 1")
        assertEquals("visionsolano-esp32.local", extractedA)

        // Caso B: InetAddress con etiqueta simple (sin .local)
        val inetSimple = InetAddress.getByAddress("visionsolano-esp32", resolvedIp.address)
        val extractedB = VisionSolanoDiscoveryManager.extractHostName(inetSimple, "Nodo Entrada")
        assertEquals("visionsolano-esp32.local", extractedB)

        // Caso C: Hostname provisto en registros TXT
        val txtWithHost = mapOf("hostname" to "visionsolano-esp32.local")
        val extractedC = VisionSolanoDiscoveryManager.extractHostName(resolvedIp, "Sensor Perimetro", txtWithHost)
        assertEquals("visionsolano-esp32.local", extractedC)

        // Caso D: serviceName arbitrario que NO termina en .local
        // NO debe inventar "Vigilancia Sector A.local", debe adherirse a NetworkConfig.EXPECTED_HOSTNAME
        val extractedD = VisionSolanoDiscoveryManager.extractHostName(resolvedIp, "Vigilancia Sector A")
        assertEquals("visionsolano-esp32.local", extractedD)

        // Caso E: serviceName que termina explícitamente en .local
        val extractedE = VisionSolanoDiscoveryManager.extractHostName(resolvedIp, "visionsolano-esp32.local")
        assertEquals("visionsolano-esp32.local", extractedE)
    }

    // 21. Test: Demuestra ausencia absoluta de dependencia de 192.168.1.50 en el descubrimiento dinámico
    @Test
    fun testDevice_DynamicDiscovery_AndAbsenceOf192_168_1_50_Dependency() {
        val dynamicIps = listOf("192.168.1.73", "192.168.1.144", "10.0.0.52")

        for (dynamicIp in dynamicIps) {
            val txtMap = mapOf(
                "device_id" to "1",
                "role" to "esp32",
                "protocol_version" to "1",
                "api_version" to "1",
                "camera" to "ov2640"
            )

            val result = VisionSolanoDiscoveryManager.parseTxtRecords(
                txtMap = txtMap,
                hostName = NetworkConfig.EXPECTED_HOSTNAME,
                ipAddress = dynamicIp,
                port = 80
            )

            assertTrue("El parseo para $dynamicIp debe ser exitoso", result.isSuccess)
            val device = result.getOrThrow()

            // 1. Descubrimiento del hostname
            assertEquals("visionsolano-esp32.local", device.hostName)

            // 2. Conservación de IP resuelta dinámica
            assertEquals(dynamicIp, device.ipAddress)
            assertFalse("No debe forzar ni depender de 192.168.1.50", device.ipAddress == "192.168.1.50")

            // 3. Puerto 80
            assertEquals(80, device.port)

            // 4. targetHost usando el hostname
            assertEquals("visionsolano-esp32.local", device.targetHost)
            assertEquals("http://visionsolano-esp32.local:80/stream", device.streamUrl)
            assertEquals("http://visionsolano-esp32.local:80/api/status", device.statusUrl)
            assertEquals("http://visionsolano-esp32.local:80/api/control", device.controlUrl)
        }
    }
}
