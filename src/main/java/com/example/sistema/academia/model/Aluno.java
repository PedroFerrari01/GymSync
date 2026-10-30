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
}