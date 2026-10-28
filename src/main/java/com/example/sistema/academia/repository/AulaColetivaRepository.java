package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.AulaColetiva;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AulaColetivaRepository extends JpaRepository<AulaColetiva, Long> {
    List<AulaColetiva> findByProfessorId(Long professorId);
}
