package com.example.sistema.academia.service;

import com.example.sistema.academia.model.AulaColetiva;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Cálculos da grade semanal de aulas (sem banco: só datas e listas). */
public final class GradeSemanal {

    /** Quantos dias à frente o aluno pode reservar. */
    public static final int DIAS_A_FRENTE = 28;

    public static final String PASSOU = "PASSOU";
    public static final String RESERVADA = "RESERVADA";
    public static final String LOTADA = "LOTADA";
    public static final String ABERTA = "ABERTA";
    public static final String LONGE = "LONGE";

    /** Uma aula numa data concreta, já com o estado para o aluno. */
    public record CelulaAula(AulaColetiva aula, LocalDate data, int livres, int inscritos,
                             boolean reservada, String estado) {}

    /** Uma linha de horário, com 7 células (seg a dom), cada uma com 0 ou mais aulas. */
    public record Linha(LocalTime horario, List<List<CelulaAula>> dias) {}

    /** Um dia da semana com as aulas dele (usado na versão do celular). */
    public record Dia(LocalDate data, String nome, boolean hoje, List<CelulaAula> aulas) {}

    private GradeSemanal() {}

    public static LocalDate segundaDa(LocalDate data) {
        return data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Lê "aaaa-mm-dd" e devolve a segunda-feira daquela semana; texto inválido cai na semana de hoje. */
    public static LocalDate semanaPedida(String texto, LocalDate hoje) {
        if (texto != null && !texto.isBlank()) {
            try {
                LocalDate pedida = segundaDa(LocalDate.parse(texto.trim()));
                // limite para ninguém navegar para sempre
                if (!pedida.isBefore(segundaDa(hoje).minusWeeks(8)) && !pedida.isAfter(segundaDa(hoje).plusWeeks(8))) {
                    return pedida;
                }
            } catch (RuntimeException ignorado) {
                // cai na semana de hoje
            }
        }
        return segundaDa(hoje);
    }

    /** Horários distintos das aulas, em ordem. */
    public static List<LocalTime> horarios(List<AulaColetiva> aulas) {
        TreeSet<LocalTime> h = new TreeSet<>();
        for (AulaColetiva a : aulas) if (a.getHorario() != null && a.getDiaSemana() != null) h.add(a.getHorario());
        return new ArrayList<>(h);
    }

    /** Aulas de um dia da semana e horário, em ordem de nome. */
    public static List<AulaColetiva> aulasDe(List<AulaColetiva> aulas, DayOfWeek dia, LocalTime horario) {
        List<AulaColetiva> r = new ArrayList<>();
        for (AulaColetiva a : aulas) {
            if (dia.equals(a.getDiaSemana()) && horario.equals(a.getHorario())) r.add(a);
        }
        r.sort(Comparator.comparing(a -> a.getNome() == null ? "" : a.getNome()));
        return r;
    }

    public static String estado(LocalDate data, LocalTime horario, LocalDateTime agora, int livres, boolean reservada) {
        if (data.atTime(horario == null ? LocalTime.MIDNIGHT : horario).isBefore(agora)) return PASSOU;
        if (reservada) return RESERVADA;
        if (livres <= 0) return LOTADA;
        if (data.isAfter(agora.toLocalDate().plusDays(DIAS_A_FRENTE))) return LONGE;
        return ABERTA;
    }

    /** Vale reservar/cancelar esta aula nesta data? (dia da semana certo, não passou, dentro do limite) */
    public static boolean dataValida(AulaColetiva aula, LocalDate data, LocalDateTime agora) {
        if (aula == null || data == null || aula.getDiaSemana() == null) return false;
        if (data.getDayOfWeek() != aula.getDiaSemana()) return false;
        LocalTime h = aula.getHorario() == null ? LocalTime.MIDNIGHT : aula.getHorario();
        if (data.atTime(h).isBefore(agora)) return false;
        return !data.isAfter(agora.toLocalDate().plusDays(DIAS_A_FRENTE));
    }
}
