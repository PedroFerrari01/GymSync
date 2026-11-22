package com.example.sistema.academia.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PixServiceTest {

    private final PixService pix = new PixService();

    @Test
    void crcBateComVetorConhecido() {
        // vetor padrão do CRC16-CCITT/FALSE
        assertEquals("29B1", PixService.crc16("123456789"));
        assertEquals("FFFF", PixService.crc16(""));
    }

    @Test
    void payloadTemCamposEValorComDuasCasas() {
        String p = pix.payload(42L, new BigDecimal("99.9"));
        assertTrue(p.startsWith("000201010211"));
        assertTrue(p.contains("5303986"));
        assertTrue(p.contains("540599.90"));
        assertTrue(p.contains("5802BR"));
        assertTrue(p.contains("5916GYMSYNC ACADEMIA"));
        assertTrue(p.contains("6009SAO PAULO"));
        assertTrue(p.contains("0504GS42"));
        assertTrue(p.contains("0014br.gov.bcb.pix"));
    }

    @Test
    void crcDoPayloadConfere() {
        String p = pix.payload(7L, new BigDecimal("150"));
        String corpo = p.substring(0, p.length() - 4);
        assertTrue(corpo.endsWith("6304"));
        assertEquals(PixService.crc16(corpo), p.substring(p.length() - 4));
        assertTrue(p.substring(p.length() - 4).matches("[0-9A-F]{4}"));
        assertTrue(p.contains("5406150.00"));
    }

    @Test
    void camposTemTamanhoEmDoisDigitos() {
        assertEquals("0502GS", PixService.campo("05", "GS"));
        assertEquals("0105abcde", PixService.campo("01", "abcde"));
    }
}
