package com.example.sistema.academia.repository;

import com.example.sistema.academia.model.EquipamentoImagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface EquipamentoImagemRepository extends JpaRepository<EquipamentoImagem, Long> {

    /** Só os ids, para a lista não carregar as fotos inteiras. */
    @Query("select i.equipamentoId from EquipamentoImagem i")
    List<Long> idsComImagem();
}
