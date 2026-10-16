package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {
    Optional<Aluno> findFirstByCpfOrEmailIgnoreCase(String cpf, String email);
    Optional<Aluno> findFirstByCpfAndEmailIgnoreCase(String cpf, String email);
    boolean existsByEmailIgnoreCase(String email);
}
