package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.TokenSenha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenSenhaRepository extends JpaRepository<TokenSenha, Long> {
    Optional<TokenSenha> findByHash(String hash);
    List<TokenSenha> findByPerfilAndUsuarioIdAndUsadoFalse(String perfil, Long usuarioId);
}
