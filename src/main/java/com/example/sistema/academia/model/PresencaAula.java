package com.example.sistema.academia.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Presença (ou falta) de um aluno numa aula coletiva em uma data. Uma marcação por aula + aluno + data. */
@Entity
@Table(name = "presencas_aula",
        uniqueConstraints = @UniqueConstraint(columnNames = {"aula_id", "aluno_id", "dataAula"}))
public class PresencaAula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AulaColetiva aula;

    @ManyToOne(optional = false)
    private Aluno aluno;

    private LocalDate dataAula;

    private boolean presente;

    /** Nome de quem marcou (professor ou administrador). */
    private String registradoPor;

    private LocalDateTime registradoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AulaColetiva getAula() { return aula; }
    public void setAula(AulaColetiva aula) { this.aula = aula; }
    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }
    public LocalDate getDataAula() { return dataAula; }
    public void setDataAula(LocalDate dataAula) { this.dataAula = dataAula; }
    public boolean isPresente() { return presente; }
    public void setPresente(boolean presente) { this.presente = presente; }
    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }
    public LocalDateTime getRegistradoEm() { return registradoEm; }
    public void setRegistradoEm(LocalDateTime registradoEm) { this.registradoEm = registradoEm; }
}
