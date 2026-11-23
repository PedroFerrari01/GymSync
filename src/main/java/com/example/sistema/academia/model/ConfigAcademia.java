package com.example.sistema.academia.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Identidade da academia (linha única, id 1): cor de destaque, nome e se há logo. A imagem fica em ConfigAcademiaLogo. */
@Entity
@Table(name = "config_academia")
public class ConfigAcademia {

    public static final Long ID = 1L;

    @Id
    private Long id = ID;

    /** #RRGGBB; null = laranja padrão do sistema. */
    @Column(length = 7)
    private String cor;

    @Column(length = 60)
    private String nome;

    @Column(nullable = false)
    private boolean temLogo;

    /** A logo é clara (ex.: letras brancas): aparece sobre um quadro escuro em vez do claro. */
    @Column(nullable = false)
    private boolean logoClara;

    private LocalDateTime atualizadaEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public boolean isTemLogo() { return temLogo; }
    public void setTemLogo(boolean temLogo) { this.temLogo = temLogo; }
    public boolean isLogoClara() { return logoClara; }
    public void setLogoClara(boolean logoClara) { this.logoClara = logoClara; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
    public void setAtualizadaEm(LocalDateTime atualizadaEm) { this.atualizadaEm = atualizadaEm; }
}
