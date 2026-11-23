package com.example.sistema.academia.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "avaliacoes_fisicas")
public class AvaliacaoFisica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Aluno aluno;

    private Long professorId;
    private String registradoPor;
    private LocalDate dataAvaliacao;
    private Double peso;
    private Double altura;
    private Double percentualGordura;
    private Double cintura;
    /** Perímetro do abdômen (barriga, na altura do umbigo), em cm. Opcional: avaliações antigas ficam sem. */
    private Double abdomen;
    private Double quadril;
    private Double peito;
    private Double braco;
    private Double coxa;
    private Double ombro;
    private Double antebraco;
    private Double panturrilha;
    /** Bioimpedância (opcional): massa muscular e massa gorda do corpo inteiro, em kg. */
    private Double massaMuscularKg;
    private Double massaGordaKg;

    @Column(length = 300)
    private String observacao;

    public Double getImc() {
        if (peso == null || altura == null || altura <= 0) return null;
        return Math.round(peso / (altura * altura) * 10.0) / 10.0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }
    public Long getProfessorId() { return professorId; }
    public void setProfessorId(Long professorId) { this.professorId = professorId; }
    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }
    public LocalDate getDataAvaliacao() { return dataAvaliacao; }
    public void setDataAvaliacao(LocalDate dataAvaliacao) { this.dataAvaliacao = dataAvaliacao; }
    public Double getPeso() { return peso; }
    public void setPeso(Double peso) { this.peso = peso; }
    public Double getAltura() { return altura; }
    public void setAltura(Double altura) { this.altura = altura; }
    public Double getPercentualGordura() { return percentualGordura; }
    public void setPercentualGordura(Double percentualGordura) { this.percentualGordura = percentualGordura; }
    public Double getCintura() { return cintura; }
    public void setCintura(Double cintura) { this.cintura = cintura; }
    public Double getAbdomen() { return abdomen; }
    public void setAbdomen(Double abdomen) { this.abdomen = abdomen; }
    public Double getQuadril() { return quadril; }
    public void setQuadril(Double quadril) { this.quadril = quadril; }
    public Double getPeito() { return peito; }
    public void setPeito(Double peito) { this.peito = peito; }
    public Double getBraco() { return braco; }
    public void setBraco(Double braco) { this.braco = braco; }
    public Double getCoxa() { return coxa; }
    public void setCoxa(Double coxa) { this.coxa = coxa; }
    public Double getOmbro() { return ombro; }
    public void setOmbro(Double ombro) { this.ombro = ombro; }
    public Double getAntebraco() { return antebraco; }
    public void setAntebraco(Double antebraco) { this.antebraco = antebraco; }
    public Double getPanturrilha() { return panturrilha; }
    public void setPanturrilha(Double panturrilha) { this.panturrilha = panturrilha; }
    public Double getMassaMuscularKg() { return massaMuscularKg; }
    public void setMassaMuscularKg(Double massaMuscularKg) { this.massaMuscularKg = massaMuscularKg; }
    public Double getMassaGordaKg() { return massaGordaKg; }
    public void setMassaGordaKg(Double massaGordaKg) { this.massaGordaKg = massaGordaKg; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
