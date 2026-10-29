package com.example.sistema.academia.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SenhaServiceTest {

    private final SenhaService desenvolvimento = new SenhaService(false);
    private final SenhaService fortes = new SenhaService(true);

    @Test
    void desenvolvimentoContinuaComoAntes() {
        assertEquals("123", desenvolvimento.provisoria());
        assertEquals(4, desenvolvimento.tamanhoMinimo());
        assertNull(desenvolvimento.validarNova("1234"));
        assertNotNull(desenvolvimento.validarNova("123"));
        assertTrue(desenvolvimento.aceitaSenhaVaziaComo123());
    }

    @Test
    void senhasFortesExigemOitoCaracteres() {
        assertEquals(8, fortes.tamanhoMinimo());
        assertNotNull(fortes.validarNova("1234567"));
        assertNull(fortes.validarNova("12345678"));
        assertFalse(fortes.aceitaSenhaVaziaComo123());
    }

    @Test
    void senhaProvisoriaForteEAleatoria() {
        String a = fortes.provisoria();
        String b = fortes.provisoria();
        assertEquals(10, a.length());
        assertNull(fortes.validarNova(a));
        assertNotEquals(a, b);
    }
}
