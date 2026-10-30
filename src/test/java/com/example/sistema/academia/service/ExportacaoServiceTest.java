package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Comparacao;
import com.example.sistema.academia.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Planilhas do admin (ExportacaoService) e comparação mês a mês (RelatorioService). */
class ExportacaoServiceTest {

    private final RelatorioService relatorio = new RelatorioService();
    private final ExportacaoService exportacao = new ExportacaoService(relatorio);
    private final LocalDate hoje = LocalDate.of(2026, 10, 15);

    private Aluno aluno(long id, String nome) {
        Aluno a = new Aluno();
        a.setId(id);
        a.setNome(nome);
        return a;
    }

    private Entrada entrada(Aluno a, LocalDate dia) {
        Entrada e = new Entrada();
        e.setAluno(a);
        e.setDataHoraEntrada(dia.atTime(7, 30));
        return e;
    }

    private Mensalidade paga(String valor, LocalDate dia) {
        Mensalidade m = new Mensalidade();
        m.setValor(new BigDecimal(valor));
        m.setSituacao("PAGA");
        m.setDataPagamento(dia);
        return m;
    }

    private Matricula matricula(LocalDate inicio) {
        Matricula m = new Matricula();
        m.setDataInicio(inicio);
        return m;
    }

    @Test
    void campoNaoViraFormulaEProtegeOSeparador() {
        assertEquals("'=1+1", ExportacaoService.campo("=1+1"));
        assertEquals("'@SOMA(A1)", ExportacaoService.campo("@SOMA(A1)"));
        assertEquals("\"Silva; Ana\"", ExportacaoService.campo("Silva; Ana"));
        assertEquals("\"diz \"\"oi\"\"\"", ExportacaoService.campo("diz \"oi\""));
        assertEquals("", ExportacaoService.campo(null));
        assertEquals("(11) 99999-0000", ExportacaoService.campo("(11) 99999-0000"));
    }

    @Test
    void planilhaDeEntradasComBomECabecalho() {
        String csv = exportacao.entradas(List.of(entrada(aluno(1, "Ana Souza"), hoje)));
        assertTrue(csv.startsWith("﻿Data;Hora;Aluno\r\n"));
        assertTrue(csv.contains("15/10/2026;07:30;Ana Souza\r\n"));
    }

    @Test
    void mesAMesComparaOMesmoPeriodo() {
        Aluno ana = aluno(1, "Ana"), bruno = aluno(2, "Bruno");
        List<Entrada> entradas = List.of(
                entrada(ana, LocalDate.of(2026, 9, 2)), entrada(bruno, LocalDate.of(2026, 9, 10)),
                entrada(ana, LocalDate.of(2026, 9, 20)), // depois do dia 15: fora da comparação
                entrada(ana, LocalDate.of(2026, 10, 1)), entrada(ana, LocalDate.of(2026, 10, 3)), entrada(ana, LocalDate.of(2026, 10, 14)));
        List<Mensalidade> mensalidades = List.of(paga("100.00", LocalDate.of(2026, 9, 5)), paga("150.00", LocalDate.of(2026, 10, 5)));
        List<Matricula> matriculas = List.of(matricula(LocalDate.of(2026, 10, 2)));

        List<Comparacao> linhas = relatorio.mesAMes(hoje, entradas, matriculas, mensalidades);
        assertEquals("2", linhas.get(0).getPrimeira());
        assertEquals("3", linhas.get(0).getUltima());
        assertEquals("+50%", linhas.get(0).getDiferenca());
        assertEquals("melhorou", linhas.get(0).getClasse());

        assertEquals("-50%", linhas.get(1).getDiferenca()); // 2 alunos -> 1 aluno
        assertEquals("piorou", linhas.get(1).getClasse());
        assertEquals("novo", linhas.get(2).getDiferenca()); // 0 -> 1 matrícula
        assertEquals("R$ 150,00", linhas.get(3).getUltima());
        assertEquals("+50%", linhas.get(3).getDiferenca());
    }

    @Test
    void resumoMensalTemDozeMeses() {
        String csv = exportacao.resumoMensal(hoje, List.of(entrada(aluno(1, "Ana"), hoje)), List.of(), List.of(paga("89.90", hoje)));
        String[] linhas = csv.split("\r\n");
        assertEquals(13, linhas.length); // cabeçalho + 12 meses
        assertTrue(linhas[1].startsWith("11/2025;"));
        assertEquals("10/2026;1;1;0;89,90", linhas[12]);
    }
}
