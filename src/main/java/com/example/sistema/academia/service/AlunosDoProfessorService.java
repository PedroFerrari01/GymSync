package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Mensagem;
import com.example.sistema.academia.model.Treino;
import com.example.sistema.academia.repository.MensagemRepository;
import com.example.sistema.academia.repository.TreinoRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Quais alunos um professor pode ver (LGPD: só o necessário).
 * São os alunos com ficha feita por ele e os que já conversaram com ele pelas mensagens
 * (o aluno pode escrever para qualquer professor, e o professor precisa conseguir responder).
 */
@Service
public class AlunosDoProfessorService {

    private final TreinoRepository treinoRepository;
    private final MensagemRepository mensagemRepository;

    public AlunosDoProfessorService(TreinoRepository treinoRepository, MensagemRepository mensagemRepository) {
        this.treinoRepository = treinoRepository;
        this.mensagemRepository = mensagemRepository;
    }

    public Set<Long> ids(Long professorId) {
        Set<Long> ids = new HashSet<>();
        if (professorId == null) return ids;
        for (Treino t : treinoRepository.findByProfessorId(professorId)) {
            if (t.getAluno() != null) ids.add(t.getAluno().getId());
        }
        for (Mensagem m : mensagemRepository.findByProfessorId(professorId)) {
            if (m.getAluno() != null) ids.add(m.getAluno().getId());
        }
        return ids;
    }
}
