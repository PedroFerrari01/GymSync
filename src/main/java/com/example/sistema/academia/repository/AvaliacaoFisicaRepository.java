package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.AvaliacaoFisica;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AvaliacaoFisicaRepository extends JpaRepository<AvaliacaoFisica, Long> {
    List<AvaliacaoFisica> findByAlunoId(Long alunoId);
    List<AvaliacaoFisica> findByAlunoIdOrderByDataAvaliacaoAscIdAsc(Long alunoId);
    List<AvaliacaoFisica> findAllByOrderByDataAvaliacaoDescIdDesc();
}
