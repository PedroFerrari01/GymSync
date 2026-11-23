package com.example.sistema.academia.controller;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlanoValidacaoTest {

    @Test
    void planoNormalEhAceito() {
        assertNull(AcademiaController.erroPlano(1, new BigDecimal("99.90")));
        assertNull(AcademiaController.erroPlano(12, new BigDecimal("1078.80")));
        assertNull(AcademiaController.erroPlano(60, new BigDecimal("100000")));
    }

    @Test
    void duracaoForaDeUmASessentaMesesEhRecusada() {
        assertNotNull(AcademiaController.erroPlano(-1, new BigDecimal("50")));
        assertNotNull(AcademiaController.erroPlano(0, new BigDecimal("50")));
        assertNotNull(AcademiaController.erroPlano(61, new BigDecimal("50")));
        assertNotNull(AcademiaController.erroPlano(null, new BigDecimal("50")));
    }

    @Test
    void valorNegativoZeroOuAbsurdoEhRecusado() {
        assertNotNull(AcademiaController.erroPlano(3, new BigDecimal("-50")));
        assertNotNull(AcademiaController.erroPlano(3, BigDecimal.ZERO));
        assertNotNull(AcademiaController.erroPlano(3, new BigDecimal("0.99")));
        assertNotNull(AcademiaController.erroPlano(3, new BigDecimal("99999999999999")));
        assertNotNull(AcademiaController.erroPlano(3, new BigDecimal("100000.01")));
        assertNotNull(AcademiaController.erroPlano(3, null));
    }
}
