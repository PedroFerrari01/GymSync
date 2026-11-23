package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.MetaProgresso;
import com.example.sistema.academia.model.AvaliacaoFisica;
import com.example.sistema.academia.model.Entrada;
import com.example.sistema.academia.model.MetaAluno;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetasServiceTest {

    private final MetasService servico = new MetasService();
    // 2026-11-18 é quarta-feira; a semana começa na segunda 2026-11-16
    private final LocalDate hoje = LocalDate.of(2026, 11, 18);

    private MetaAluno meta(String tipo, double alvo, LocalDate prazo) {
        MetaAluno m = new MetaAluno();
        m.setId(1L);
        m.setTipo(tipo);
        m.setAlvo(alvo);
        m.setPrazo(prazo);
        return m;
    }

    private AvaliacaoFisica av(Double peso, Double gordura) {
        AvaliacaoFisica a = new AvaliacaoFisica();
        a.setPeso(peso);
        a.setPercentualGordura(gordura);
        return a;
    }

    private Entrada entrada(LocalDateTime quando) {
        Entrada e = new Entrada();
        e.setDataHoraEntrada(quando);
        return e;
    }

    @Test
    void treinosDaSemanaContaDiasDistintosDesdeASegunda() {
        List<Entrada> es = List.of(
                entrada(LocalDateTime.of(2026, 11, 15, 18, 0)),  // domingo anterior: fora
                entrada(LocalDateTime.of(2026, 11, 16, 7, 0)),
                entrada(LocalDateTime.of(2026, 11, 16, 19, 0)),  // mesmo dia: conta 1
                entrada(LocalDateTime.of(2026, 11, 18, 8, 0)));
        assertEquals(2, MetasService.treinosDaSemana(es, hoje));
        assertEquals(0, MetasService.treinosDaSemana(List.of(), hoje));
        assertEquals(0, MetasService.treinosDaSemana(null, hoje));
    }

    @Test
    void treinosProgressoZeroMetadeCompletoEPassouDoAlvo() {
        MetaAluno m = meta(MetaAluno.TREINOS_SEMANA, 4, null);
        assertEquals(0, servico.calcular(List.of(m), 0, List.of(), hoje).get(0).percentual());
        assertEquals(50, servico.calcular(List.of(m), 2, List.of(), hoje).get(0).percentual());
        MetaProgresso completo = servico.calcular(List.of(m), 4, List.of(), hoje).get(0);
        assertEquals(100, completo.percentual());
        assertTrue(completo.concluida());
        MetaProgresso passou = servico.calcular(List.of(m), 6, List.of(), hoje).get(0);
        assertEquals(100, passou.percentual());
        assertTrue(passou.concluida());
    }

    @Test
    void pesoUsaPrimeiraEUltimaAvaliacaoRumoAoAlvo() {
        MetaAluno m = meta(MetaAluno.PESO, 62, null);
        List<AvaliacaoFisica> avs = List.of(av(68.0, 27.0), av(66.0, 26.0), av(65.0, 25.0));
        MetaProgresso p = servico.calcular(List.of(m), 0, avs, hoje).get(0);
        assertEquals(50, p.percentual()); // 68 -> 65 de 68 -> 62
        assertFalse(p.concluida());
    }

    @Test
    void pesoPassouDoAlvoEPesoSemMudancaEPesoPiorou() {
        MetaAluno m = meta(MetaAluno.PESO, 62, null);
        assertEquals(100, servico.calcular(List.of(m), 0, List.of(av(68.0, null), av(60.0, null)), hoje).get(0).percentual());
        assertEquals(0, servico.calcular(List.of(m), 0, List.of(av(68.0, null), av(68.0, null)), hoje).get(0).percentual());
        assertEquals(0, servico.calcular(List.of(m), 0, List.of(av(68.0, null), av(70.0, null)), hoje).get(0).percentual());
    }

    @Test
    void ganharPesoComoAlvoTambemFunciona() {
        assertEquals(50, MetasService.percentualRumoAoAlvo(60, 65, 70));
        assertEquals(100, MetasService.percentualRumoAoAlvo(60, 72, 70));
    }

    @Test
    void gorduraComUmaAvaliacaoTemZeroPorCento() {
        MetaAluno m = meta(MetaAluno.GORDURA, 22, null);
        MetaProgresso p = servico.calcular(List.of(m), 0, List.of(av(68.0, 27.5)), hoje).get(0);
        assertEquals(0, p.percentual()); // inicio == atual, alvo diferente
        assertFalse(p.semDados());
    }

    @Test
    void semAvaliacaoNaoMarcaComoConcluida() {
        MetaAluno m = meta(MetaAluno.GORDURA, 22, null);
        MetaProgresso p = servico.calcular(List.of(m), 0, List.of(), hoje).get(0);
        assertTrue(p.semDados());
        assertEquals(0, p.percentual());
        assertFalse(p.concluida());
        // avaliação sem o campo (null) também é "sem dados"
        assertTrue(servico.calcular(List.of(m), 0, List.of(av(70.0, null)), hoje).get(0).semDados());
    }

    @Test
    void prazoVencidoESemPrazo() {
        MetaAluno vencida = meta(MetaAluno.TREINOS_SEMANA, 4, hoje.minusDays(1));
        assertTrue(servico.calcular(List.of(vencida), 1, List.of(), hoje).get(0).atrasada());
        MetaAluno futura = meta(MetaAluno.TREINOS_SEMANA, 4, hoje.plusDays(3));
        MetaProgresso f = servico.calcular(List.of(futura), 1, List.of(), hoje).get(0);
        assertFalse(f.atrasada());
        assertEquals("Até 21/11/2026", f.prazoTexto());
        assertNull(servico.calcular(List.of(meta(MetaAluno.PESO, 60, null)), 0, List.of(), hoje).get(0).prazoTexto());
    }

    @Test
    void validacaoDosAlvos() {
        assertNull(MetasService.validar("TREINOS_SEMANA", 4.0, null, hoje));
        assertNotNull(MetasService.validar("TREINOS_SEMANA", 0.0, null, hoje));
        assertNotNull(MetasService.validar("TREINOS_SEMANA", 8.0, null, hoje));
        assertNotNull(MetasService.validar("TREINOS_SEMANA", 3.5, null, hoje));
        assertNull(MetasService.validar("PESO", 62.5, hoje, hoje));
        assertNotNull(MetasService.validar("PESO", 10.0, null, hoje));
        assertNotNull(MetasService.validar("GORDURA", 80.0, null, hoje));
        assertNotNull(MetasService.validar("GORDURA", 20.0, hoje.minusDays(1), hoje));
        assertNotNull(MetasService.validar("OUTRA", 20.0, null, hoje));
        assertNotNull(MetasService.validar("PESO", null, null, hoje));
        assertNotNull(MetasService.validar("PESO", Double.NaN, null, hoje));
    }
}
