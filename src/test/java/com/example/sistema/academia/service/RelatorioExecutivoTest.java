package com.example.sistema.academia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.Matricula;
import com.example.sistema.academia.model.Mensalidade;
import com.example.sistema.academia.model.Plano;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class RelatorioExecutivoTest {

    private static final YearMonth MES = YearMonth.of(2026, 11);

    private Aluno aluno(Long id, String situacao) {
        Aluno a = new Aluno();
        a.setId(id);
        a.setSituacao(situacao);
        return a;
    }

    private Matricula matricula(Aluno a, String plano, String situacao, LocalDate inicio) {
        Plano p = new Plano();
        p.setNome(plano);
        Matricula m = new Matricula();
        m.setAluno(a);
        m.setPlano(p);
        m.setSituacao(situacao);
        m.setDataInicio(inicio);
        return m;
    }

    private Mensalidade mensalidade(Matricula mat, String situacao, String valor, LocalDate pago) {
        Mensalidade m = new Mensalidade();
        m.setMatricula(mat);
        m.setSituacao(situacao);
        m.setValor(new BigDecimal(valor));
        m.setDataPagamento(pago);
        return m;
    }

    @Test
    void resumoCalculaReceitaVariacaoTicketEInadimplencia() {
        Aluno a1 = aluno(1L, "ATIVO");
        Aluno a2 = aluno(2L, "ATIVO");
        Aluno a3 = aluno(3L, "INATIVO");
        Matricula m1 = matricula(a1, "Anual", "ATIVA", LocalDate.of(2026, 11, 2));
        Matricula m2 = matricula(a2, "Mensal", "ATIVA", LocalDate.of(2026, 5, 2));
        Matricula m3 = matricula(a3, "Mensal", "CANCELADA", LocalDate.of(2026, 1, 2));
        List<Mensalidade> mens = List.of(
                mensalidade(m1, "PAGA", "100", LocalDate.of(2026, 11, 5)),
                mensalidade(m2, "PAGA", "50", LocalDate.of(2026, 11, 8)),
                mensalidade(m1, "PAGA", "100", LocalDate.of(2026, 10, 5)),
                mensalidade(m2, "ATRASADA", "70", null),
                mensalidade(m2, "ATRASADA", "70", null));

        var r = RelatorioExecutivo.resumo(MES, List.of(a1, a2, a3), List.of(m1, m2, m3), mens);

        assertEquals(2, r.alunosAtivos());
        assertEquals(1, r.alunosInativos());
        assertEquals(1, r.matriculasNovasMes());
        assertEquals(1, r.matriculasCanceladas());
        assertEquals(new BigDecimal("150"), r.receitaMes());
        assertEquals(new BigDecimal("100"), r.receitaMesAnterior());
        assertEquals(50.0, r.variacaoPct(), 0.001);
        assertEquals(2, r.pagamentosMes());
        assertEquals(new BigDecimal("75.00"), r.ticketMedio());
        assertEquals(new BigDecimal("140"), r.emAtraso());
        assertEquals(1, r.alunosInadimplentes());
        assertEquals(50.0, r.inadimplenciaPct(), 0.001);
    }

    @Test
    void semBaseNoMesAnteriorNaoHaVariacaoNemDivisaoPorZero() {
        var r = RelatorioExecutivo.resumo(MES, List.of(), List.of(), List.of());
        assertNull(r.variacaoPct());
        assertNull(r.inadimplenciaPct());
        assertEquals(BigDecimal.ZERO, r.ticketMedio());
    }

    @Test
    void receitaDe12MesesVemEmOrdemCronologica() {
        Matricula m = matricula(aluno(1L, "ATIVO"), "Anual", "ATIVA", LocalDate.of(2026, 1, 1));
        List<Mensalidade> mens = List.of(
                mensalidade(m, "PAGA", "10", LocalDate.of(2025, 12, 3)),
                mensalidade(m, "PAGA", "20", LocalDate.of(2026, 11, 3)));
        List<BigDecimal> v = RelatorioExecutivo.receitaPorMes(MES, 12, mens);
        assertEquals(12, v.size());
        assertEquals(new BigDecimal("10"), v.get(0));
        assertEquals(new BigDecimal("20"), v.get(11));
        List<String> r = RelatorioExecutivo.rotulosMeses(MES, 12);
        assertEquals("12/25", r.get(0));
        assertEquals("11/26", r.get(11));
    }

    @Test
    void planosMaisContratadosContaSoMatriculasAtivas() {
        Aluno a = aluno(1L, "ATIVO");
        var lista = RelatorioExecutivo.planosMaisContratados(List.of(
                matricula(a, "Anual", "ATIVA", null), matricula(a, "Anual", "ATIVA", null),
                matricula(a, "Mensal", "ATIVA", null), matricula(a, "Mensal", "CANCELADA", null)), 5);
        assertEquals(2, lista.size());
        assertEquals("Anual", lista.get(0).plano());
        assertEquals(100, lista.get(0).pct());
        assertEquals(50, lista.get(1).pct());
    }

    @Test
    void graficoSvgEscapaRotulosEDesenhaUmaBarraPorValor() {
        String svg = RelatorioExecutivo.graficoBarras(List.of("<a>", "b"), List.of(5, 10), 200, 100);
        assertTrue(svg.startsWith("<svg"));
        assertTrue(svg.contains("&lt;a&gt;"));
        assertEquals(2, svg.split("<rect", -1).length - 1);
        // sem dados: não quebra
        assertTrue(RelatorioExecutivo.graficoBarras(List.of(), List.of(), 200, 100).contains("</svg>"));
    }
}
