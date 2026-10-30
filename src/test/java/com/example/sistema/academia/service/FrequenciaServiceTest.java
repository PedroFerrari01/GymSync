package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.CalendarioMes;
import com.example.sistema.academia.dto.DiaCalendario;
import com.example.sistema.academia.model.Entrada;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrequenciaServiceTest {

    private final FrequenciaService service = new FrequenciaService();
    private final LocalDate hoje = LocalDate.of(2026, 10, 30);

    private Entrada entrada(int ano, int mes, int dia, int hora) {
        Entrada e = new Entrada();
        e.setDataHoraEntrada(LocalDateTime.of(ano, mes, dia, hora, 0));
        return e;
    }

    @Test
    void marcaOsDiasTreinadosDoMesSemContarDuasVezes() {
        List<Entrada> entradas = List.of(
                entrada(2026, 10, 1, 7), entrada(2026, 10, 1, 19), // duas entradas no mesmo dia = 1 dia
                entrada(2026, 10, 15, 8),
                entrada(2026, 9, 30, 8)); // mês anterior não entra
        CalendarioMes cal = service.calendario(entradas, YearMonth.of(2026, 10), hoje);

        assertEquals("Outubro de 2026", cal.getTitulo());
        assertEquals(2, cal.getDiasTreinados());
        List<DiaCalendario> dias = cal.getSemanas().stream().flatMap(List::stream).filter(d -> !d.isVazio()).toList();
        assertEquals(31, dias.size());
        assertTrue(dias.get(0).isTreinou());
        assertTrue(dias.get(14).isTreinou());
        assertFalse(dias.get(1).isTreinou());
        assertTrue(dias.get(29).isHoje());
        assertTrue(dias.get(30).isFuturo());
    }

    @Test
    void semanasComecamNaSegundaETemSeteDias() {
        // 01/10/2026 é uma quinta: três espaços vazios antes (seg, ter, qua)
        CalendarioMes cal = service.calendario(List.of(), YearMonth.of(2026, 10), hoje);
        List<DiaCalendario> primeira = cal.getSemanas().get(0);
        assertTrue(primeira.get(2).isVazio());
        assertEquals(1, primeira.get(3).getDia());
        cal.getSemanas().forEach(s -> assertEquals(7, s.size()));
    }

    @Test
    void naoNavegaParaOFuturo() {
        assertEquals(YearMonth.of(2026, 10), service.mesPedido("2027-01", hoje));
        assertEquals(YearMonth.of(2026, 10), service.mesPedido("bobagem", hoje));
        assertEquals(YearMonth.of(2026, 10), service.mesPedido(null, hoje));
        assertEquals(YearMonth.of(2026, 8), service.mesPedido("2026-08", hoje));

        assertNull(service.calendario(List.of(), YearMonth.of(2026, 10), hoje).getMesSeguinte());
        CalendarioMes setembro = service.calendario(List.of(), YearMonth.of(2026, 9), hoje);
        assertEquals("2026-10", setembro.getMesSeguinte());
        assertEquals("2026-08", setembro.getMesAnterior());
    }
}
