package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Visao360;
import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.AvaliacaoFisica;
import com.example.sistema.academia.model.Entrada;
import com.example.sistema.academia.model.Matricula;
import com.example.sistema.academia.model.Mensagem;
import com.example.sistema.academia.model.Mensalidade;
import com.example.sistema.academia.model.Plano;
import com.example.sistema.academia.model.Professor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Visao360ServiceTest {

    private final LocalDateTime agora = LocalDateTime.of(2026, 11, 20, 10, 0);

    private Aluno aluno() {
        Aluno a = new Aluno();
        a.setId(1L);
        a.setNome("Ana Teste");
        return a;
    }

    private Visao360 vazio() {
        return Visao360Service.montar(aluno(), List.of(), List.of(), List.of(), List.of(), List.of(), agora);
    }

    private Entrada entrada(int mes, int dia) {
        Entrada e = new Entrada();
        e.setDataHoraEntrada(LocalDateTime.of(2026, mes, dia, 7, 0));
        return e;
    }

    @Test
    void alunoSemHistoricoNaoQuebra() {
        Visao360 v = vazio();
        assertEquals("Ana Teste", v.nome());
        assertEquals("Sem plano", v.plano());
        assertEquals("Sem matrícula", v.situacaoMatricula());
        assertEquals("Nenhuma", v.ultimaEntrada());
        assertEquals("Nenhuma", v.ultimaAvaliacao());
        assertEquals(0, v.diasTreinados30());
        assertEquals(0, v.mensalidadesEmAtraso());
        assertTrue(v.linhaDoTempo().isEmpty());
    }

    @Test
    void linhaDoTempoVemDaMaisRecenteParaAMaisAntiga() {
        Plano plano = new Plano();
        plano.setNome("Anual");
        Matricula mat = new Matricula();
        mat.setPlano(plano);
        mat.setDataInicio(LocalDate.of(2026, 1, 5));
        mat.setSituacao("ATIVA");
        Mensalidade paga = new Mensalidade();
        paga.setSituacao("PAGA");
        paga.setValor(new BigDecimal("99.90"));
        paga.setVencimento(LocalDate.of(2026, 10, 5));
        paga.setDataPagamento(LocalDate.of(2026, 10, 4));
        AvaliacaoFisica av = new AvaliacaoFisica();
        av.setDataAvaliacao(LocalDate.of(2026, 11, 10));
        av.setPeso(72.5);
        av.setPercentualGordura(18.2);

        Visao360 v = Visao360Service.montar(aluno(), List.of(mat), List.of(paga), List.of(av), List.of(), List.of(), agora);

        List<LocalDate> datas = v.linhaDoTempo().stream().map(Visao360.Evento::data).toList();
        assertEquals(List.of(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 10, 4), LocalDate.of(2026, 1, 5)), datas);
        assertEquals("Anual", v.plano());
        assertEquals("Ativa", v.situacaoMatricula());
        assertEquals("10/11/2026", v.ultimaAvaliacao());
        assertEquals("Peso 72,5 kg · gordura 18,2%", v.linhaDoTempo().get(0).detalhe());
        assertEquals("R$ 99,90", v.linhaDoTempo().get(1).detalhe());
    }

    @Test
    void entradasViramUmaLinhaPorSemana() {
        // 02/11/2026 é segunda: 3 entradas na semana de 02/11 (2 no mesmo dia contam as duas) e 1 na seguinte
        List<Entrada> entradas = List.of(entrada(11, 2), entrada(11, 4), entrada(11, 6), entrada(11, 10));
        Visao360 v = Visao360Service.montar(aluno(), List.of(), List.of(), List.of(), entradas, List.of(), agora);

        assertEquals(2, v.linhaDoTempo().size());
        assertEquals("1 entrada na catraca", v.linhaDoTempo().get(0).titulo());
        assertEquals("3 entradas na catraca", v.linhaDoTempo().get(1).titulo());
        assertEquals("Semana de 02/11", v.linhaDoTempo().get(1).detalhe());
        assertEquals(4, v.diasTreinados30());
        assertEquals("Há 10 dias (10/11)", v.ultimaEntrada());
    }

    @Test
    void frequenciaConta30DiasEIgnoraEntradasAntigas() {
        Visao360 v = Visao360Service.montar(aluno(), List.of(), List.of(), List.of(),
                List.of(entrada(11, 19), entrada(10, 21), entrada(10, 20)), List.of(), agora);
        // 20/11 menos 29 dias = 22/10: 21/10 e 20/10 ficam fora
        assertEquals(1, v.diasTreinados30());
    }

    @Test
    void mensalidadeEmAtrasoContaEApareceNaLinhaDoTempo() {
        Mensalidade atrasada = new Mensalidade();
        atrasada.setSituacao("ATRASADA");
        atrasada.setVencimento(LocalDate.of(2026, 11, 5));
        atrasada.setValor(new BigDecimal("120"));
        Visao360 v = Visao360Service.montar(aluno(), List.of(), List.of(atrasada), List.of(), List.of(), List.of(), agora);
        assertEquals(1, v.mensalidadesEmAtraso());
        assertEquals("ATRASO", v.linhaDoTempo().get(0).tipo());
    }

    @Test
    void mensagemMostraSoDataEQuemEnviouNuncaOTexto() {
        Professor prof = new Professor();
        prof.setId(9L);
        prof.setNome("Marcos Lima");
        Mensagem m1 = new Mensagem();
        m1.setProfessor(prof);
        m1.setRemetente("ALUNO");
        m1.setTexto("segredo médico do aluno");
        m1.setDataHora(LocalDateTime.of(2026, 11, 18, 9, 0));
        Mensagem m2 = new Mensagem();
        m2.setProfessor(prof);
        m2.setRemetente("ALUNO");
        m2.setTexto("outra coisa privada");
        m2.setDataHora(LocalDateTime.of(2026, 11, 18, 18, 0));

        Visao360 v = Visao360Service.montar(aluno(), List.of(), List.of(), List.of(), List.of(), List.of(m1, m2), agora);

        assertEquals(1, v.linhaDoTempo().size());
        Visao360.Evento e = v.linhaDoTempo().get(0);
        assertEquals("2 mensagens do aluno", e.titulo());
        assertEquals("Para Marcos Lima", e.detalhe());
        assertFalse(v.toString().contains("segredo"));
        assertFalse(v.toString().contains("privada"));
    }

    @Test
    void permissaoAdminVeTudoProfessorSoOsDele() {
        Set<Long> meus = Set.of(1L, 2L);
        assertTrue(Visao360Service.podeVer("ADMIN", Set.of(), 99L));
        assertTrue(Visao360Service.podeVer("PROFESSOR", meus, 2L));
        assertFalse(Visao360Service.podeVer("PROFESSOR", meus, 3L));
        assertFalse(Visao360Service.podeVer("PROFESSOR", null, 1L));
        assertFalse(Visao360Service.podeVer("ALUNO", meus, 1L));
        assertFalse(Visao360Service.podeVer(null, meus, 1L));
        assertFalse(Visao360Service.podeVer("ADMIN", meus, null));
    }
}
