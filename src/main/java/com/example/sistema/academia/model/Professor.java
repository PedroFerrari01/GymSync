package com.example.sistema.academia.model;

import jakarta.persistence.*;

@Entity
@Table(name = "professores")
public class Professor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String cpf;

    private String email;

    private String contato;

    private String especialidade;

    private String senha = "123";

    /** true quando a senha é provisória e precisa ser trocada no próximo acesso (só no modo de senhas fortes). */
    private Boolean trocarSenha;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContato() { return contato; }
    public void setContato(String contato) { this.contato = contato; }

    public String getEspecialidade() { return especialidade; }
    public void setEspecialidade(String especialidade) { this.especialidade = especialidade; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public boolean isTrocarSenha() { return Boolean.TRUE.equals(trocarSenha); }
    public void setTrocarSenha(boolean trocarSenha) { this.trocarSenha = trocarSenha; }
}