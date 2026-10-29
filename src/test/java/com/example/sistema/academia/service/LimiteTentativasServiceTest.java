package com.example.sistema.academia.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimiteTentativasServiceTest {

    /** Relógio que só anda quando o teste manda. */
    private static class RelogioManual extends Clock {
        Instant agora = Instant.parse("2026-10-29T12:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
        void avancar(Duration d) { agora = agora.plus(d); }
    }

    private RelogioManual relogio;
    private LimiteTentativasService limite;

    @BeforeEach
    void preparar() {
        relogio = new RelogioManual();
        limite = new LimiteTentativasService(Duration.ofMinutes(15), relogio);
    }

    private void errar(String chave, int vezes) {
        for (int i = 0; i < vezes; i++) limite.registrarFalha(chave, 5);
    }

    @Test
    void quatroErrosAindaNaoBloqueiam() {
        errar("conta:ALUNO:ana", 4);
        assertFalse(limite.bloqueada("conta:ALUNO:ana"));
    }

    @Test
    void quintoErroBloqueiaPorQuinzeMinutos() {
        errar("conta:ALUNO:ana", 5);
        assertTrue(limite.bloqueada("conta:ALUNO:ana"));
        assertEquals(15, limite.minutosRestantes("conta:ALUNO:ana"));
    }

    @Test
    void bloqueioDeUmaContaNaoAfetaOutra() {
        errar("conta:ALUNO:ana", 5);
        assertFalse(limite.bloqueada("conta:ALUNO:bia"));
    }

    @Test
    void depoisDoTempoPodeTentarDeNovo() {
        errar("conta:ALUNO:ana", 5);
        relogio.avancar(Duration.ofMinutes(15));
        assertFalse(limite.bloqueada("conta:ALUNO:ana"));
        // e a contagem recomeça do zero
        errar("conta:ALUNO:ana", 4);
        assertFalse(limite.bloqueada("conta:ALUNO:ana"));
    }

    @Test
    void acertarASenhaZeraAContagem() {
        errar("conta:ALUNO:ana", 4);
        limite.registrarSucesso("conta:ALUNO:ana");
        errar("conta:ALUNO:ana", 4);
        assertFalse(limite.bloqueada("conta:ALUNO:ana"));
    }

    @Test
    void errosAntigosNaoSomamComOsNovos() {
        errar("conta:ALUNO:ana", 4);
        relogio.avancar(Duration.ofMinutes(16));
        errar("conta:ALUNO:ana", 1);
        assertFalse(limite.bloqueada("conta:ALUNO:ana"));
    }

    @Test
    void cadaChaveTemSeuProprioLimite() {
        for (int i = 0; i < 29; i++) limite.registrarFalha("ip:10.0.0.1", 30);
        assertFalse(limite.bloqueada("ip:10.0.0.1"));
        limite.registrarFalha("ip:10.0.0.1", 30);
        assertTrue(limite.bloqueada("ip:10.0.0.1"));
    }
}
