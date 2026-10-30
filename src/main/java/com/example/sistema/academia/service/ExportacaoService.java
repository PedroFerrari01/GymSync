package com.example.sistema.academia.service;

import com.example.sistema.academia.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Planilhas (CSV) dos relatórios do administrador. Separador ";" e BOM no começo para o Excel em português
 * abrir com acentos e colunas certas.
 */
@Service
public class ExportacaoService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Locale BR = Locale.of("pt", "BR");

    @Autowired private RelatorioService relatorioService;

    public ExportacaoService() {}

    ExportacaoService(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    /** Um campo da planilha: entre aspas quando precisa e sem virar fórmula no Excel (=, +, -, @ no começo). */
    static String campo(Object valor) {
        if (valor == null) return "";
        String texto = valor.toString();
        if (!texto.isEmpty() && "=+-@\t\r".indexOf(texto.charAt(0)) >= 0) texto = "'" + texto;
        if (texto.contains(";") || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
            texto = "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }

    private static String planilha(List<String> cabecalho, List<List<Object>> linhas) {
        StringBuilder sb = new StringBuilder("﻿");
        sb.append(String.join(";", cabecalho)).append("\r\n");
        for (List<Object> linha : linhas) {
            StringJoiner sj = new StringJoiner(";");
            linha.forEach(v -> sj.add(campo(v)));
            sb.append(sj).append("\r\n");
        }
        return sb.toString();
    }

    private static String data(LocalDate d) {
        return d == null ? "" : d.format(DATA);
    }

    private static String dinheiro(BigDecimal v) {
        NumberFormat nf = NumberFormat.getNumberInstance(BR);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return v == null ? "" : nf.format(v);
    }

    public String alunos(List<Aluno> alunos, List<Matricula> matriculas) {
        Map<Long, Matricula> ativa = new HashMap<>();
        for (Matricula m : matriculas) {
            if ("ATIVA".equals(m.getSituacao()) && m.getAluno() != null) ativa.put(m.getAluno().getId(), m);
        }
        List<List<Object>> linhas = new ArrayList<>();
        for (Aluno a : alunos) {
            Matricula m = ativa.get(a.getId());
            linhas.add(Arrays.asList(a.getNome(), a.getCpf(), a.getEmail(), a.getContato(), data(a.getDataNascimento()), a.getSituacao(),
                    m != null && m.getPlano() != null ? m.getPlano().getNome() : "", m != null ? data(m.getDataFim()) : ""));
        }
        return planilha(List.of("Nome", "CPF", "E-mail", "Telefone", "Nascimento", "Situação", "Plano ativo", "Fim da matrícula"), linhas);
    }

    public String mensalidades(List<Mensalidade> mensalidades) {
        List<Mensalidade> ordenadas = new ArrayList<>(mensalidades);
        ordenadas.sort(Comparator.comparing(Mensalidade::getVencimento, Comparator.nullsLast(Comparator.naturalOrder())));
        List<List<Object>> linhas = new ArrayList<>();
        for (Mensalidade m : ordenadas) {
            Matricula mat = m.getMatricula();
            linhas.add(Arrays.asList(mat != null && mat.getAluno() != null ? mat.getAluno().getNome() : "",
                    mat != null && mat.getPlano() != null ? mat.getPlano().getNome() : "",
                    dinheiro(m.getValor()), data(m.getVencimento()), m.getSituacao(), data(m.getDataPagamento())));
        }
        return planilha(List.of("Aluno", "Plano", "Valor (R$)", "Vencimento", "Situação", "Pago em"), linhas);
    }

    public String entradas(List<Entrada> entradas) {
        List<List<Object>> linhas = new ArrayList<>();
        for (Entrada e : entradas) {
            if (e.getDataHoraEntrada() == null) continue;
            linhas.add(Arrays.asList(e.getDataHoraEntrada().toLocalDate().format(DATA), e.getDataHoraEntrada().format(HORA),
                    e.getAluno() != null ? e.getAluno().getNome() : ""));
        }
        return planilha(List.of("Data", "Hora", "Aluno"), linhas);
    }

    /** Um mês por linha (os últimos 12, do mais antigo ao atual), com o mês atual até hoje. */
    public String resumoMensal(LocalDate hoje, List<Entrada> entradas, List<Matricula> matriculas, List<Mensalidade> mensalidades) {
        List<List<Object>> linhas = new ArrayList<>();
        YearMonth atual = YearMonth.from(hoje);
        for (int i = 11; i >= 0; i--) {
            YearMonth mes = atual.minusMonths(i);
            int ateDia = i == 0 ? hoje.getDayOfMonth() : mes.lengthOfMonth();
            RelatorioService.ResumoMes r = relatorioService.resumoMes(mes, ateDia, entradas, matriculas, mensalidades);
            linhas.add(Arrays.asList(String.format("%02d/%d", mes.getMonthValue(), mes.getYear()), r.entradas(),
                    r.alunosQueTreinaram(), r.novasMatriculas(), dinheiro(r.recebido())));
        }
        return planilha(List.of("Mês", "Entradas na catraca", "Alunos que treinaram", "Novas matrículas", "Recebido (R$)"), linhas);
    }
}
