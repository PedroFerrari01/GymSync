package com.example.sistema.academia.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Conta presenças e faltas nas aulas coletivas. Puro: não usa banco, só as marcações recebidas. */
public final class PresencaCalculo {

    public static final int JANELA_DIAS = 30;

    private PresencaCalculo() {}

    public record Marca(Long alunoId, String nome, LocalDate data, boolean presente) {}

    public record Linha(Long alunoId, String nome, int presentes, int faltas, int total, int percentual) {}

    /** Por aluno, só os últimos {@code dias} dias até hoje; maiores faltosos primeiro (faltas, depois menor %). */
    public static List<Linha> ranking(List<Marca> marcas, LocalDate hoje, int dias) {
        Map<Long, int[]> contas = new LinkedHashMap<>();
        Map<Long, String> nomes = new LinkedHashMap<>();
        LocalDate inicio = hoje.minusDays(dias);
        for (Marca m : marcas) {
            if (m.data() == null || !m.data().isAfter(inicio) || m.data().isAfter(hoje)) continue;
            int[] c = contas.computeIfAbsent(m.alunoId(), k -> new int[2]);
            nomes.putIfAbsent(m.alunoId(), m.nome());
            if (m.presente()) c[0]++; else c[1]++;
        }
        List<Linha> linhas = new ArrayList<>();
        contas.forEach((id, c) -> linhas.add(linha(id, nomes.get(id), c[0], c[1])));
        linhas.sort(Comparator.comparingInt(Linha::faltas).reversed()
                .thenComparingInt(Linha::percentual)
                .thenComparing(Linha::nome, String.CASE_INSENSITIVE_ORDER));
        return linhas;
    }

    /** Resumo de um aluno só (para a Minha Área); zeros se não houver marcação. */
    public static Linha doAluno(Long alunoId, List<Marca> marcas, LocalDate hoje, int dias) {
        return ranking(marcas.stream().filter(m -> alunoId.equals(m.alunoId())).toList(), hoje, dias).stream()
                .findFirst().orElse(new Linha(alunoId, "", 0, 0, 0, 0));
    }

    private static Linha linha(Long id, String nome, int presentes, int faltas) {
        int total = presentes + faltas;
        int pct = total == 0 ? 0 : (int) Math.round(presentes * 100.0 / total);
        return new Linha(id, nome, presentes, faltas, total, pct);
    }
}
