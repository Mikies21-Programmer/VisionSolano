`timescale 1ns / 1ps
// Solo para probar "lampara" en la Nexys 4. No se entrega.
module top_prueba (
    input  wire CLK100MHZ,
    input  wire CPU_RESETN,
    input  wire SW0,
    output wire JA1,
    output wire LED0
);
    wire lamp;

    lampara u_lampara (
        .clk      (CLK100MHZ),
        .reset    (~CPU_RESETN),  // el botón de reset es activo en bajo
        .encender (SW0),          // SW0 simula la orden de la Nexys
        .lamp_out (lamp)
    );

    assign JA1  = lamp;  // al IN del relé
    assign LED0 = lamp;  // para verlo en la placa
endmodule