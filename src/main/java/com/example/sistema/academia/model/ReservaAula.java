package com.example.sistema.academia.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "reservas_aula")
public class ReservaAula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AulaColetiva aula;

    @ManyToOne(optional = false)
    private Aluno aluno;

    private LocalDate dataAula;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AulaColetiva getAula() { return aula; }
    public void setAula(AulaColetiva aula) { this.aula = aula; }
    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }
    public LocalDate getDataAula() { return dataAula; }
    public void setDataAula(LocalDate dataAula) { this.dataAula = dataAula; }
}
