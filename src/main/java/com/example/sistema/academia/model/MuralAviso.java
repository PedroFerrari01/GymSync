package com.example.sistema.academia.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Aviso curto da academia ("Feriado: abre às 8h"). Encerrar não apaga: só tira do mural. */
@Entity
@Table(name = "mural_avisos")
public class MuralAviso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String titulo;

    @Column(nullable = false, length = 300)
    private String texto;

    private LocalDate dataPublicacao;
    private LocalDate dataTermino;
    private boolean encerrado;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public LocalDate getDataPublicacao() { return dataPublicacao; }
    public void setDataPublicacao(LocalDate dataPublicacao) { this.dataPublicacao = dataPublicacao; }
    public LocalDate getDataTermino() { return dataTermino; }
    public void setDataTermino(LocalDate dataTermino) { this.dataTermino = dataTermino; }
    public boolean isEncerrado() { return encerrado; }
    public void setEncerrado(boolean encerrado) { this.encerrado = encerrado; }
}
