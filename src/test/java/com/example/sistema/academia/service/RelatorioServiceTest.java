package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Barra;
import com.example.sistema.academia.model.Mensalidade;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RelatorioServiceTest {

    private Mensalidade com(String situacao) {
        Mensalidade m = new Mensalidade();
        m.setSituacao(situacao);
        return m;
    }

    @Test
    void contaMensalidadesPorSituacao() {
        List<Mensalidade> lista = new ArrayList<>();
        for (int i = 0; i < 4; i++) lista.add(com("PAGA"));
        lista.add(com("ATRASADA"));
        lista.add(com("ATRASADA"));

        List<Barra> r = new RelatorioService().porSituacao(lista);

        assertEquals(5, r.size());
        assertEquals("PAGA", r.get(0).getRotulo());
        assertEquals("4", r.get(0).getTexto());
        assertEquals(100, r.get(0).getPercentual());
        assertEquals("2", r.get(3).getTexto());
        assertEquals(50, r.get(3).getPercentual());
        assertEquals("0", r.get(1).getTexto());
    }

    @Test
    void listaVaziaDaZeroSemErro() {
        List<Barra> r = new RelatorioService().porSituacao(new ArrayList<>());
        assertEquals(0, r.get(0).getPercentual());
    }

    @Test
    void faturamentoCalculaVariacaoMelhorMesTotalEMedia() {
        var r = new RelatorioService().detalheFaturamento(
                List.of("a", "b", "c"),
                List.of(new java.math.BigDecimal("100"), new java.math.BigDecimal("150"), new java.math.BigDecimal("75")));
        assertEquals(new java.math.BigDecimal("325"), r.total());
        assertEquals(new java.math.BigDecimal("108.33"), r.media());
        assertEquals("b", r.melhorMes());
        assertEquals(null, r.meses().get(0).variacaoPct());
        assertEquals(50.0, r.meses().get(1).variacaoPct(), 0.001);
        assertEquals(new java.math.BigDecimal("-75"), r.meses().get(2).variacaoValor());
        assertEquals(-50.0, r.meses().get(2).variacaoPct(), 0.001);
    }

    @Test
    void faturamentoSemBaseNaoDividePorZero() {
        var r = new RelatorioService().detalheFaturamento(
                List.of("a", "b"), List.of(java.math.BigDecimal.ZERO, new java.math.BigDecimal("50")));
        assertEquals(null, r.meses().get(1).variacaoPct());
        assertEquals("b", r.melhorMes());
    }

    @Test
    void diasMarcamPicoEVazio() {
        var r = new RelatorioService().detalheDias(new long[]{0, 10, 20, 10, 0, 0, 0});
        assertEquals(true, r.get(2).pico());
        assertEquals(50, r.get(2).pct());
        assertEquals(true, r.get(0).vazio());
        assertEquals(false, r.get(4).vazio());
    }

    @Test
    void diasSemEntradasNaoDestacaNada() {
        var r = new RelatorioService().detalheDias(new long[7]);
        assertEquals(false, r.get(0).pico());
        assertEquals(false, r.get(0).vazio());
    }

    @Test
    void situacoesSomamValorEPercentual() {
        List<Mensalidade> lista = new ArrayList<>();
        for (String sit : new String[]{"PAGA", "PAGA", "PAGA", "ATRASADA"}) {
            Mensalidade m = com(sit);
            m.setValor(new java.math.BigDecimal("90"));
            lista.add(m);
        }
        var r = new RelatorioService().detalheSituacoes(lista);
        assertEquals(new java.math.BigDecimal("270"), r.get(0).valor());
        assertEquals(75, r.get(0).pct());
        assertEquals(25, r.get(3).pct());
        assertEquals(java.math.BigDecimal.ZERO, r.get(1).valor());
    }
}
