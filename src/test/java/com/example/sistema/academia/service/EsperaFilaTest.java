package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.EsperaAula;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EsperaFilaTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 23, 10, 0);

    private EsperaAula na(long alunoId) {
        Aluno a = new Aluno();
        a.setId(alunoId);
        EsperaAula e = new EsperaAula();
        e.setAluno(a);
        return e;
    }

    @Test
    void entraQuandoLotadaESemReservaNemFila() {
        assertEquals(EsperaFila.Resultado.OK, EsperaFila.podeEntrar(true, 0, false, false));
    }

    @Test
    void naoEntraSeAindaTemVaga() {
        assertEquals(EsperaFila.Resultado.NAO_ESTA_LOTADA, EsperaFila.podeEntrar(true, 1, false, false));
    }

    @Test
    void naoEntraDuplicado() {
        assertEquals(EsperaFila.Resultado.JA_NA_FILA, EsperaFila.podeEntrar(true, 0, false, true));
    }

    @Test
    void quemJaTemReservaNaoEntraNaFila() {
        assertEquals(EsperaFila.Resultado.JA_RESERVOU, EsperaFila.podeEntrar(true, 0, true, false));
    }

    @Test
    void dataInvalidaTemPrioridade() {
        assertEquals(EsperaFila.Resultado.DATA_INVALIDA, EsperaFila.podeEntrar(false, 0, false, false));
        assertFalse(EsperaFila.mensagem(EsperaFila.Resultado.DATA_INVALIDA).isBlank());
        assertTrue(EsperaFila.mensagem(EsperaFila.Resultado.OK).isEmpty());
    }

    @Test
    void posicaoComecaEmUmEZeroQuemNaoEstaNaFila() {
        List<EsperaAula> fila = List.of(na(7), na(9), na(4));
        assertEquals(1, EsperaFila.posicao(fila, 7L));
        assertEquals(3, EsperaFila.posicao(fila, 4L));
        assertEquals(0, EsperaFila.posicao(fila, 99L));
        assertEquals(0, EsperaFila.posicao(null, 7L));
        assertEquals(0, EsperaFila.posicao(fila, null));
    }

    @Test
    void sairDaFilaFazOsDemaisSubirUmaPosicao() {
        List<EsperaAula> fila = new ArrayList<>(List.of(na(7), na(9), na(4)));
        fila.remove(0);
        assertEquals(1, EsperaFila.posicao(fila, 9L));
        assertEquals(2, EsperaFila.posicao(fila, 4L));
    }

    @Test
    void abrirUmaVagaAvisaSoOPrimeiroDaFila() {
        List<EsperaAula> fila = List.of(na(7), na(9), na(4));
        List<EsperaAula> avisados = EsperaFila.promover(fila, 1, AGORA);
        assertEquals(1, avisados.size());
        assertEquals(7L, avisados.get(0).getAluno().getId());
        assertEquals(AGORA, fila.get(0).getNotificadaEm());
        assertNull(fila.get(1).getNotificadaEm());
    }

    @Test
    void naoAvisaDeNovoQuemJaFoiAvisadoNemPassaAFrenteSemVaga() {
        List<EsperaAula> fila = List.of(na(7), na(9));
        EsperaFila.promover(fila, 1, AGORA);
        assertTrue(EsperaFila.promover(fila, 1, AGORA.plusMinutes(5)).isEmpty());
        assertEquals(AGORA, fila.get(0).getNotificadaEm());
    }

    @Test
    void duasVagasAvisamDoisEUmaVagaNovaAvisaOProximo() {
        List<EsperaAula> fila = List.of(na(7), na(9), na(4));
        assertEquals(2, EsperaFila.promover(fila, 2, AGORA).size());
        assertNull(fila.get(2).getNotificadaEm());
        List<EsperaAula> depois = EsperaFila.promover(fila, 3, AGORA.plusMinutes(1));
        assertEquals(1, depois.size());
        assertEquals(4L, depois.get(0).getAluno().getId());
    }

    @Test
    void semVagaOuSemFilaNaoAvisaNinguem() {
        assertTrue(EsperaFila.promover(List.of(na(7)), 0, AGORA).isEmpty());
        assertTrue(EsperaFila.promover(List.of(), 2, AGORA).isEmpty());
        assertTrue(EsperaFila.promover(null, 2, AGORA).isEmpty());
    }

    @Test
    void textoDoSininhoAvisaDaVagaEPedeParaReservar() {
        String t = EsperaService.textoVaga("Boxe", "24/11");
        assertTrue(t.contains("Abriu uma vaga na aula Boxe"));
        assertTrue(t.contains("reserve agora"));
    }

    @Test
    void chaveDaCelulaJuntaAulaEData() {
        assertEquals("5|2026-11-24", EsperaService.chave(5L, java.time.LocalDate.of(2026, 11, 24)));
    }
}
