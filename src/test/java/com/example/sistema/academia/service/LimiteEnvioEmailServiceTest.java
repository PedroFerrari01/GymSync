package com.example.sistema.academia.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimiteEnvioEmailServiceTest {

    /** Relógio que anda só quando o teste manda. */
    static class Relogio extends Clock {
        Instant agora = Instant.parse("2026-11-11T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId z) { return this; }
        @Override public Instant instant() { return agora; }
    }

    @Test
    void permiteTresPorHoraEBarraOQuarto() {
        LimiteEnvioEmailService s = new LimiteEnvioEmailService(3, Duration.ofHours(1), new Relogio());
        assertTrue(s.permitir("PROFESSOR:1"));
        assertTrue(s.permitir("PROFESSOR:1"));
        assertTrue(s.permitir("PROFESSOR:1"));
        assertFalse(s.permitir("PROFESSOR:1"));
    }

    @Test
    void cadaUsuarioTemOSeuLimite() {
        LimiteEnvioEmailService s = new LimiteEnvioEmailService(1, Duration.ofHours(1), new Relogio());
        assertTrue(s.permitir("ALUNO:1"));
        assertFalse(s.permitir("ALUNO:1"));
        assertTrue(s.permitir("ALUNO:2"));
    }

    @Test
    void voltaALiberarDepoisDeUmaHora() {
        Relogio r = new Relogio();
        LimiteEnvioEmailService s = new LimiteEnvioEmailService(1, Duration.ofHours(1), r);
        assertTrue(s.permitir("ALUNO:1"));
        r.agora = r.agora.plus(Duration.ofMinutes(30));
        assertFalse(s.permitir("ALUNO:1"));
        r.agora = r.agora.plus(Duration.ofMinutes(31));
        assertTrue(s.permitir("ALUNO:1"));
    }
}
