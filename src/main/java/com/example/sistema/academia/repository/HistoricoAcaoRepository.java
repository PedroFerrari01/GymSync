package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.HistoricoAcao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistoricoAcaoRepository extends JpaRepository<HistoricoAcao, Long> {
    List<HistoricoAcao> findTop50ByOrderByDataHoraDescIdDesc();
}
