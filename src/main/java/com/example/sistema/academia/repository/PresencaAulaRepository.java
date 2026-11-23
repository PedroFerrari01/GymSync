package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.PresencaAula;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PresencaAulaRepository extends JpaRepository<PresencaAula, Long> {
    List<PresencaAula> findByAulaIdAndDataAula(Long aulaId, LocalDate dataAula);
    List<PresencaAula> findByAulaId(Long aulaId);
    List<PresencaAula> findByAlunoId(Long alunoId);
    List<PresencaAula> findByDataAulaGreaterThanEqual(LocalDate data);
    Optional<PresencaAula> findFirstByAulaIdAndAlunoIdAndDataAula(Long aulaId, Long alunoId, LocalDate dataAula);
}
