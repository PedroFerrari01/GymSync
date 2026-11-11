package com.example.sistema.academia.dto;

import java.util.List;

/** Progresso de um aluno para a tela do professor/admin: peso e cargas ao longo do tempo. */
public record ProgressoAluno(String nome, List<Serie> peso, List<Serie> cargas) {

    /** Uma sequência de barras. altura vai de 4 a 100 (porcentagem do maior valor). */
    public record Serie(String titulo, List<Barra> barras) {}

    public record Barra(String rotulo, String valor, int altura) {}
}
