# VISION SOLANO — PLAN TÉCNICO DE INTEGRACIÓN DEL SISTEMA DE VIGILANCIA



**Nexys 4 + ESP32-S3/OV2640 + Aplicación Android**

**Revisión 2.1 — Estado actual de integración — 24 de septiembre de 2026**



> Versión online de consulta del documento base actualizado. Su objetivo es que los tres equipos tengan una referencia común antes de iniciar la integración física.



## 0. Estado actual de integración



VisionSolano se divide en tres bloques: ESP32-S3 + OV2640 para captura/detección/eventos candidatos; Nexys 4 + Artix-7 para validación, FSM, decisión y actuadores; Android para supervisión, visualización, histórico y configuración.



### Android ya está preparado



- Descubrimiento mDNS/NSD.

- Servicio _visionsolano._tcp.

- Hostname preferido: visionsolano-esp32.local.

- Puerto HTTP: 80.

- Endpoints previstos: /stream, /api/status, /api/control.

- ACCESS_LOCAL_NETWORK para el target actual.

- VisionSolanoDevice conserva hostname + IP resuelta + puerto.

- targetHost prioriza el hostname mDNS para no depender de una IP DHCP fija.

- Heartbeat preparado a 1 s.

- Timeout preparado a 3 s.

- CompositeSurveillanceRepository con Mock como fallback.

- Telemetría Mock marcada como simulada.

- DetectionEvent con ordenamiento por timestamp + seq y deduplicación por (deviceId, seq).

- 21 pruebas unitarias superadas en la etapa documentada.

- assembleDebug validado.



### Android todavía no tiene



- HTTP real de /api/status.

- HTTP real de /api/control.

- Decodificador/cliente real del stream MJPEG.

- Parser final de la trama binaria Nexys.

- Implementación final del CRC de hardware.

- Integración física con ESP32-S3.

- Integración física con Nexys 4.



### Bloqueo actual



Para continuar con integración real necesitamos los artefactos reales de los equipos ESP32-S3/OV2640 y Nexys 4/Vivado. No se deben inventar IP, puertos, trama, CRC, estados o transportes todavía no confirmados.



Repositorio Android: https://github.com/Mikies21-Programmer/VisionSolano



Baseline verificado: 28b2f0f7148721235e4a1848585732372736592e



Mensaje: Android: preparar descubrimiento mDNS y arquitectura de red



## 1. Propósito y alcance



Definir una arquitectura común para que tres equipos desarrollen VisionSolano sin depender de supuestos distintos. El sistema se divide en aplicación Android, nodo de visión ESP32-S3 con cámara OV2640 y cerebro de decisiones en una Nexys 4 programada con Vivado.



## 2. Responsabilidades



| Bloque | Función primaria | No debe hacer |

|---|---|---|

| ESP32-S3 + OV2640 | Captura de video, detección de persona/movimiento, eventos candidatos y streaming | Tomar la decisión final de sirena/alarma |

| Nexys 4 + Artix-7 | Validar eventos, ejecutar FSM, reglas, actuadores y comunicar telemetría | Procesar video crudo de cámara en primera fase |

| Android | Mostrar video, estado, alertas, histórico y configuración | Ser el cerebro de seguridad |



Regla: ESP32 observa → Nexys decide → Android supervisa.



## 3. Flujo de datos objetivo



Video: OV2640 → ESP32-S3 → Wi-Fi/LAN → Android.



Eventos: ESP32-S3 → LAN → Nexys 4 → FSM → estado/decisión → ruta de retorno → Android.



Ruta de video prevista: MJPEG sobre HTTP, /stream, puerto 80. Resolución inicial propuesta por el plan: 320x240; evaluar 640x480 después de medir FPS, latencia y carga.



## 4. Datos que Android ya entrega



| Elemento | Valor preparado |

|---|---|

| Service Type | _visionsolano._tcp |

| Hostname | visionsolano-esp32.local |

| HTTP port | 80 |

| Stream | /stream |

| Status | /api/status |

| Control | /api/control |

| protocol_version | 1 |

| api_version | 1 |

| role | esp32 |

| camera | ov2640 |

| Heartbeat | 1 s |

| Timeout | 3 s |



Los valores históricos 192.168.1.50 y 192.168.1.100 son ejemplos de laboratorio, no IP obligatorias. La IP real de ESP32 se descubre por mDNS/NSD.



Registros TXT esperados: device_id, role, protocol_version, api_version y camera. hostname no debe considerarse obligatorio; Android lo obtiene mediante DNS-SD/mDNS y lo mantiene como dato del dispositivo.



## 5. Lo que necesitamos del equipo ESP32-S3/OV2640



### Hardware



- Modelo exacto de la placa ESP32-S3.

- Modelo exacto de cámara/módulo.

- GPIO de OV2640.

- Configuración de PSRAM.

- Framework y versión de SDK.

- Commit o snapshot del firmware.



### mDNS



- service type real.

- service instance name.

- hostname mDNS real.

- puerto real.

- TXT real.

- evidencia del anuncio/descubrimiento.



### HTTP



GET /api/status: entregar un JSON real del dispositivo con los campos que correspondan al contrato común: protocol_version, esp32_ip, wifi_rssi, camera_online, fps, width, height, uptime_seconds y memoria disponible si se expone.



GET /stream: entregar URL real, Content-Type, resolución, FPS medido y evidencia de funcionamiento.



GET /capture: entregar formato y Content-Type.



POST /api/control: entregar comandos soportados, JSON de entrada, respuesta y códigos de error.



### Detección



Entregar modelo, método de detección, FPS real de inferencia, person_count, confidence, zona, persistencia, regla de generación de eventos y mediciones de falsos positivos/negativos.



### Evento ESP32 → Nexys



Android tiene preparado el modelo conceptual: protocolVersion, deviceId, messageType, eventCode, zone, personCount, confidence, motion, stationary, seq y timestampMs.



Entregar un ejemplo REAL con bytes hexadecimales, campos decodificados, transporte, IP destino, puerto, frecuencia y significado de cada byte.



## 6. Lo que necesitamos del equipo Nexys 4 / Vivado



### Hardware y proyecto



- Part number exacto y variante de placa.

- XDC.

- Frecuencia de reloj.

- Versión de Vivado.

- Proyecto Vivado.

- Diagrama de bloques.

- Bitstream de prueba si aplica.



### Ethernet



Confirmar si Ethernet estará realmente activa; arquitectura usada; IP real; máscara/gateway cuando corresponda; puerto UDP/TCP real; mecanismo de recepción/emisión; evidencia de link; ejemplo de paquete real.



Android no asumirá una IP o puerto de Nexys hasta recibir esta información.



### FSM



Entregar estados reales, tabla de transición, condiciones, umbrales, persistencia, tiempos, timeout, estado después de reset, política de salida segura y actuadores por estado.



Estados base: SEGURO, ADVERTENCIA y PELIGRO. Los umbrales finales deben quedar respaldados por pruebas reales y versionados.



## 7. Protocolo binario — pendiente de congelación



Este punto es crítico. La documentación histórica contiene propuestas de formato de trama incompatibles entre sí. No se debe programar el parser definitivo hasta que 02_PROTOCOL contenga una sola versión oficial.



La versión propuesta en el plan contiene campos de versión, device_id, tipo, event_code, zone, person_count, confidence, flags, seq, timestamp y CRC16. También existen notas históricas de una estructura genérica MAGIC / VER / TYPE / SEQ / LEN / PAYLOAD / CRC.



El equipo conjunto debe congelar: estructura, tamaño, endianess, significado de cada byte, transporte, CRC, inicialización/polinomio, reglas de seq, heartbeat y códigos de retorno FPGA.



## 8. Heartbeat y timeout



Propuesta inicial: ESP32 envía heartbeat cada 1 s; Nexys considera enlace perdido después de 3 s sin información válida; Android usa 1 s como intervalo y 3 s como timeout inicial; Android nunca controla directamente la sirena.



## 9. Qué NO debe asumir ningún equipo



- 192.168.1.50 como IP fija de ESP32.

- 192.168.1.100 como IP fija de Nexys.

- TCP Raw como contrato definitivo.

- UDP como contrato definitivo hasta aprobar 02_PROTOCOL.

- UART como enlace obligatorio.

- Cualquier baudrate no aprobado.

- CRC de una documentación antigua.

- 640x480 como resolución final.

- 30 FPS como medición física.

- Valores de amenaza de los mocks.

- Eventos generados por el repositorio Mock.



## 10. Estado Android en GitHub



Repositorio: https://github.com/Mikies21-Programmer/VisionSolano



Rama: main



Commit baseline: 28b2f0f7148721235e4a1848585732372736592e



Archivos de referencia del bloque Android: NetworkConfig.kt, VisionSolanoDiscoveryManager.kt, ESP32Api.kt, VisionSolanoDevice.kt, VisionSolanoNetworkRepository.kt, CompositeSurveillanceRepository.kt, DetectionEvent.kt, SystemStatus.kt, FpgaAnalysis.kt y SurveillanceViewModel.kt.



ESP32Api todavía opera con useMockResponses = true. La implementación HTTP real debe comenzar solo después de recibir el contrato y ejemplos reales del firmware.



## 11. Próximo orden de trabajo



### ESP32

1. Encender hardware.

2. Conectar a LAN.

3. Confirmar mDNS.

4. Confirmar /api/status.

5. Confirmar /stream.

6. Entregar JSON real.

7. Entregar evento real.



### Nexys

1. Confirmar placa/part number.

2. Confirmar Vivado/XDC/reloj.

3. Confirmar Ethernet.

4. Confirmar IP/puerto.

5. Confirmar FSM.

6. Confirmar trama.

7. Confirmar CRC.

8. Entregar paquetes reales de prueba.



### Protocolo conjunto

Actualizar primero 02_PROTOCOL. Después cada equipo modifica el código bajo el contrato común.



### Integración física

ESP32 → Nexys → retorno → Android, y en paralelo ESP32 → /stream → Android.



## 12. Matriz T01–T12



| ID | Prueba | Resultado esperado |

|---|---|---|

| T01 | ESP32 entra a Wi-Fi | IP válida + RSSI |

| T02 | Android abre /stream | Video continuo |

| T03 | ESP32 envía heartbeat | Nexys detecta enlace |

| T04 | CRC incorrecto | Nexys rechaza |

| T05 | seq repetido | No duplica evento |

| T06 | PERSON_DETECTED | FSM cambia según reglas |

| T07 | MOVEMENT persistente | Escala según regla |

| T08 | Corte de Wi-Fi | Timeout y política segura |

| T09 | Android pierde conexión | UI muestra SIN CONEXIÓN |

| T10 | Reinicio ESP32 | Recupera stream/heartbeat/seq |

| T11 | Reinicio Nexys | Salidas seguras |

| T12 | Carga sostenida | FPS, latencia, pérdida y memoria |



## 13. Criterio de integración completa



VisionSolano se considera integrado cuando un evento real del ESP32 viaje a la Nexys, sea validado, produzca una transición de la FSM, la decisión/estado regrese mediante la arquitectura definida y Android lo muestre junto con el video correspondiente. Android no es la fuente de verdad de la alarma.



## 14. Bloqueo actual de integración



El desarrollo Android independiente puede continuar. La siguiente implementación real de red debe esperar a recibir los artefactos de ESP32 y Nexys.



### Entregables ESP32 obligatorios

- firmware;

- mDNS;

- JSON real de /api/status;

- stream real;

- endpoints reales;

- evento real;

- formato de evento;

- IP/puerto observados;

- mediciones reales.



### Entregables Nexys obligatorios

- proyecto Vivado;

- XDC;

- reloj;

- red;

- IP/puerto;

- FSM;

- estados;

- timeout;

- protocolo;

- CRC;

- paquete real de prueba;

- paquete real de retorno.



### Entregable conjunto

- decisión formal de arquitectura de retorno FPGA → Android;

- única versión oficial de 02_PROTOCOL.



## 15. Regla de cambios



Si se modifica IP, puerto, endpoint, campo, tamaño, estado, código, CRC, transporte o versión, primero se actualiza 02_PROTOCOL y después el código de cada equipo.



## 16. Fuente de implementación Android



Repositorio oficial: https://github.com/Mikies21-Programmer/VisionSolano



Baseline: 28b2f0f7148721235e4a1848585732372736592e



Este baseline corresponde exclusivamente al bloque Android. El firmware ESP32 y el RTL/Vivado de Nexys deberán ser aportados por los equipos correspondientes.



## 17. Registro de revisión 2.1



Se actualizó el documento para reflejar el estado real del repositorio Android; se eliminó la IP fija como requisito de descubrimiento; se distinguieron Mock y hardware real; se detallaron los datos que Android ya proporciona; se detallaron los datos requeridos de ESP32 y Nexys; y se estableció explícitamente el bloqueo de integración hasta recibir los artefactos físicos/firmware y congelar 02_PROTOCOL.



**Estado de la etapa:** Android preparado para recibir hardware → esperando datos reales de ESP32 y Nexys.