package com.example.sistema.academia.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Link de redefinição de senha enviado por e-mail. Guarda só o hash do código, nunca o código em si. */
@Entity
@Table(name = "tokens_senha")
public class TokenSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String perfil;

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false, unique = true, length = 64)
    private String hash;

    @Column(nullable = false)
    private LocalDateTime expiraEm;

    private boolean usado;

    public Long getId() { return id; }

    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public LocalDateTime getExpiraEm() { return expiraEm; }
    public void setExpiraEm(LocalDateTime expiraEm) { this.expiraEm = expiraEm; }

    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
}
