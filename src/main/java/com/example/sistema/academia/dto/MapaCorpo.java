package com.example.sistema.academia.dto;

import java.util.List;

/** Dados do mapa do corpo de um aluno: as medidas por região e o período entre a primeira e a última avaliação. */
public class MapaCorpo {
    private final Long alunoId;
    private final String nome;
    private final int avaliacoes;
    private final String dataPrimeira;
    private final String dataUltima;
    private final Double alturaCm;
    private final List<MedidaCorpo> medidas;

    public MapaCorpo(Long alunoId, String nome, int avaliacoes, String dataPrimeira, String dataUltima,
                     Double alturaCm, List<MedidaCorpo> medidas) {
        this.alunoId = alunoId;
        this.nome = nome;
        this.avaliacoes = avaliacoes;
        this.dataPrimeira = dataPrimeira;
        this.dataUltima = dataUltima;
        this.alturaCm = alturaCm;
        this.medidas = medidas;
    }

    public Long getAlunoId() { return alunoId; }
    public String getNome() { return nome; }
    public int getAvaliacoes() { return avaliacoes; }
    public String getDataPrimeira() { return dataPrimeira; }
    public String getDataUltima() { return dataUltima; }
    /** Nulo quando a altura nunca foi informada. */
    public Double getAlturaCm() { return alturaCm; }
    public List<MedidaCorpo> getMedidas() { return medidas; }
}
