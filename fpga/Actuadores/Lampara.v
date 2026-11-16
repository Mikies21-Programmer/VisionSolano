`timescale 1ns / 1ps
// ============================================================
// Módulo: lampara (versión 1)
// Proyecto: VisionSolano, actuadores
//
//
//
// Hardware: JA1 (C17) -> 1k -> base 2N2222 (emisor a GND, colector a IN
// del módulo de relé). El módulo de relé se activa con nivel bajo y el
// transistor invierte la señal, así que lamp_out = 1 enciende la lámpara.
//
//
//
//
// Qué hace:
//   Enciende la lámpara (reflector de 120 V por medio de un relé)
//   mientras "encender" esté en 1. Cuando "encender" vuelve a 0,
//   la lámpara sigue encendida 3 s más y luego se apaga.
//
// Cómo integrarlo (copiar en el top):
//
//   lampara u_lampara (
//       .clk      (clk_100mhz),  // reloj de 100 MHz de la Nexys
//       .reset    (rst),         // reset activo en ALTO
//       .encender (tu_senal),    // 1 = prender la lámpara
//       .lamp_out (JA1)          // pin JA1 (B13) -> IN del relé
//   );
//
// Pines: usa solo JA1 del Pmod JA. No usa los JB.
// En el .xdc agregar:
//   set_property -dict { PACKAGE_PIN B13 IOSTANDARD LVCMOS33 } [get_ports JA1]
// ============================================================
module lampara #(
    parameter TIEMPO_EXTRA = 300_000_000  // 3 s con reloj de 100 MHz
)(
    input  wire clk,
    input  wire reset,
    input  wire encender,
    output reg  lamp_out
);

    // Sincronizador: pasa "encender" por dos registros para que
    // no cause errores si viene de un switch o de otro reloj.
    reg enc_s1, enc_s2;
    always @(posedge clk) begin
        enc_s1 <= encender;
        enc_s2 <= enc_s1;
    end

    // Contador del tiempo extra después de quitar la orden
    reg [31:0] contador;

    always @(posedge clk) begin
        if (reset) begin
            contador <= 0;
            lamp_out <= 1'b0;
        end else if (enc_s2) begin
            contador <= TIEMPO_EXTRA;   // mientras haya orden, recarga
            lamp_out <= 1'b1;
        end else if (contador != 0) begin
            contador <= contador - 1;   // cuenta los 3 s 
            lamp_out <= 1'b1;
        end else begin
            lamp_out <= 1'b0;           // se acabó el tiempo
        end
    end

endmodule