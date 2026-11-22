package com.example.sistema.academia.service;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MovimentoServiceTest {

    // 2026-11-23 é segunda-feira
    private static LocalDateTime seg(int hora) { return LocalDateTime.of(2026, 11, 23, hora, 10); }
    private static LocalDateTime ter(int hora) { return LocalDateTime.of(2026, 11, 24, hora, 10); }

    private static List<LocalDateTime> varias(LocalDateTime d, int vezes) {
        List<LocalDateTime> l = new ArrayList<>();
        for (int i = 0; i < vezes; i++) l.add(d);
        return l;
    }

    @Test
    void semDadosMapaVazioESemSugestao() {
        long[][] m = MovimentoService.matriz(List.of());
        assertTrue(MovimentoService.mapa(m).vazio());
        MovimentoService.Sugestao s = MovimentoService.sugerir(m, -1);
        assertTrue(s.semDados());
        assertTrue(s.melhores().isEmpty());
        assertNull(s.maisCheio());
        assertTrue(MovimentoService.matriz(null)[0].length > 0);
    }

    @Test
    void contaPorDiaEHoraEIgnoraForaDaFaixa() {
        List<LocalDateTime> e = new ArrayList<>();
        e.addAll(varias(seg(18), 3));
        e.add(ter(7));
        e.add(seg(3));   // antes das 5h
        e.add(seg(23));  // última hora da grade
        long[][] m = MovimentoService.matriz(e);
        assertEquals(3, m[0][18 - MovimentoService.HORA_INICIO]);
        assertEquals(1, m[1][7 - MovimentoService.HORA_INICIO]);
        assertEquals(1, m[0][23 - MovimentoService.HORA_INICIO]);
        MovimentoService.Mapa mapa = MovimentoService.mapa(m);
        assertEquals(5, mapa.total());
        assertEquals(3, mapa.maximo());
        assertEquals(4, mapa.linhas().get(0).total());
        assertEquals(5, mapa.linhas().get(0).celulas().get(13).nivel());
        assertEquals(0, mapa.linhas().get(3).celulas().get(0).nivel());
        assertEquals(2, mapa.linhas().get(1).celulas().get(2).nivel());
    }

    @Test
    void umDiaSoSugereHorasComMovimentoDaSemana() {
        List<LocalDateTime> e = new ArrayList<>();
        e.addAll(varias(seg(7), 5));
        e.addAll(varias(seg(12), 1));
        e.addAll(varias(seg(18), 9));
        e.addAll(varias(seg(19), 2));
        long[][] m = MovimentoService.matriz(e);
        MovimentoService.Sugestao s = MovimentoService.sugerir(m, 0);
        assertEquals(3, s.melhores().size());
        assertEquals(12, s.melhores().get(0).hora());
        assertEquals(19, s.melhores().get(1).hora());
        assertEquals(7, s.melhores().get(2).hora());
        assertEquals(18, s.maisCheio().hora());
        // horas sem nenhuma entrada (ex.: 6h, 14h) nunca aparecem
        assertTrue(s.melhores().stream().noneMatch(h -> h.hora() == 6 || h.hora() == 14));
    }

    @Test
    void diaSemMovimentoNaoSugere() {
        long[][] m = MovimentoService.matriz(varias(seg(18), 4));
        MovimentoService.Sugestao terca = MovimentoService.sugerir(m, 1);
        assertTrue(terca.semDados());
        assertFalse(MovimentoService.sugerir(m, -1).semDados());
    }

    @Test
    void empateDesempataPelaHoraMaisCedoESemMaisCheio() {
        List<LocalDateTime> e = new ArrayList<>();
        e.addAll(varias(seg(9), 2));
        e.addAll(varias(seg(10), 2));
        e.addAll(varias(seg(11), 2));
        MovimentoService.Sugestao s = MovimentoService.sugerir(MovimentoService.matriz(e), -1);
        assertEquals(List.of(9, 10, 11), s.melhores().stream().map(MovimentoService.Horario::hora).toList());
        assertNull(s.maisCheio()); // todos iguais: nada a evitar
    }

    @Test
    void semanaSomaTodosOsDias() {
        List<LocalDateTime> e = new ArrayList<>();
        e.addAll(varias(seg(18), 3));
        e.addAll(varias(ter(18), 3));
        e.addAll(varias(ter(6), 1));
        e.addAll(varias(seg(8), 2));
        e.addAll(varias(seg(12), 3));
        MovimentoService.Sugestao s = MovimentoService.sugerir(MovimentoService.matriz(e), -1);
        assertEquals(6, s.melhores().get(0).hora());
        assertEquals(18, s.maisCheio().hora());
        assertEquals("cheio", s.maisCheio().nivelTexto());
    }

    @Test
    void planoComecaEmHojeEAmanhaEMarcaMelhoresHorarios() {
        List<LocalDateTime> e = new ArrayList<>();
        e.addAll(varias(seg(7), 5));
        e.addAll(varias(seg(12), 1));
        e.addAll(varias(seg(18), 9));
        e.addAll(varias(seg(19), 2));
        MovimentoService.PlanoAluno p = MovimentoService.plano(MovimentoService.matriz(e), 6); // hoje = domingo
        assertEquals(7, p.dias().size());
        assertEquals("Hoje", p.dias().get(0).destaque());
        assertEquals("Amanhã", p.dias().get(1).destaque());
        assertTrue(p.dias().get(0).semDados());
        MovimentoService.DiaAluno segunda = p.dias().get(1);
        assertEquals(19, segunda.barras().size());
        assertEquals(100, segunda.barras().get(18 - MovimentoService.HORA_INICIO).percentual());
        assertTrue(segunda.barras().get(18 - MovimentoService.HORA_INICIO).evitar());
        assertTrue(segunda.barras().get(12 - MovimentoService.HORA_INICIO).melhor());
        assertTrue(segunda.resumo().startsWith("Melhor horário: 12h às 13h"));
        assertTrue(segunda.resumo().contains("Evite: 18h às 19h"));
    }
}
