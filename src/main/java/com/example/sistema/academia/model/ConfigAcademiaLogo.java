package com.example.sistema.academia.model;

import jakarta.persistence.*;

/** Logo da academia no banco (linha única, id 1), separada da configuração para não ser lida a cada página. */
@Entity
@Table(name = "config_academia_logo")
public class ConfigAcademiaLogo {

    @Id
    private Long id = ConfigAcademia.ID;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] dados;

    public ConfigAcademiaLogo() {}

    public ConfigAcademiaLogo(String tipo, byte[] dados) {
        this.tipo = tipo;
        this.dados = dados;
    }

    public Long getId() { return id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public byte[] getDados() { return dados; }
    public void setDados(byte[] dados) { this.dados = dados; }
}
