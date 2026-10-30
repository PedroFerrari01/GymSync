package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Acompanhamento;
import com.example.sistema.academia.model.*;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Acompanhamento do professor: quem sumiu da academia e quanto cada aluno seguiu a ficha na semana.
 * Considera só alunos ativos que têm ficha nos treinos recebidos (os do professor, ou todos para o admin).
 */
@Service
public class AcompanhamentoService {

    public static final int DIAS_SUMIDO = 10;

    public List<Acompanhamento> montar(List<Treino> treinos, List<ExercicioTreino> exercicios,
                                       List<Entrada> entradas, List<RegistroExercicio> registros, LocalDate hoje) {
        LocalDate inicioSemana = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        Map<Long, Aluno> alunos = new LinkedHashMap<>();
        Set<Long> treinosConsiderados = new HashSet<>();
        for (Treino t : treinos) {
            treinosConsiderados.add(t.getId());
            if (t.getAluno() != null && "ATIVO".equals(t.getAluno().getSituacao())) {
                alunos.putIfAbsent(t.getAluno().getId(), t.getAluno());
            }
        }

        Map<Long, Integer> totalPorAluno = new HashMap<>();
        for (ExercicioTreino ex : exercicios) {
            if (ex.getTreino() == null || !treinosConsiderados.contains(ex.getTreino().getId())) continue;
            totalPorAluno.merge(ex.getTreino().getAluno().getId(), 1, Integer::sum);
        }

        Map<Long, LocalDate> ultimaEntrada = new HashMap<>();
        Map<Long, Long> entradasSemana = new HashMap<>();
        for (Entrada e : entradas) {
            if (e.getAluno() == null || e.getDataHoraEntrada() == null) continue;
            LocalDate dia = e.getDataHoraEntrada().toLocalDate();
            ultimaEntrada.merge(e.getAluno().getId(), dia, (a, b) -> a.isAfter(b) ? a : b);
            if (!dia.isBefore(inicioSemana)) entradasSemana.merge(e.getAluno().getId(), 1L, Long::sum);
        }

        // Exercício feito na semana: conta uma vez, mesmo registrado em dois dias
        Map<Long, Set<Long>> feitos = new HashMap<>();
        for (RegistroExercicio r : registros) {
            if (r.getAluno() == null || r.getExercicio() == null || r.getDataRegistro() == null) continue;
            if (r.getDataRegistro().isBefore(inicioSemana)) continue;
            if (!treinosConsiderados.contains(r.getExercicio().getTreino().getId())) continue;
            feitos.computeIfAbsent(r.getAluno().getId(), k -> new HashSet<>()).add(r.getExercicio().getId());
        }

        List<Acompanhamento> linhas = new ArrayList<>();
        for (Aluno a : alunos.values()) {
            LocalDate ultima = ultimaEntrada.get(a.getId());
            Integer dias = ultima == null ? null : (int) ChronoUnit.DAYS.between(ultima, hoje);
            boolean sumido = dias == null || dias >= DIAS_SUMIDO;
            int total = totalPorAluno.getOrDefault(a.getId(), 0);
            int feitosSemana = feitos.getOrDefault(a.getId(), Set.of()).size();
            int adesao = total == 0 ? 0 : Math.min(100, Math.round(feitosSemana * 100f / total));
            linhas.add(new Acompanhamento(a.getId(), a.getNome(), a.getContato(), dias,
                    entradasSemana.getOrDefault(a.getId(), 0L), feitosSemana, total, adesao, sumido));
        }
        // Quem está há mais tempo sem vir primeiro (quem nunca veio no topo); depois por nome
        linhas.sort(Comparator.comparing((Acompanhamento l) -> l.getDiasSemTreinar() == null ? Integer.MAX_VALUE : l.getDiasSemTreinar())
                .reversed().thenComparing(Acompanhamento::getNome));
        return linhas;
    }
}
