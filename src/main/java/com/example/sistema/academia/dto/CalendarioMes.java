package com.example.sistema.academia.dto;

import java.util.List;

public class CalendarioMes {
    private final String titulo;
    private final List<List<DiaCalendario>> semanas;
    private final int diasTreinados;
    private final String mesAnterior;
    private final String mesSeguinte;

    public CalendarioMes(String titulo, List<List<DiaCalendario>> semanas, int diasTreinados, String mesAnterior, String mesSeguinte) {
        this.titulo = titulo;
        this.semanas = semanas;
        this.diasTreinados = diasTreinados;
        this.mesAnterior = mesAnterior;
        this.mesSeguinte = mesSeguinte;
    }

    public String getTitulo() { return titulo; }
    public List<List<DiaCalendario>> getSemanas() { return semanas; }
    public int getDiasTreinados() { return diasTreinados; }
    /** Mês anterior no formato aaaa-mm (para o link de navegação). */
    public String getMesAnterior() { return mesAnterior; }
    /** Mês seguinte no formato aaaa-mm, ou null se for o mês atual (não há o que ver no futuro). */
    public String getMesSeguinte() { return mesSeguinte; }
}
