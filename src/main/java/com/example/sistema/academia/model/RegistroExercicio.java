package com.example.sistema.academia.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "registros_exercicio")
public class RegistroExercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private ExercicioTreino exercicio;

    @ManyToOne(optional = false)
    private Aluno aluno;

    private LocalDate dataRegistro;
    private Double carga;

    @Column(length = 200)
    private String observacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ExercicioTreino getExercicio() { return exercicio; }
    public void setExercicio(ExercicioTreino exercicio) { this.exercicio = exercicio; }
    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }
    public LocalDate getDataRegistro() { return dataRegistro; }
    public void setDataRegistro(LocalDate dataRegistro) { this.dataRegistro = dataRegistro; }
    public Double getCarga() { return carga; }
    public void setCarga(Double carga) { this.carga = carga; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
