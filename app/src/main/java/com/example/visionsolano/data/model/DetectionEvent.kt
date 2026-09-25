package com.example.visionsolano.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Evento estructurado de detección de hardware conforme al protocolo de VisionSolano.
 *
 * Mantiene la separación estricta entre la telemetría binaria/sensor y el modelo
 * de presentación de la interfaz de usuario ([SecurityEvent]).
 */
data class DetectionEvent(
    val protocolVersion: Int = 1,
    val deviceId: Int = 1,
    val messageType: String = "DETECTION",
    val eventCode: Int = 0x20,
    val zone: String = "Sector A",
    val personCount: Int = 0,
    val confidence: Float = 0.95f,
    val motion: Boolean = true,
    val stationary: Boolean = false,
    val seq: Long = 1L,
    val timestampMs: Long = System.currentTimeMillis()
) {
    /**
     * Convierte el evento de detección de protocolo al modelo visual de UI [SecurityEvent].
     */
    fun toSecurityEvent(): SecurityEvent {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val formattedTime = timeFormat.format(Date(timestampMs))

        val priority = when {
            confidence > 0.85f && motion -> EventPriority.ALTA
            confidence > 0.60f || motion -> EventPriority.MEDIA
            else -> EventPriority.BAJA
        }

        val title = when {
            motion && personCount > 0 -> "PERSONA DETECTADA ($personCount)"
            motion -> "MOVIMIENTO EN $zone"
            stationary -> "OBJETO ESTACIONARIO EN $zone"
            else -> "DETECCIÓN SENSORIAL ($zone)"
        }

        val description = "Código: 0x${eventCode.toString(16).uppercase()} | Confianza: ${(confidence * 100).toInt()}%"
        val payload = "Seq: #$seq | Dispositivo ID: $deviceId | Zona: $zone | Protocolo: v$protocolVersion"

        return SecurityEvent(
            id = "EVT-$seq",
            title = title,
            description = description,
            time = formattedTime,
            source = EventSource.ESP_CAM,
            priority = priority,
            sensorPayload = payload
        )
    }

    companion object {
        /**
         * Comparador canónico: Ordena de forma descendente por timestampMs y luego por seq.
         * Garantiza orden cronológico estricto independientemente del orden de llegada de red.
         */
        val TIME_AND_SEQ_COMPARATOR: Comparator<DetectionEvent> =
            compareByDescending<DetectionEvent> { it.timestampMs }
                .thenByDescending { it.seq }
    }
}

/**
 * Ordena una colección de eventos de detección por timestampMs descendente y seq descendente.
 */
fun List<DetectionEvent>.sortByTimeAndSeq(): List<DetectionEvent> {
    return this.sortedWith(DetectionEvent.TIME_AND_SEQ_COMPARATOR)
}

/**
 * Desduplica una lista de eventos basándose en la clave única (deviceId, seq).
 * En caso de retransmisiones o paquetes duplicados con el mismo seq para un mismo dispositivo,
 * conserva la instancia con el timestamp más reciente (timestampMs mayor) y descarta los duplicados.
 */
fun List<DetectionEvent>.deduplicateBySeq(): List<DetectionEvent> {
    return this.groupBy { it.deviceId to it.seq }
        .map { (_, events) -> events.maxByOrNull { it.timestampMs } ?: events.first() }
        .sortedWith(DetectionEvent.TIME_AND_SEQ_COMPARATOR)
}

