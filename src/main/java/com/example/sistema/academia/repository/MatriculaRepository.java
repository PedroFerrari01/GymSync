package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    List<Matricula> findByAlunoId(Long alunoId);
    List<Matricula> findByPlanoId(Long planoId);
    boolean existsByAlunoIdAndSituacao(Long alunoId, String situacao);
    List<Matricula> findBySituacaoAndDataFimBefore(String situacao, LocalDate data);
    List<Matricula> findBySituacaoAndDataFimBetween(String situacao, LocalDate inicio, LocalDate fim);
}
