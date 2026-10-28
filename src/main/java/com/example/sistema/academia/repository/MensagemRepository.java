package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.Mensagem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {
    List<Mensagem> findByAlunoId(Long alunoId);
    List<Mensagem> findByProfessorId(Long professorId);
    List<Mensagem> findByAlunoIdAndProfessorIdOrderByDataHoraAscIdAsc(Long alunoId, Long professorId);
    long countByAlunoIdAndRemetenteAndLidaFalse(Long alunoId, String remetente);
    long countByProfessorIdAndRemetenteAndLidaFalse(Long professorId, String remetente);
    long countByAlunoIdAndProfessorIdAndRemetenteAndLidaFalse(Long alunoId, Long professorId, String remetente);
}
