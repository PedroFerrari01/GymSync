package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.MuralAviso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MuralAvisoRepository extends JpaRepository<MuralAviso, Long> {
    List<MuralAviso> findByEncerradoFalseOrderByIdDesc();
}
