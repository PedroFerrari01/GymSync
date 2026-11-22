package com.example.sistema.academia.service;

import com.example.sistema.academia.model.AulaColetiva;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GradeSemanalTest {

    private static AulaColetiva aula(String nome, DayOfWeek dia, int h) {
        AulaColetiva a = new AulaColetiva();
        a.setNome(nome);
        a.setDiaSemana(dia);
        a.setHorario(LocalTime.of(h, 0));
        a.setVagas(10);
        return a;
    }

    @Test
    void segundaDaSemana() {
        assertEquals(LocalDate.of(2026, 11, 23), GradeSemanal.segundaDa(LocalDate.of(2026, 11, 26)));
        assertEquals(LocalDate.of(2026, 11, 23), GradeSemanal.segundaDa(LocalDate.of(2026, 11, 29)));
        assertEquals(LocalDate.of(2026, 11, 23), GradeSemanal.segundaDa(LocalDate.of(2026, 11, 23)));
    }

    @Test
    void semanaPedidaInvalidaOuLongeVoltaParaHoje() {
        LocalDate hoje = LocalDate.of(2026, 11, 24);
        LocalDate segunda = LocalDate.of(2026, 11, 23);
        assertEquals(segunda, GradeSemanal.semanaPedida(null, hoje));
        assertEquals(segunda, GradeSemanal.semanaPedida("lixo", hoje));
        assertEquals(segunda, GradeSemanal.semanaPedida("2030-01-01", hoje));
        assertEquals(LocalDate.of(2026, 11, 30), GradeSemanal.semanaPedida("2026-12-02", hoje));
    }

    @Test
    void horariosSemRepetirEEmOrdem() {
        List<AulaColetiva> aulas = List.of(aula("A", DayOfWeek.MONDAY, 18), aula("B", DayOfWeek.TUESDAY, 7), aula("C", DayOfWeek.FRIDAY, 18));
        assertEquals(List.of(LocalTime.of(7, 0), LocalTime.of(18, 0)), GradeSemanal.horarios(aulas));
    }

    @Test
    void aulasDeFiltraDiaEHorario() {
        List<AulaColetiva> aulas = List.of(aula("Zumba", DayOfWeek.MONDAY, 18), aula("Spinning", DayOfWeek.MONDAY, 18), aula("Yoga", DayOfWeek.TUESDAY, 18));
        List<AulaColetiva> r = GradeSemanal.aulasDe(aulas, DayOfWeek.MONDAY, LocalTime.of(18, 0));
        assertEquals(2, r.size());
        assertEquals("Spinning", r.get(0).getNome());
        assertTrue(GradeSemanal.aulasDe(aulas, DayOfWeek.WEDNESDAY, LocalTime.of(18, 0)).isEmpty());
    }

    @Test
    void estados() {
        LocalDateTime agora = LocalDateTime.of(2026, 11, 24, 12, 0);
        LocalTime h = LocalTime.of(18, 0);
        assertEquals(GradeSemanal.PASSOU, GradeSemanal.estado(LocalDate.of(2026, 11, 24), LocalTime.of(8, 0), agora, 5, false));
        assertEquals(GradeSemanal.RESERVADA, GradeSemanal.estado(LocalDate.of(2026, 11, 24), h, agora, 0, true));
        assertEquals(GradeSemanal.LOTADA, GradeSemanal.estado(LocalDate.of(2026, 11, 25), h, agora, 0, false));
        assertEquals(GradeSemanal.ABERTA, GradeSemanal.estado(LocalDate.of(2026, 11, 25), h, agora, 3, false));
        assertEquals(GradeSemanal.LONGE, GradeSemanal.estado(LocalDate.of(2026, 12, 30), h, agora, 3, false));
    }

    @Test
    void dataValidaConfereDiaPassadoELimite() {
        AulaColetiva a = aula("Spinning", DayOfWeek.WEDNESDAY, 18);
        LocalDateTime agora = LocalDateTime.of(2026, 11, 24, 12, 0);
        assertTrue(GradeSemanal.dataValida(a, LocalDate.of(2026, 11, 25), agora));
        assertFalse(GradeSemanal.dataValida(a, LocalDate.of(2026, 11, 26), agora), "dia da semana errado");
        assertFalse(GradeSemanal.dataValida(a, LocalDate.of(2026, 11, 18), agora), "já passou");
        assertFalse(GradeSemanal.dataValida(a, LocalDate.of(2026, 12, 30), agora), "longe demais");
        assertFalse(GradeSemanal.dataValida(null, LocalDate.of(2026, 11, 25), agora));
    }
}
