package com.example.sistema.academia.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Lugar na fila de espera de uma aula lotada, numa data. A ordem da fila é a ordem de criação (id). */
@Entity
@Table(name = "espera_aula")
public class EsperaAula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AulaColetiva aula;

    @ManyToOne(optional = false)
    private Aluno aluno;

    private LocalDate dataAula;
    private LocalDateTime criadaEm;
    /** Preenchido quando abriu vaga e este aluno foi avisado no sininho. */
    private LocalDateTime notificadaEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AulaColetiva getAula() { return aula; }
    public void setAula(AulaColetiva aula) { this.aula = aula; }
    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }
    public LocalDate getDataAula() { return dataAula; }
    public void setDataAula(LocalDate dataAula) { this.dataAula = dataAula; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public void setCriadaEm(LocalDateTime criadaEm) { this.criadaEm = criadaEm; }
    public LocalDateTime getNotificadaEm() { return notificadaEm; }
    public void setNotificadaEm(LocalDateTime notificadaEm) { this.notificadaEm = notificadaEm; }
}
