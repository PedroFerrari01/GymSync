package com.example.sistema.academia.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "alunos")
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String cpf;

    private String email;

    private LocalDate dataNascimento;

    private String contato;

    private String senha = "123";

    /** true quando a senha é provisória e precisa ser trocada no próximo acesso (só no modo de senhas fortes). */
    private Boolean trocarSenha;

    private String situacao = "ATIVO";

    /** Quando o aluno aceitou a política de privacidade (LGPD). Null = ainda não aceitou. */
    private LocalDateTime aceitePrivacidade;

    /** Altura em centímetros, opcional. Só serve para desenhar o mapa do corpo na proporção certa. */
    private Integer alturaCm;

    /**
     * Desenho-base do mapa do corpo: MASCULINA, FEMININA ou NAO_INFORMADA (padrão). Opcional e só visual;
     * o nome é "silhueta" de propósito, para não ser confundido com sexo ou identidade de gênero.
     */
    @Column(length = 20)
    private String silhueta = SILHUETA_NAO_INFORMADA;

    public static final String SILHUETA_MASCULINA = "MASCULINA";
    public static final String SILHUETA_FEMININA = "FEMININA";
    public static final String SILHUETA_NAO_INFORMADA = "NAO_INFORMADA";

    /** Qualquer valor fora da lista (ou vazio, ou de aluno antigo sem a coluna) vira "não informada". */
    public static String silhuetaValida(String valor) {
        if (SILHUETA_MASCULINA.equals(valor) || SILHUETA_FEMININA.equals(valor)) return valor;
        return SILHUETA_NAO_INFORMADA;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

    public String getContato() { return contato; }
    public void setContato(String contato) { this.contato = contato; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public boolean isTrocarSenha() { return Boolean.TRUE.equals(trocarSenha); }
    public void setTrocarSenha(boolean trocarSenha) { this.trocarSenha = trocarSenha; }

    public String getSituacao() { return situacao; }
    public void setSituacao(String situacao) { this.situacao = situacao; }

    public LocalDateTime getAceitePrivacidade() { return aceitePrivacidade; }
    public void setAceitePrivacidade(LocalDateTime aceitePrivacidade) { this.aceitePrivacidade = aceitePrivacidade; }

    public Integer getAlturaCm() { return alturaCm; }
    public void setAlturaCm(Integer alturaCm) { this.alturaCm = alturaCm; }

    public String getSilhueta() { return silhuetaValida(silhueta); }
    public void setSilhueta(String silhueta) { this.silhueta = silhuetaValida(silhueta); }
}