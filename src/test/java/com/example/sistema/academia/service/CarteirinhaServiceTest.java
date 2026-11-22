package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Aluno;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CarteirinhaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-21T12:00:00Z");

    private final CarteirinhaService servico = new CarteirinhaService("chave-de-teste");

    /** Mesma chave, mas com o relógio andado: simula o código sendo lido depois (ou antes) de emitido. */
    private static CarteirinhaService emHorario(String chave, Instant instante) {
        return new CarteirinhaService(chave, Clock.fixed(instante, ZoneOffset.UTC));
    }

    @Test
    void tokenValidoDevolveOAluno() {
        String t = servico.token(42L);
        assertTrue(t.startsWith("GS2.42."));
        assertEquals(Optional.of(42L), servico.alunoDoToken(t));
        assertEquals(Optional.of(42L), servico.alunoDoToken("  " + t + " "));
    }

    @Test
    void tokenAlteradoOuDeOutraChaveEhRecusado() {
        String t = servico.token(42L);
        assertTrue(servico.alunoDoToken(t.replace("GS2.42.", "GS2.43.")).isEmpty());
        assertTrue(servico.alunoDoToken(t.substring(0, t.length() - 1) + "x").isEmpty());
        assertTrue(new CarteirinhaService("outra").alunoDoToken(t).isEmpty());
        assertTrue(new CarteirinhaService("").alunoDoToken(t).isEmpty());
    }

    @Test
    void codigoMalformadoEhRecusado() {
        assertTrue(servico.alunoDoToken(null).isEmpty());
        assertTrue(servico.alunoDoToken("").isEmpty());
        assertTrue(servico.alunoDoToken("513.790.480-52").isEmpty());
        assertTrue(servico.alunoDoToken("GS2.abc.123.xyz").isEmpty());
        assertTrue(servico.alunoDoToken("GS2.1.a.b").isEmpty());
        assertTrue(servico.alunoDoToken("GS2.1.123").isEmpty());
    }

    @Test
    void codigoValeAteOPrazoEDepoisExpira() {
        String t = emHorario("k", AGORA).token(9L);
        assertEquals(Optional.of(9L), emHorario("k", AGORA.plusSeconds(1)).alunoDoToken(t));
        assertEquals(Optional.of(9L), emHorario("k", AGORA.plusSeconds(CarteirinhaService.VALIDADE_SEGUNDOS)).alunoDoToken(t));
        CarteirinhaService.Leitura vencido = emHorario("k", AGORA.plusSeconds(CarteirinhaService.VALIDADE_SEGUNDOS + 1)).ler(t);
        assertEquals(CarteirinhaService.Situacao.EXPIRADO, vencido.situacao());
        assertNull(vencido.alunoId());
        assertTrue(vencido.motivoRecusa().contains("vencida"));
        assertTrue(emHorario("k", AGORA.plus(Duration.ofHours(2))).alunoDoToken(t).isEmpty());
    }

    @Test
    void codigoDoFuturoEhRecusado() {
        String t = emHorario("k", AGORA.plusSeconds(600)).token(9L);
        CarteirinhaService.Leitura l = emHorario("k", AGORA).ler(t);
        assertEquals(CarteirinhaService.Situacao.NO_FUTURO, l.situacao());
        assertTrue(l.motivoRecusa().contains("futuro"));
        // Uma pequena diferença de relógio entre aparelho e servidor é aceita
        String quaseAgora = emHorario("k", AGORA.plusSeconds(20)).token(9L);
        assertTrue(emHorario("k", AGORA).ler(quaseAgora).valido());
    }

    @Test
    void horarioForjadoQuebraAAssinatura() {
        String t = emHorario("k", AGORA.minusSeconds(3600)).token(9L);
        String[] p = t.split("[.]");
        String forjado = p[0] + "." + p[1] + "." + AGORA.getEpochSecond() + "." + p[3];
        CarteirinhaService.Leitura l = emHorario("k", AGORA).ler(forjado);
        assertEquals(CarteirinhaService.Situacao.INVALIDO, l.situacao());
    }

    @Test
    void chaveDiferenteEhRecusadaMesmoDentroDoPrazo() {
        String t = emHorario("chave-a", AGORA).token(9L);
        assertTrue(emHorario("chave-b", AGORA).alunoDoToken(t).isEmpty());
        assertTrue(emHorario("chave-a", AGORA).alunoDoToken(t).isPresent());
    }

    @Test
    void formatoAntigoSemHorarioEhRecusadoComAviso() {
        CarteirinhaService.Leitura l = servico.ler("GS1.1.Jdl9odu5YufrGEvmZRIc1z");
        assertEquals(CarteirinhaService.Situacao.FORMATO_ANTIGO, l.situacao());
        assertTrue(l.alunoId() == null);
        assertTrue(l.motivoRecusa().contains("desatualizada"));
    }

    @Test
    void mensagemGenericaParaCodigoInvalido() {
        assertEquals("código da carteirinha inválido.", servico.ler("lixo").motivoRecusa());
    }

    @Test
    void tokenNaoLevaCpf() {
        assertFalse(servico.token(7L).contains("513"));
    }

    @Test
    void regrasDaCatraca() {
        Aluno a = new Aluno();
        a.setSituacao("ATIVO");
        assertNull(CarteirinhaService.motivoBloqueio(a, true, false));
        assertEquals("não possui matrícula ativa", CarteirinhaService.motivoBloqueio(a, false, false));
        assertEquals("está com mensalidade em atraso", CarteirinhaService.motivoBloqueio(a, true, true));
        a.setSituacao("INATIVO");
        assertEquals("está com cadastro INATIVO", CarteirinhaService.motivoBloqueio(a, true, false));
    }

    @Test
    void qrEmSvg() {
        String svg = servico.qrSvg(servico.token(1L));
        assertTrue(svg.startsWith("<svg"));
        assertTrue(svg.contains("<path"));
    }
}
