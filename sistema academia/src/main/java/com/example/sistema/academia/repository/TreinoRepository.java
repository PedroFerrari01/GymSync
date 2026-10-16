package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.Treino;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TreinoRepository extends JpaRepository<Treino, Long> {
    List<Treino> findByAlunoId(Long alunoId);
    List<Treino> findByProfessorId(Long professorId);
}
