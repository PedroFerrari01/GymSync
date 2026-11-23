package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.Mensagem;
import com.example.sistema.academia.model.Treino;
import com.example.sistema.academia.repository.MensagemRepository;
import com.example.sistema.academia.repository.TreinoRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlunosDoProfessorServiceTest {

    private static Aluno aluno(long id) {
        Aluno a = new Aluno();
        a.setId(id);
        return a;
    }

    @Test
    void juntaAlunosDaFichaEDasConversasSemRepetir() {
        TreinoRepository treinos = mock(TreinoRepository.class);
        MensagemRepository mensagens = mock(MensagemRepository.class);
        Treino t1 = new Treino();
        t1.setAluno(aluno(1));
        Treino t2 = new Treino();
        t2.setAluno(aluno(2));
        Mensagem m = new Mensagem();
        m.setAluno(aluno(2));
        Mensagem m2 = new Mensagem();
        m2.setAluno(aluno(5));
        when(treinos.findByProfessorId(10L)).thenReturn(List.of(t1, t2));
        when(mensagens.findByProfessorId(10L)).thenReturn(List.of(m, m2));

        Set<Long> ids = new AlunosDoProfessorService(treinos, mensagens).ids(10L);

        assertEquals(Set.of(1L, 2L, 5L), ids);
    }

    @Test
    void mapaDoCorpoSoComAvaliacoesDosAlunosDoProfessor() {
        com.example.sistema.academia.model.AvaliacaoFisica meu = new com.example.sistema.academia.model.AvaliacaoFisica();
        meu.setAluno(aluno(1));
        com.example.sistema.academia.model.AvaliacaoFisica alheio = new com.example.sistema.academia.model.AvaliacaoFisica();
        alheio.setAluno(aluno(9));
        com.example.sistema.academia.model.AvaliacaoFisica semAluno = new com.example.sistema.academia.model.AvaliacaoFisica();

        List<com.example.sistema.academia.model.AvaliacaoFisica> r =
                AlunosDoProfessorService.apenasDeles(List.of(meu, alheio, semAluno), Set.of(1L));

        assertEquals(List.of(meu), r);
        assertTrue(AlunosDoProfessorService.apenasDeles(List.of(alheio), Set.of()).isEmpty());
    }

    @Test
    void professorSemFichaNemConversaNaoVeNinguem() {
        TreinoRepository treinos = mock(TreinoRepository.class);
        MensagemRepository mensagens = mock(MensagemRepository.class);
        when(treinos.findByProfessorId(11L)).thenReturn(List.of());
        when(mensagens.findByProfessorId(11L)).thenReturn(List.of());

        assertTrue(new AlunosDoProfessorService(treinos, mensagens).ids(11L).isEmpty());
        assertTrue(new AlunosDoProfessorService(treinos, mensagens).ids(null).isEmpty());
    }
}
