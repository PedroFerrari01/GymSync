package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.ReservaAula;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservaAulaRepository extends JpaRepository<ReservaAula, Long> {
    long countByAulaIdAndDataAula(Long aulaId, LocalDate dataAula);
    List<ReservaAula> findByAulaIdAndDataAula(Long aulaId, LocalDate dataAula);
    List<ReservaAula> findByAulaId(Long aulaId);
    List<ReservaAula> findByAlunoId(Long alunoId);
    Optional<ReservaAula> findFirstByAulaIdAndAlunoIdAndDataAula(Long aulaId, Long alunoId, LocalDate dataAula);
}
