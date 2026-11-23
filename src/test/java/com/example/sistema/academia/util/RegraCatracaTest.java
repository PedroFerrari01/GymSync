package com.example.sistema.academia.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegraCatracaTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 23, 10, 0, 0);

    @Test
    void leituraLogoDepoisDaEntradaEhRepetida() {
        assertTrue(RegraCatraca.entradaRepetida(AGORA.minusSeconds(5), AGORA));
        assertTrue(RegraCatraca.entradaRepetida(AGORA.minusSeconds(59), AGORA));
    }

    @Test
    void depoisDeUmMinutoEhNovaEntrada() {
        assertFalse(RegraCatraca.entradaRepetida(AGORA.minusSeconds(60), AGORA));
        assertFalse(RegraCatraca.entradaRepetida(AGORA.minusHours(3), AGORA));
    }

    @Test
    void semEntradaAnteriorOuComRelogioAtrasadoNaoEhRepetida() {
        assertFalse(RegraCatraca.entradaRepetida(null, AGORA));
        assertFalse(RegraCatraca.entradaRepetida(AGORA.plusSeconds(10), AGORA));
    }
}
