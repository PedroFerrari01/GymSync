package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.Professor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
    Optional<Professor> findFirstByCpfOrEmailIgnoreCase(String cpf, String email);
    Optional<Professor> findFirstByCpfAndEmailIgnoreCase(String cpf, String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
