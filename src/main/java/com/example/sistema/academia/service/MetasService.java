package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.MetaProgresso;
import com.example.sistema.academia.model.AvaliacaoFisica;
import com.example.sistema.academia.model.Entrada;
import com.example.sistema.academia.model.MetaAluno;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Progresso das metas do aluno. Puro: recebe os dados prontos, não acessa banco. */
@Service
public class MetasService {

    public static final int MAX_METAS = 3;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Dias distintos com entrada desde a segunda-feira da semana de {@code hoje}. */
    public static int treinosDaSemana(List<Entrada> entradas, LocalDate hoje) {
        if (entradas == null) return 0;
        LocalDate segunda = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Set<LocalDate> dias = new HashSet<>();
        for (Entrada e : entradas) {
            if (e == null || e.getDataHoraEntrada() == null) continue;
            LocalDate d = e.getDataHoraEntrada().toLocalDate();
            if (!d.isBefore(segunda) && !d.isAfter(hoje)) dias.add(d);
        }
        return dias.size();
    }

    /** Percentual 0..100 de quem sai de {@code inicio} rumo a {@code alvo} e está em {@code atual} (serve para subir ou descer). */
    public static int percentualRumoAoAlvo(double inicio, double atual, double alvo) {
        if (Math.abs(alvo - inicio) < 1e-9) return atual == alvo ? 100 : 0;
        boolean descendo = alvo < inicio;
        if (descendo ? atual <= alvo : atual >= alvo) return 100;
        double p = (atual - inicio) / (alvo - inicio) * 100.0;
        return (int) Math.max(0, Math.min(100, Math.round(p)));
    }

    /** @param avaliacoes da mais antiga para a mais nova */
    public List<MetaProgresso> calcular(List<MetaAluno> metas, int treinosSemana, List<AvaliacaoFisica> avaliacoes, LocalDate hoje) {
        List<MetaProgresso> lista = new ArrayList<>();
        if (metas == null) return lista;
        List<AvaliacaoFisica> av = avaliacoes == null ? List.of() : avaliacoes;
        for (MetaAluno m : metas) {
            if (m == null) continue;
            lista.add(calcularUma(m, treinosSemana, av, hoje));
        }
        return lista;
    }

    private MetaProgresso calcularUma(MetaAluno m, int treinosSemana, List<AvaliacaoFisica> av, LocalDate hoje) {
        String tipo = m.getTipo();
        String titulo, icone, atual, alvoTxt, alvoCampo;
        int pct;
        boolean semDados = false;
        if (MetaAluno.TREINOS_SEMANA.equals(tipo)) {
            titulo = "Treinos por semana";
            icone = "ph-barbell";
            int alvo = (int) Math.round(m.getAlvo());
            pct = alvo <= 0 ? 100 : (int) Math.min(100, Math.round(treinosSemana * 100.0 / alvo));
            atual = treinosSemana + (treinosSemana == 1 ? " treino" : " treinos") + " nesta semana";
            alvoTxt = "Alvo: " + alvo + (alvo == 1 ? " treino" : " treinos");
            alvoCampo = String.valueOf(alvo);
        } else {
            boolean peso = MetaAluno.PESO.equals(tipo);
            titulo = peso ? "Peso alvo" : "Gordura corporal alvo";
            icone = peso ? "ph-scales" : "ph-percent";
            String un = peso ? " kg" : "%";
            List<Double> valores = new ArrayList<>();
            for (AvaliacaoFisica a : av) {
                Double v = peso ? a.getPeso() : a.getPercentualGordura();
                if (v != null) valores.add(v);
            }
            alvoTxt = "Alvo: " + num(m.getAlvo()) + un;
            alvoCampo = num(m.getAlvo()).replace(',', '.');
            if (valores.isEmpty()) {
                semDados = true;
                pct = 0;
                atual = "Sem avaliação ainda";
            } else {
                double primeira = valores.get(0), ultima = valores.get(valores.size() - 1);
                pct = percentualRumoAoAlvo(primeira, ultima, m.getAlvo());
                atual = "Agora: " + num(ultima) + un + " (início " + num(primeira) + un + ")";
            }
        }
        String prazoTxt = null;
        boolean atrasada = false;
        if (m.getPrazo() != null) {
            prazoTxt = "Até " + m.getPrazo().format(DATA);
            atrasada = pct < 100 && m.getPrazo().isBefore(hoje);
        }
        return new MetaProgresso(m.getId(), tipo, titulo, icone, atual, alvoTxt, pct, pct >= 100 && !semDados, semDados,
                prazoTxt, atrasada, alvoCampo, m.getPrazo() != null ? m.getPrazo().toString() : "");
    }

    private static String num(double v) {
        return (v == Math.rint(v)) ? String.valueOf((long) v) : String.format(Locale.forLanguageTag("pt-BR"), "%.1f", v);
    }

    /** Valida alvo por tipo. Devolve mensagem de erro ou null. */
    public static String validar(String tipo, Double alvo, LocalDate prazo, LocalDate hoje) {
        if (tipo == null || !(MetaAluno.TREINOS_SEMANA.equals(tipo) || MetaAluno.PESO.equals(tipo) || MetaAluno.GORDURA.equals(tipo)))
            return "Escolha o tipo da meta.";
        if (alvo == null || alvo.isNaN() || alvo.isInfinite()) return "Informe o valor da meta.";
        if (MetaAluno.TREINOS_SEMANA.equals(tipo) && (alvo < 1 || alvo > 7 || alvo != Math.rint(alvo)))
            return "Treinos por semana: informe um número de 1 a 7.";
        if (MetaAluno.PESO.equals(tipo) && (alvo < 30 || alvo > 300)) return "Peso alvo: informe entre 30 e 300 kg.";
        if (MetaAluno.GORDURA.equals(tipo) && (alvo < 3 || alvo > 60)) return "Gordura alvo: informe entre 3% e 60%.";
        if (prazo != null && prazo.isBefore(hoje)) return "O prazo não pode ser uma data que já passou.";
        return null;
    }
}
