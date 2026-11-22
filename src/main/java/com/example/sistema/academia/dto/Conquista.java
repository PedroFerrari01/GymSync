package com.example.sistema.academia.dto;

/** Uma conquista (badge) do aluno: calculada na hora, nada é gravado. */
public record Conquista(String id, String nome, String descricao, String icone, int atual, int meta, String progresso) {

    public boolean conquistada() {
        return atual >= meta;
    }

    /** Percentual de 0 a 100 para a barrinha de progresso. */
    public int percentual() {
        if (meta <= 0) return 100;
        return Math.min(100, (int) Math.round(atual * 100.0 / meta));
    }
}
