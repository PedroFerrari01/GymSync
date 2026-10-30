package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.CalendarioMes;
import com.example.sistema.academia.dto.DiaCalendario;
import com.example.sistema.academia.model.Entrada;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class FrequenciaService {

    private static final Locale BR = Locale.of("pt", "BR");

    /** Lê o mês pedido na tela (aaaa-mm). Mês inválido ou no futuro vira o mês atual. */
    public YearMonth mesPedido(String texto, LocalDate hoje) {
        YearMonth atual = YearMonth.from(hoje);
        if (texto == null || texto.isBlank()) return atual;
        try {
            YearMonth pedido = YearMonth.parse(texto.trim());
            return pedido.isAfter(atual) ? atual : pedido;
        } catch (DateTimeParseException e) {
            return atual;
        }
    }

    /** Calendário do mês com os dias em que o aluno passou pela catraca. Semanas começam na segunda. */
    public CalendarioMes calendario(List<Entrada> entradas, YearMonth mes, LocalDate hoje) {
        Set<Integer> treinados = new HashSet<>();
        for (Entrada e : entradas) {
            if (e.getDataHoraEntrada() == null) continue;
            LocalDate data = e.getDataHoraEntrada().toLocalDate();
            if (YearMonth.from(data).equals(mes)) treinados.add(data.getDayOfMonth());
        }

        List<List<DiaCalendario>> semanas = new ArrayList<>();
        List<DiaCalendario> semana = new ArrayList<>();
        int vaziosAntes = mes.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < vaziosAntes; i++) semana.add(new DiaCalendario(0, false, false, false));

        for (int dia = 1; dia <= mes.lengthOfMonth(); dia++) {
            LocalDate data = mes.atDay(dia);
            semana.add(new DiaCalendario(dia, treinados.contains(dia), data.equals(hoje), data.isAfter(hoje)));
            if (semana.size() == 7) {
                semanas.add(semana);
                semana = new ArrayList<>();
            }
        }
        if (!semana.isEmpty()) {
            while (semana.size() < 7) semana.add(new DiaCalendario(0, false, false, false));
            semanas.add(semana);
        }

        String nomeMes = mes.getMonth().getDisplayName(TextStyle.FULL, BR);
        String titulo = Character.toUpperCase(nomeMes.charAt(0)) + nomeMes.substring(1) + " de " + mes.getYear();
        String seguinte = mes.isBefore(YearMonth.from(hoje)) ? mes.plusMonths(1).toString() : null;
        return new CalendarioMes(titulo, semanas, treinados.size(), mes.minusMonths(1).toString(), seguinte);
    }
}
