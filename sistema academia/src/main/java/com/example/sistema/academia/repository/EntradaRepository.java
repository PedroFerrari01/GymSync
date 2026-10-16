package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.Entrada;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {
    List<Entrada> findByAlunoId(Long alunoId);
    List<Entrada> findByAlunoIdOrderByDataHoraEntradaDesc(Long alunoId);
    List<Entrada> findAllByOrderByDataHoraEntradaDesc();
}
