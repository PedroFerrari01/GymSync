package com.example.sistema.academia.service;

import com.example.sistema.academia.model.*;
import com.example.sistema.academia.repository.*;
import com.example.sistema.academia.util.Validador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VisitanteServiceTest {

    private VisitanteService servico;
    private AlunoRepository alunos;
    private TreinoRepository treinos;
    private EntradaRepository entradas;
    private MensalidadeRepository mensalidades;
    private EquipamentoRepository equipamentos;
    private Aluno ana;
    private Aluno real;
    private Treino ficha;

    @BeforeEach
    void preparar() {
        servico = new VisitanteService();
        alunos = mock(AlunoRepository.class);
        treinos = mock(TreinoRepository.class);
        entradas = mock(EntradaRepository.class);
        mensalidades = mock(MensalidadeRepository.class);
        equipamentos = mock(EquipamentoRepository.class);
        ReflectionTestUtils.setField(servico, "habilitado", true);
        ReflectionTestUtils.setField(servico, "alunoRepository", alunos);
        ReflectionTestUtils.setField(servico, "treinoRepository", treinos);
        ReflectionTestUtils.setField(servico, "entradaRepository", entradas);
        ReflectionTestUtils.setField(servico, "mensalidadeRepository", mensalidades);
        ReflectionTestUtils.setField(servico, "equipamentoRepository", equipamentos);
        for (String campo : List.of("matriculaRepository", "registroExercicioRepository", "avaliacaoFisicaRepository",
                "mensagemRepository", "reservaAulaRepository", "manutencaoRepository")) {
            ReflectionTestUtils.setField(servico, campo, mockDe(campo));
        }

        ana = new Aluno();
        ana.setId(1L);
        ana.setEmail(VisitanteService.EMAIL_ALUNO);
        real = new Aluno();
        real.setId(99L);
        real.setEmail("joao@gmail.com");
        when(alunos.findByEmailEndingWithIgnoreCase("@exemplo.gymsync")).thenReturn(List.of(ana));

        ficha = new Treino();
        ficha.setDataCriacao(LocalDate.now().minusDays(20 + 10)); // dados criados há 10 dias
        when(treinos.findByAlunoId(1L)).thenReturn(List.of(ficha));
    }

    private Object mockDe(String campo) {
        return switch (campo) {
            case "matriculaRepository" -> mock(MatriculaRepository.class);
            case "registroExercicioRepository" -> mock(RegistroExercicioRepository.class);
            case "avaliacaoFisicaRepository" -> mock(AvaliacaoFisicaRepository.class);
            case "mensagemRepository" -> mock(MensagemRepository.class);
            case "reservaAulaRepository" -> mock(ReservaAulaRepository.class);
            default -> mock(ManutencaoRepository.class);
        };
    }

    @Test
    void cpfsDeExemploSaoValidosEDiferentes() {
        Set<String> vistos = new HashSet<>();
        for (int n = 1; n <= 8; n++) {
            String cpf = VisitanteService.cpfDeExemplo(n);
            assertEquals(14, cpf.length(), cpf);
            assertTrue(Validador.cpfValido(cpf), "CPF inválido: " + cpf);
            assertTrue(vistos.add(cpf));
        }
    }

    @Test
    void renovarDatasEmpurraEntradasEMensalidadesParaHoje() {
        Entrada e = new Entrada();
        LocalDateTime ontemHaDezDias = LocalDate.now().minusDays(11).atTime(8, 0);
        e.setDataHoraEntrada(ontemHaDezDias);
        when(entradas.findByAlunoId(1L)).thenReturn(List.of(e));

        Mensalidade paga = new Mensalidade();
        paga.setVencimento(LocalDate.now().minusDays(30));
        paga.setDataPagamento(LocalDate.now().minusDays(32));
        Mensalidade proxima = new Mensalidade();
        proxima.setVencimento(LocalDate.now().plusDays(2));
        when(mensalidades.findByMatriculaAlunoId(1L)).thenReturn(List.of(paga, proxima));

        servico.renovarDatas();

        assertEquals(LocalDate.now().minusDays(1), e.getDataHoraEntrada().toLocalDate());
        assertEquals(LocalDate.now().minusDays(20), paga.getVencimento());
        assertEquals(LocalDate.now().minusDays(22), paga.getDataPagamento());
        assertEquals(LocalDate.now().plusDays(12), proxima.getVencimento());
        assertEquals(LocalDate.now().minusDays(20), ficha.getDataCriacao()); // marcador em dia
    }

    @Test
    void renovarDatasNaoFazNadaQuandoOsDadosJaEstaoEmDia() {
        ficha.setDataCriacao(LocalDate.now().minusDays(20));
        servico.renovarDatas();
        verify(entradas, never()).save(any());
        verify(mensalidades, never()).save(any());
    }

    @Test
    void renovarDatasNaoMexeEmAlunoReal() {
        // o aluno real nem é consultado: só quem tem e-mail de exemplo
        servico.renovarDatas();
        verify(entradas, never()).findByAlunoId(99L);
        verify(treinos, never()).findByAlunoId(99L);
        assertFalse(real.getEmail().endsWith("@exemplo.gymsync"));
    }

    @Test
    void completaAlturaSilhuetaEMedidasNovasDaAnaEmBancoAntigo() {
        AvaliacaoFisicaRepository avaliacoes = (AvaliacaoFisicaRepository) ReflectionTestUtils.getField(servico, "avaliacaoFisicaRepository");
        AvaliacaoFisica a1 = new AvaliacaoFisica(), a2 = new AvaliacaoFisica(), a3 = new AvaliacaoFisica();
        a3.setOmbro(110.0); // valor já salvo não muda
        when(avaliacoes.findByAlunoIdOrderByDataAvaliacaoAscIdAsc(1L)).thenReturn(List.of(a1, a2, a3));
        when(alunos.findByEmailEndingWithIgnoreCase("@exemplo.gymsync")).thenReturn(List.of(ana, real));

        servico.completarCamposNovos();

        assertEquals(165, ana.getAlturaCm());
        assertEquals(Aluno.SILHUETA_FEMININA, ana.getSilhueta());
        assertEquals(104.0, a1.getOmbro());
        assertEquals(24.8, a1.getMassaMuscularKg());
        assertEquals(15.8, a3.getMassaGordaKg());
        assertEquals(110.0, a3.getOmbro());
        // O aluno real não é tocado
        assertEquals(null, real.getAlturaCm());
        verify(avaliacoes, never()).findByAlunoIdOrderByDataAvaliacaoAscIdAsc(99L);
    }

    @Test
    void renovarDatasDesligadoNaoFazNada() {
        ReflectionTestUtils.setField(servico, "habilitado", false);
        servico.renovarDatas();
        Mockito.verifyNoInteractions(alunos);
    }
}
