package com.example.sistema.academia.dto;

/** Uma linha do acompanhamento do professor: frequência e adesão de um aluno com ficha. */
public class Acompanhamento {
    private final Long alunoId;
    private final String nome;
    private final String contato;
    private final Integer diasSemTreinar; // null = nunca passou pela catraca
    private final long entradasSemana;
    private final int feitosSemana;
    private final int totalExercicios;
    private final int adesao;
    private final boolean sumido;
    private String linkWhatsapp;

    public Acompanhamento(Long alunoId, String nome, String contato, Integer diasSemTreinar, long entradasSemana,
                          int feitosSemana, int totalExercicios, int adesao, boolean sumido) {
        this.alunoId = alunoId;
        this.nome = nome;
        this.contato = contato;
        this.diasSemTreinar = diasSemTreinar;
        this.entradasSemana = entradasSemana;
        this.feitosSemana = feitosSemana;
        this.totalExercicios = totalExercicios;
        this.adesao = adesao;
        this.sumido = sumido;
    }

    public Long getAlunoId() { return alunoId; }
    public String getNome() { return nome; }
    public String getContato() { return contato; }
    public Integer getDiasSemTreinar() { return diasSemTreinar; }
    public long getEntradasSemana() { return entradasSemana; }
    public int getFeitosSemana() { return feitosSemana; }
    public int getTotalExercicios() { return totalExercicios; }
    /** Exercícios diferentes registrados na semana / exercícios da ficha, de 0 a 100. */
    public int getAdesao() { return adesao; }
    public boolean isSumido() { return sumido; }
    public String getLinkWhatsapp() { return linkWhatsapp; }
    public void setLinkWhatsapp(String linkWhatsapp) { this.linkWhatsapp = linkWhatsapp; }

    public String getUltimaVisita() {
        if (diasSemTreinar == null) return "Nunca veio";
        if (diasSemTreinar == 0) return "Hoje";
        if (diasSemTreinar == 1) return "Ontem";
        return "Há " + diasSemTreinar + " dias";
    }
}
