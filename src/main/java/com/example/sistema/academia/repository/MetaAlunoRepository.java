package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.MetaAluno;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MetaAlunoRepository extends JpaRepository<MetaAluno, Long> {
    List<MetaAluno> findByAlunoIdOrderByIdAsc(Long alunoId);
    List<MetaAluno> findByAlunoId(Long alunoId);
}
