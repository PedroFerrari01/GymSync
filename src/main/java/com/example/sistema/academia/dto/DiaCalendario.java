package com.example.sistema.academia.dto;

/** Um quadradinho do calendário de frequência. Dia 0 = espaço vazio antes do dia 1 ou depois do último dia. */
public class DiaCalendario {
    private final int dia;
    private final boolean treinou;
    private final boolean hoje;
    private final boolean futuro;

    public DiaCalendario(int dia, boolean treinou, boolean hoje, boolean futuro) {
        this.dia = dia;
        this.treinou = treinou;
        this.hoje = hoje;
        this.futuro = futuro;
    }

    public int getDia() { return dia; }
    public boolean isTreinou() { return treinou; }
    public boolean isHoje() { return hoje; }
    public boolean isFuturo() { return futuro; }
    public boolean isVazio() { return dia == 0; }
}
