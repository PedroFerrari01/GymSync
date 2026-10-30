package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Acompanhamento;
import com.example.sistema.academia.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcompanhamentoServiceTest {

    private final AcompanhamentoService service = new AcompanhamentoService();
    private final LocalDate hoje = LocalDate.of(2026, 10, 30); // sexta; a semana começou na segunda, 26/10

    private Aluno aluno(long id, String nome, String situacao) {
        Aluno a = new Aluno();
        a.setId(id);
        a.setNome(nome);
        a.setSituacao(situacao);
        return a;
    }

    private Treino treino(long id, Aluno aluno) {
        Treino t = new Treino();
        t.setId(id);
        t.setAluno(aluno);
        return t;
    }

    private ExercicioTreino exercicio(long id, Treino treino) {
        ExercicioTreino ex = new ExercicioTreino();
        ex.setId(id);
        ex.setTreino(treino);
        return ex;
    }

    private Entrada entrada(Aluno aluno, LocalDate dia) {
        Entrada e = new Entrada();
        e.setAluno(aluno);
        e.setDataHoraEntrada(dia.atTime(8, 0));
        return e;
    }

    private RegistroExercicio registro(Aluno aluno, ExercicioTreino ex, LocalDate dia) {
        RegistroExercicio r = new RegistroExercicio();
        r.setAluno(aluno);
        r.setExercicio(ex);
        r.setDataRegistro(dia);
        return r;
    }

    @Test
    void calculaSumidosEAdesaoDaSemana() {
        Aluno ana = aluno(1, "Ana", "ATIVO");
        Aluno bruno = aluno(2, "Bruno", "ATIVO");
        Aluno carla = aluno(3, "Carla", "ATIVO");
        Aluno inativo = aluno(4, "Diego", "INATIVO");
        Treino ta = treino(10, ana), tb = treino(11, bruno), tc = treino(12, carla), td = treino(13, inativo);
        ExercicioTreino supino = exercicio(100, ta), remada = exercicio(101, ta), agacho = exercicio(102, ta), rosca = exercicio(103, ta);
        ExercicioTreino legBruno = exercicio(110, tb);

        List<Acompanhamento> linhas = service.montar(
                List.of(ta, tb, tc, td),
                List.of(supino, remada, agacho, rosca, legBruno),
                List.of(entrada(ana, hoje.minusDays(1)), entrada(ana, hoje.minusDays(3)), entrada(ana, hoje.minusDays(8)),
                        entrada(bruno, hoje.minusDays(12))),
                List.of(registro(ana, supino, hoje.minusDays(1)), registro(ana, supino, hoje.minusDays(3)), // mesmo exercício 2x conta 1
                        registro(ana, remada, hoje.minusDays(3)),
                        registro(ana, agacho, hoje.minusDays(8))), // semana passada não conta
                hoje);

        // Inativo fica de fora; quem nunca veio primeiro, depois quem está há mais tempo sem vir
        assertEquals(List.of("Carla", "Bruno", "Ana"), linhas.stream().map(Acompanhamento::getNome).toList());

        Acompanhamento carlaL = linhas.get(0);
        assertNull(carlaL.getDiasSemTreinar());
        assertTrue(carlaL.isSumido());
        assertEquals("Nunca veio", carlaL.getUltimaVisita());
        assertEquals(0, carlaL.getTotalExercicios());

        Acompanhamento brunoL = linhas.get(1);
        assertEquals(12, brunoL.getDiasSemTreinar());
        assertTrue(brunoL.isSumido());
        assertEquals(0, brunoL.getEntradasSemana());

        Acompanhamento anaL = linhas.get(2);
        assertFalse(anaL.isSumido());
        assertEquals("Ontem", anaL.getUltimaVisita());
        assertEquals(2, anaL.getEntradasSemana());
        assertEquals(2, anaL.getFeitosSemana());
        assertEquals(4, anaL.getTotalExercicios());
        assertEquals(50, anaL.getAdesao());
    }

    @Test
    void dezDiasJaContaComoSumido() {
        Aluno ana = aluno(1, "Ana", "ATIVO");
        Treino t = treino(10, ana);
        assertTrue(service.montar(List.of(t), List.of(), List.of(entrada(ana, hoje.minusDays(10))), List.of(), hoje).get(0).isSumido());
        assertFalse(service.montar(List.of(t), List.of(), List.of(entrada(ana, hoje.minusDays(9))), List.of(), hoje).get(0).isSumido());
    }
}
