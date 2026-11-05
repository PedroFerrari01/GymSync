package com.example.sistema.academia.model;

import jakarta.persistence.*;

/** Foto do aparelho, guardada no banco (no site publicado os arquivos de pasta somem a cada publicação). */
@Entity
@Table(name = "equipamento_imagens")
public class EquipamentoImagem {

    @Id
    private Long equipamentoId;

    @Column(nullable = false, length = 20)
    private String tipo;

    // LONGBLOB de propósito: o tipo escolhido sozinho pelo Hibernate no MySQL era pequeno demais para uma foto
    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] dados;

    public EquipamentoImagem() {}

    public EquipamentoImagem(Long equipamentoId, String tipo, byte[] dados) {
        this.equipamentoId = equipamentoId;
        this.tipo = tipo;
        this.dados = dados;
    }

    public Long getEquipamentoId() { return equipamentoId; }
    public void setEquipamentoId(Long equipamentoId) { this.equipamentoId = equipamentoId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public byte[] getDados() { return dados; }
    public void setDados(byte[] dados) { this.dados = dados; }
}
