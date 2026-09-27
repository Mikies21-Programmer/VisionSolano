> **Copia de referencia.** La versión oficial está en `docs/02_PROTOCOL.md`. Si hay diferencias, manda la de `docs/`.

# 02_PROTOCOL — Enlace ESP32-S3 ↔ Nexys 4

| | |
|---|---|
| Versión del protocolo | 1 |
| Estado | **BORRADOR** — pendiente de firma de los encargados |
| Redacta | Iván — encargado de Nexys 4 (FPGA) |
| Fecha | 25/09/2026 |
| Base | Plan técnico VisionSolano v2.0 / v2.1 (secciones 21, 22 y 28) |

Este documento es la **única fuente válida** del formato de trama entre ESP32 y Nexys. Si algo aquí contradice al plan técnico, manda este documento. La interfaz HTTP/JSON entre ESP32 y Android no se define aquí.

---

## 1. Transporte

| Parámetro | Valor |
|---|---|
| Protocolo | UDP sobre IPv4, red local del laboratorio |
| ESP32 → Nexys | Puerto destino **5000** |
| Nexys → ESP32 | Puerto destino **5001** |
| IP de la Nexys | Estática, asignada por el encargado FPGA dentro de la red del laboratorio (se registra en la hoja maestra). La ESP32 la usa como destino. |
| IP de la ESP32 | Dinámica (DHCP/mDNS). **La Nexys no necesita conocerla:** responde a la IP de origen del último paquete válido recibido. |
| Tamaño | Cada datagrama UDP contiene **exactamente una trama de 20 bytes**. Cualquier otro tamaño se descarta. |

## 2. Reglas generales

- **Orden de bytes: big-endian** (orden de red) en todos los campos de más de un byte. El byte más significativo va primero.
- **CRC:** CRC-16/CCITT-FALSE — polinomio `0x1021`, valor inicial `0xFFFF`, sin reflexión de entrada ni salida, XOR final `0x0000`. Se calcula sobre los bytes 0–17 y se guarda en los bytes 18–19 (big-endian).
- **Verificación del CRC:** para la cadena ASCII `"123456789"` el resultado debe ser `0x29B1`.

## 3. Formato de trama (20 bytes)

| Byte(s) | Campo | Tipo | Descripción |
|---|---|---|---|
| 0 | `version` | uint8 | Siempre `1` en esta versión |
| 1 | `device_id` | uint8 | Emisor: `1` = ESP32, `2` = Nexys |
| 2 | `message_type` | uint8 | `0` = HEARTBEAT, `1` = EVENT, `2` = STATE |
| 3 | `event_code` | uint8 | Ver sección 4 |
| 4 | `zone` | uint8 | Zona de vigilancia. Inicial: `1` |
| 5 | `person_count` | uint8 | Personas detectadas |
| 6 | `confidence` | uint8 | 0–100 |
| 7 | `flags` | uint8 | bit0 = movimiento, bit1 = persona quieta, bits 2–7 = 0 |
| 8–11 | `seq` | uint32 | Ver sección 5 |
| 12–15 | `timestamp_ms` | uint32 | Milisegundos desde el arranque del emisor |
| 16–17 | `reserved` | uint16 | Siempre `0` |
| 18–19 | `crc16` | uint16 | CRC de los bytes 0–17 |

## 4. Códigos

### 4.1 Eventos de la ESP32 (`device_id = 1`)

| `event_code` | Nombre | Uso en la FSM |
|---|---|---|
| 0 | HEARTBEAT | Mantener enlace vivo (con `message_type = 0`) |
| 1 | NO_MOTION | Candidato a SEGURO |
| 2 | PERSON_DETECTED | Evaluar ADVERTENCIA / PELIGRO |
| 3 | MOTION_DETECTED | Escalar según persistencia |
| 4 | PERSON_STATIONARY | Regla de persona quieta |
| 5 | CAMERA_ERROR | ADVERTENCIA + registrar falla |
| 6 | NETWORK_ERROR | Diagnóstico |

### 4.2 Estados de la Nexys (`device_id = 2`, `message_type = 2`)

| `event_code` | Estado | Salidas |
|---|---|---|
| 10 | SEGURO | Luz OFF, sirena OFF |
| 11 | ADVERTENCIA | Luz ON, sirena OFF |
| 12 | PELIGRO | Luz ON, sirena ON |
| 13 | LINK_LOST | Luz ON, sirena OFF (política segura propuesta) |

En una trama STATE de la Nexys:
- `zone`, `person_count`, `confidence` y `flags` repiten los valores del último evento válido que provocó la decisión.
- `seq` = **seq del último paquete válido aceptado de la ESP32** (sirve como acuse de recibo y como `last_seq` para Android).
- `timestamp_ms` = milisegundos desde el arranque de la Nexys.

## 5. Número de secuencia (`seq`)

**En la ESP32:**
- Empieza en `0` al arrancar y aumenta en 1 con **cada trama enviada** (eventos y heartbeats comparten el mismo contador).

**En la Nexys**, al recibir una trama con CRC correcto:
- `seq` mayor que el último aceptado → **se acepta**. Si el salto es mayor que 1, se incrementa el contador de paquetes perdidos.
- `seq` igual al último aceptado → **duplicado**, se descarta y se cuenta.
- `seq` menor que el último aceptado → se descarta, **excepto** si `seq = 0` o si la Nexys está en LINK_LOST. En esos casos se asume que la ESP32 se reinició y se acepta como nuevo punto de partida.

## 6. Tiempos

| Parámetro | Valor | Regla |
|---|---|---|
| Heartbeat ESP32 | 1 s | Si no hubo evento en el último segundo, la ESP32 envía un HEARTBEAT |
| Evento | Inmediato | La ESP32 envía EVENT en cuanto cambia lo que detecta |
| Timeout de enlace | 3 s | Cualquier trama válida reinicia el watchdog. Si pasan 3 s sin tramas válidas → LINK_LOST |
| Estado de la Nexys | Al cambiar + cada 1 s | La Nexys envía STATE cada vez que cambia de estado y, además, una vez por segundo |
| Arranque / reset de la Nexys | — | Salidas apagadas, estado SEGURO. Si en 3 s no llega nada → LINK_LOST |

## 7. Validación en el receptor (en este orden)

1. Longitud del datagrama = 20 bytes.
2. `version = 1`.
3. `device_id` es el esperado (la Nexys solo acepta `1`; la ESP32 solo acepta `2`).
4. CRC correcto.
5. `message_type` y `event_code` dentro de los valores definidos.
6. Regla de `seq` (sección 5).

Una trama que falla cualquier paso se descarta **sin cambiar el estado** y se incrementa el contador correspondiente: longitud, versión, CRC, código inválido, duplicado o salto.

## 8. Ejemplos (verificados)

**EVENT — persona detectada, confianza 87, con movimiento, seq 1532:**
```
01 01 01 02 01 01 57 01 00 00 05 FC 00 41 72 6C 00 00 8B F6
```

**HEARTBEAT — seq 1533:**
```
01 01 00 00 01 00 00 00 00 00 05 FD 00 41 76 54 00 00 0F EE
```

**STATE de la Nexys — ADVERTENCIA, acusa seq 1532:**
```
01 02 02 0B 01 01 57 01 00 00 05 FC 00 00 CB 20 00 00 0A 77
```

## 9. Relación con `/api/status` (Android)

Se propone que la ESP32 funcione como puerta de enlace: guarda la última trama STATE recibida y la publica así:

| Campo JSON | Origen |
|---|---|
| `fpga_state` | `event_code` 10–13 → `"SAFE"`, `"WARNING"`, `"DANGER"`, `"LINK_LOST"` |
| `threat_level` | 0, 1, 2 (LINK_LOST = −1) |
| `last_event_code` | Último `event_code` enviado por la ESP32 |
| `last_seq` | Bytes 8–11 de la trama STATE |

Si la ESP32 no recibe STATE en 3 s, publica `fpga_state = "LINK_LOST"`.

## 10. Decisiones por confirmar

- [ ] Puertos 5000 / 5001
- [ ] IP estática de la Nexys: `____________`
- [ ] Política de salidas en LINK_LOST (propuesta: luz ON, sirena OFF)
- [ ] ESP32 como puerta de enlace hacia Android (sección 9)
- [ ] Contador `seq` compartido entre eventos y heartbeats

## 11. Firmas

| Área | Encargado | Fecha | Aprobado |
|---|---|---|---|
| Nexys 4 (FPGA) | Iván | | [ ] |
| ESP32-S3 + cámara | Jaqueline | | [ ] |
| Android Studio y Obsidian | Miguel | | [ ] |
| Revisión de integración ("semáforo") | Abinadab | | [ ] |

La revisión de integración verifica este documento contra los de las demás áreas y declara si el sistema está listo para unirse o qué debe corregirse. Cualquier cambio posterior de un área debe registrarse en el historial y mantenerse compatible con este protocolo.

## Historial

| Versión | Fecha | Cambio |
|---|---|---|
| 1.0-borrador | 25/09/2026 | Primera redacción a partir del plan técnico v2.0/v2.1 |
