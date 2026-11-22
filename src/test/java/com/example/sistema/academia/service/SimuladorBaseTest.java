package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.Matricula;
import com.example.sistema.academia.model.Mensalidade;
import com.example.sistema.academia.model.Plano;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Base do simulador de faturamento: ativos, ticket médio, receita mensal e % de inadimplência. */
class SimuladorBaseTest {

    private final RelatorioService service = new RelatorioService();

    private Matricula matricula(Long idAluno, String situacao, String valor) {
        Aluno a = new Aluno();
        ReflectionTestUtils.setField(a, "id", idAluno);
        Plano p = new Plano();
        p.setValor(valor == null ? null : new BigDecimal(valor));
        Matricula m = new Matricula();
        m.setAluno(a);
        m.setPlano(p);
        m.setSituacao(situacao);
        return m;
    }

    @Test
    void calculaBaseComMatriculasAtivas() {
        Matricula a = matricula(1L, "ATIVA", "100.00");
        Matricula b = matricula(2L, "ATIVA", "200.00");
        Matricula c = matricula(3L, "VENCIDA", "500.00");
        Mensalidade atrasada = new Mensalidade();
        atrasada.setMatricula(b);
        atrasada.setSituacao("ATRASADA");

        RelatorioService.BaseSimulador base = service.baseSimulador(List.of(a, b, c), List.of(atrasada));

        assertEquals(2, base.ativos());
        assertEquals(new BigDecimal("150.00"), base.ticket());
        assertEquals(new BigDecimal("300.00"), base.receita());
        assertEquals(50.0, base.inadimplenciaPct());
    }

    @Test
    void semMatriculasNaoDividePorZeroENaoQuebraComPlanoSemValor() {
        RelatorioService.BaseSimulador vazio = service.baseSimulador(List.of(), List.of());
        assertEquals(0, vazio.ativos());
        assertEquals(0, vazio.ticket().signum());
        assertEquals(0.0, vazio.inadimplenciaPct());

        RelatorioService.BaseSimulador semValor = service.baseSimulador(List.of(matricula(1L, "ATIVA", null)), List.of());
        assertEquals(0, semValor.ativos());
    }
}
