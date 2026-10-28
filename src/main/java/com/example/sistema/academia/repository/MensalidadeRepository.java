package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.Mensalidade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MensalidadeRepository extends JpaRepository<Mensalidade, Long> {
    List<Mensalidade> findByMatriculaId(Long matriculaId);
    List<Mensalidade> findByMatriculaAlunoId(Long alunoId);
    List<Mensalidade> findByMatriculaPlanoId(Long planoId);
    List<Mensalidade> findBySituacao(String situacao);
    List<Mensalidade> findBySituacaoAndVencimentoBefore(String situacao, LocalDate data);
    boolean existsByMatriculaAlunoIdAndSituacao(Long alunoId, String situacao);
}
