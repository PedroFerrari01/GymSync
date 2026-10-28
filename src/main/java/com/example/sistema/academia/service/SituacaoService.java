package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Matricula;
import com.example.sistema.academia.model.Mensalidade;
import com.example.sistema.academia.repository.MatriculaRepository;
import com.example.sistema.academia.repository.MensalidadeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class SituacaoService {

    @Autowired private MensalidadeRepository mensalidadeRepository;
    @Autowired private MatriculaRepository matriculaRepository;

    public void atualizar() {
        LocalDate hoje = LocalDate.now();

        List<Mensalidade> atrasadas = mensalidadeRepository.findBySituacaoAndVencimentoBefore("PENDENTE", hoje);
        atrasadas.forEach(m -> m.setSituacao("ATRASADA"));
        mensalidadeRepository.saveAll(atrasadas);

        List<Matricula> vencidas = matriculaRepository.findBySituacaoAndDataFimBefore("ATIVA", hoje);
        vencidas.forEach(m -> m.setSituacao("VENCIDA"));
        matriculaRepository.saveAll(vencidas);
    }
}
