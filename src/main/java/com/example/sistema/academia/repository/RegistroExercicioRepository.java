package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.RegistroExercicio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RegistroExercicioRepository extends JpaRepository<RegistroExercicio, Long> {
    List<RegistroExercicio> findByAlunoId(Long alunoId);
    List<RegistroExercicio> findByExercicioId(Long exercicioId);
    List<RegistroExercicio> findByExercicioTreinoId(Long treinoId);
}
