package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.Matricula;
import com.example.sistema.academia.model.Mensalidade;
import com.example.sistema.academia.repository.MatriculaRepository;
import com.example.sistema.academia.repository.MensalidadeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Trancamento de matrícula: regras puras (sem banco) e o congelamento no SituacaoService. */
class TrancamentoServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 20);

    private static Matricula matricula(String situacao) {
        Matricula m = new Matricula();
        m.setSituacao(situacao);
        m.setDataInicio(HOJE.minusDays(60));
        m.setDataFim(HOJE.plusMonths(6));
        Aluno a = new Aluno();
        a.setNome("Ana Souza");
        m.setAluno(a);
        return m;
    }

    private static Mensalidade mensalidade(Matricula m, String situacao, LocalDate vencimento) {
        Mensalidade men = new Mensalidade();
        men.setMatricula(m);
        men.setSituacao(situacao);
        men.setVencimento(vencimento);
        return men;
    }

    @Test
    void trancaMatriculaAtivaSemAtraso() {
        Matricula m = matricula("ATIVA");
        List<Mensalidade> mens = List.of(mensalidade(m, "PAGA", HOJE.minusDays(20)), mensalidade(m, "PENDENTE", HOJE.plusDays(10)));
        TrancamentoService.Resultado r = TrancamentoService.trancar(m, mens, HOJE, HOJE.plusDays(30), "  Viagem ", HOJE);
        assertTrue(r.ok(), r.erro());
        assertEquals("TRANCADA", m.getSituacao());
        assertEquals(HOJE, m.getTrancadaDesde());
        assertEquals(HOJE.plusDays(30), m.getTrancadaAte());
        assertEquals("Viagem", m.getMotivoTrancamento());
    }

    @Test
    void retornoEMotivoSaoOpcionaisEInicioVazioUsaHoje() {
        Matricula m = matricula("ATIVA");
        assertTrue(TrancamentoService.trancar(m, List.of(), null, null, null, HOJE).ok());
        assertEquals(HOJE, m.getTrancadaDesde());
        assertNull(m.getTrancadaAte());
        assertNull(m.getMotivoTrancamento());
    }

    @Test
    void recusaQuandoHaMensalidadeAtrasada() {
        Matricula m = matricula("ATIVA");
        List<Mensalidade> mens = List.of(mensalidade(m, "ATRASADA", HOJE.minusDays(3)));
        TrancamentoService.Resultado r = TrancamentoService.trancar(m, mens, HOJE, null, null, HOJE);
        assertFalse(r.ok());
        assertTrue(r.erro().contains("atraso"));
        assertEquals("ATIVA", m.getSituacao());
        assertNull(m.getTrancadaDesde());
    }

    @Test
    void recusaPendenteJaVencidaMesmoSemTerSidoMarcadaAtrasada() {
        Matricula m = matricula("ATIVA");
        List<Mensalidade> mens = List.of(mensalidade(m, "PENDENTE", HOJE.minusDays(1)));
        assertFalse(TrancamentoService.trancar(m, mens, HOJE, null, null, HOJE).ok());
        assertEquals("ATIVA", m.getSituacao());
    }

    @Test
    void pendenteQueVenceHojeNaoImpedeOTrancamento() {
        Matricula m = matricula("ATIVA");
        List<Mensalidade> mens = List.of(mensalidade(m, "PENDENTE", HOJE));
        assertTrue(TrancamentoService.trancar(m, mens, HOJE, null, null, HOJE).ok());
    }

    @Test
    void duploTrancamentoERecusado() {
        Matricula m = matricula("ATIVA");
        assertTrue(TrancamentoService.trancar(m, List.of(), HOJE, null, null, HOJE).ok());
        TrancamentoService.Resultado r = TrancamentoService.trancar(m, List.of(), HOJE, null, null, HOJE);
        assertFalse(r.ok());
        assertTrue(r.erro().contains("já está trancada"));
    }

    @Test
    void soTrancaMatriculaAtiva() {
        for (String s : new String[]{"CANCELADA", "VENCIDA"}) {
            Matricula m = matricula(s);
            assertFalse(TrancamentoService.trancar(m, List.of(), HOJE, null, null, HOJE).ok(), s);
            assertEquals(s, m.getSituacao());
        }
        assertFalse(TrancamentoService.trancar(null, List.of(), HOJE, null, null, HOJE).ok());
    }

    @Test
    void periodoInvalidoERecusadoSemMudarNada() {
        Matricula m = matricula("ATIVA");
        assertFalse(TrancamentoService.trancar(m, List.of(), HOJE.plusDays(1), null, null, HOJE).ok(), "início no futuro");
        assertFalse(TrancamentoService.trancar(m, List.of(), HOJE.minusDays(31), null, null, HOJE).ok(), "início muito antigo");
        assertFalse(TrancamentoService.trancar(m, List.of(), HOJE.minusDays(61), null, null, HOJE).ok(), "antes da matrícula");
        assertFalse(TrancamentoService.trancar(m, List.of(), HOJE, HOJE, null, HOJE).ok(), "retorno igual ao início");
        assertFalse(TrancamentoService.trancar(m, List.of(), HOJE, HOJE.minusDays(2), null, HOJE).ok(), "retorno antes do início");
        assertFalse(TrancamentoService.trancar(m, List.of(), HOJE, HOJE.plusDays(366), null, HOJE).ok(), "retorno longe demais");
        assertFalse(TrancamentoService.trancar(m, List.of(), HOJE, null, "x".repeat(201), HOJE).ok(), "motivo longo");
        assertEquals("ATIVA", m.getSituacao());
        assertNull(m.getTrancadaDesde());
    }

    @Test
    void destrancarDesloncaFimEVencimentosPendentesPelosDiasTrancados() {
        Matricula m = matricula("ATIVA");
        Mensalidade paga = mensalidade(m, "PAGA", HOJE.minusDays(5));
        Mensalidade analise = mensalidade(m, "EM_ANALISE", HOJE.plusDays(3));
        Mensalidade pend1 = mensalidade(m, "PENDENTE", HOJE.plusDays(10));
        Mensalidade pend2 = mensalidade(m, "PENDENTE", HOJE.plusDays(40));
        List<Mensalidade> mens = List.of(paga, analise, pend1, pend2);
        LocalDate fimAntes = m.getDataFim();

        assertTrue(TrancamentoService.trancar(m, mens, HOJE, HOJE.plusDays(20), "Lesão", HOJE).ok());
        LocalDate volta = HOJE.plusDays(15); // voltou antes do previsto: contam os dias realmente trancados (15)
        List<Mensalidade> alteradas = new ArrayList<>();
        TrancamentoService.Resultado r = TrancamentoService.destrancar(m, mens, volta, alteradas);

        assertTrue(r.ok(), r.erro());
        assertEquals("ATIVA", m.getSituacao());
        assertEquals(fimAntes.plusDays(15), m.getDataFim());
        assertEquals(HOJE.plusDays(25), pend1.getVencimento());
        assertEquals(HOJE.plusDays(55), pend2.getVencimento());
        assertEquals(HOJE.minusDays(5), paga.getVencimento(), "paga não muda");
        assertEquals(HOJE.plusDays(3), analise.getVencimento(), "em análise não muda");
        assertEquals(List.of(pend1, pend2), alteradas);
        assertEquals(volta, m.getTrancadaAte(), "registro: voltou nesse dia");
        assertEquals(HOJE, m.getTrancadaDesde());
    }

    @Test
    void destrancarNoMesmoDiaNaoDesloca() {
        Matricula m = matricula("ATIVA");
        Mensalidade pend = mensalidade(m, "PENDENTE", HOJE.plusDays(10));
        LocalDate fim = m.getDataFim();
        TrancamentoService.trancar(m, List.of(pend), HOJE, null, null, HOJE);
        List<Mensalidade> alteradas = new ArrayList<>();
        assertTrue(TrancamentoService.destrancar(m, List.of(pend), HOJE, alteradas).ok());
        assertEquals(fim, m.getDataFim());
        assertEquals(HOJE.plusDays(10), pend.getVencimento());
        assertTrue(alteradas.isEmpty());
    }

    @Test
    void naoDestrancaQuemNaoEstaTrancadaNemDuasVezes() {
        Matricula m = matricula("ATIVA");
        assertFalse(TrancamentoService.destrancar(m, List.of(), HOJE, null).ok());
        TrancamentoService.trancar(m, List.of(), HOJE, null, null, HOJE);
        assertTrue(TrancamentoService.destrancar(m, List.of(), HOJE, null).ok());
        assertFalse(TrancamentoService.destrancar(m, List.of(), HOJE, null).ok());
        assertFalse(TrancamentoService.destrancar(null, List.of(), HOJE, null).ok());
    }

    @Test
    void catracaBloqueiaTrancadaComADataDeRetorno() {
        Matricula m = matricula("ATIVA");
        TrancamentoService.trancar(m, List.of(), HOJE, LocalDate.of(2026, 12, 5), null, HOJE);
        String motivo = TrancamentoService.motivoCatraca(m);
        assertEquals("está com a matrícula trancada até 05/12", motivo);
        Aluno a = new Aluno();
        a.setSituacao("ATIVO");
        assertEquals(motivo, CarteirinhaService.motivoBloqueio(a, false, false, motivo));
        // Sem data de retorno e matrícula ativa (não trancada): regras antigas continuam
        m.setTrancadaAte(null);
        assertEquals("está com a matrícula trancada", TrancamentoService.motivoCatraca(m));
        assertNull(TrancamentoService.motivoCatraca(matricula("ATIVA")));
        assertNull(CarteirinhaService.motivoBloqueio(a, true, false, null));
        assertEquals("está com mensalidade em atraso", CarteirinhaService.motivoBloqueio(a, true, true, null));
        a.setSituacao("INATIVO");
        assertEquals("está com cadastro INATIVO", CarteirinhaService.motivoBloqueio(a, false, false, motivo));
    }

    @Test
    void avisosDoAlunoEDoAdmin() {
        Matricula m = matricula("ATIVA");
        assertNull(TrancamentoService.avisoAluno(m));
        TrancamentoService.trancar(m, List.of(), HOJE, LocalDate.of(2026, 11, 25), "x", HOJE);
        assertEquals("Sua matrícula está trancada até 25/11.", TrancamentoService.avisoAluno(m));
        assertTrue(TrancamentoService.retornoProximo(m, HOJE));             // volta em 5 dias
        assertFalse(TrancamentoService.retornoProximo(m, HOJE.minusDays(5))); // volta em 10 dias
        assertTrue(TrancamentoService.retornoProximo(m, HOJE.plusDays(30)));  // atrasado: ainda trancada
        assertTrue(TrancamentoService.textoRetorno(m, HOJE).contains("volta em 5 dias"));
        m.setTrancadaAte(null);
        assertFalse(TrancamentoService.retornoProximo(m, HOJE), "sem retorno previsto não avisa");
        assertTrue(TrancamentoService.avisoAluno(m).contains("recepção"));
    }

    @Test
    void situacaoServiceNaoAtrasaMensalidadeDeMatriculaTrancada() {
        MensalidadeRepository mensalidades = mock(MensalidadeRepository.class);
        MatriculaRepository matriculas = mock(MatriculaRepository.class);
        SituacaoService situacao = new SituacaoService();
        ReflectionTestUtils.setField(situacao, "mensalidadeRepository", mensalidades);
        ReflectionTestUtils.setField(situacao, "matriculaRepository", matriculas);

        Matricula trancada = matricula("TRANCADA");
        Matricula ativa = matricula("ATIVA");
        Mensalidade congelada = mensalidade(trancada, "PENDENTE", LocalDate.now().minusDays(10));
        Mensalidade normal = mensalidade(ativa, "PENDENTE", LocalDate.now().minusDays(1));
        when(mensalidades.findBySituacaoAndVencimentoBefore("PENDENTE", LocalDate.now())).thenReturn(List.of(congelada, normal));
        when(matriculas.findBySituacaoAndDataFimBefore("ATIVA", LocalDate.now())).thenReturn(List.of());

        situacao.atualizar();

        assertEquals("PENDENTE", congelada.getSituacao());
        assertEquals("ATRASADA", normal.getSituacao());
    }

    @Test
    void mensalidadeDeMatriculaTrancadaNaoGeraAvisoDeVencimento() {
        AvisoService avisos = new AvisoService();
        Matricula trancada = matricula("TRANCADA");
        Matricula ativa = matricula("ATIVA");
        Mensalidade congelada = mensalidade(trancada, "PENDENTE", HOJE.plusDays(1));
        congelada.setValor(java.math.BigDecimal.TEN);
        Mensalidade normal = mensalidade(ativa, "PENDENTE", HOJE.plusDays(2));
        normal.setValor(java.math.BigDecimal.TEN);
        assertTrue(avisos.avisoVencimento(List.of(congelada), HOJE).isEmpty());
        assertTrue(avisos.avisoVencimento(List.of(congelada, normal), HOJE).isPresent());
    }
}
