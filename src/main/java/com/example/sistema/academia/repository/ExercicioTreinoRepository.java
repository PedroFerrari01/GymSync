package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.ExercicioTreino;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExercicioTreinoRepository extends JpaRepository<ExercicioTreino, Long> {
    List<ExercicioTreino> findByTreinoId(Long treinoId);
    List<ExercicioTreino> findByTreinoAlunoId(Long alunoId);
    List<ExercicioTreino> findByTreinoProfessorId(Long professorId);
}
