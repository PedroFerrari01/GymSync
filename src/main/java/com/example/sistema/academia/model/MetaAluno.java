package com.example.sistema.academia.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Meta do aluno: treinos por semana, peso alvo ou gordura alvo. Uma meta por tipo (salvar de novo edita). */
@Entity
@Table(name = "metas_aluno")
public class MetaAluno {

    public static final String TREINOS_SEMANA = "TREINOS_SEMANA";
    public static final String PESO = "PESO";
    public static final String GORDURA = "GORDURA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private double alvo;

    private LocalDate criadaEm;
    private LocalDate prazo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public double getAlvo() { return alvo; }
    public void setAlvo(double alvo) { this.alvo = alvo; }
    public LocalDate getCriadaEm() { return criadaEm; }
    public void setCriadaEm(LocalDate criadaEm) { this.criadaEm = criadaEm; }
    public LocalDate getPrazo() { return prazo; }
    public void setPrazo(LocalDate prazo) { this.prazo = prazo; }
}
