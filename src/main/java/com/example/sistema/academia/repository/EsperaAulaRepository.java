package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.EsperaAula;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EsperaAulaRepository extends JpaRepository<EsperaAula, Long> {
    List<EsperaAula> findByAulaIdAndDataAulaOrderByIdAsc(Long aulaId, LocalDate dataAula);
    Optional<EsperaAula> findFirstByAulaIdAndAlunoIdAndDataAula(Long aulaId, Long alunoId, LocalDate dataAula);
    List<EsperaAula> findByDataAulaBetweenOrderByIdAsc(LocalDate de, LocalDate ate);
    List<EsperaAula> findByAlunoId(Long alunoId);
    List<EsperaAula> findByAulaId(Long aulaId);
}
