package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Aluno;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CarteirinhaServiceTest {

    private final CarteirinhaService servico = new CarteirinhaService("chave-de-teste");

    @Test
    void tokenValidoDevolveOAluno() {
        String t = servico.token(42L);
        assertTrue(t.startsWith("GS1.42."));
        assertEquals(Optional.of(42L), servico.alunoDoToken(t));
        assertEquals(Optional.of(42L), servico.alunoDoToken("  " + t + " "));
    }

    @Test
    void tokenAlteradoOuDeOutraChaveEhRecusado() {
        String t = servico.token(42L);
        assertTrue(servico.alunoDoToken(t.replace("GS1.42.", "GS1.43.")).isEmpty());
        assertTrue(servico.alunoDoToken(t.substring(0, t.length() - 1) + "x").isEmpty());
        assertTrue(new CarteirinhaService("outra").alunoDoToken(t).isEmpty());
        assertTrue(new CarteirinhaService("").alunoDoToken(t).isEmpty());
    }

    @Test
    void codigoMalformadoEhRecusado() {
        assertTrue(servico.alunoDoToken(null).isEmpty());
        assertTrue(servico.alunoDoToken("").isEmpty());
        assertTrue(servico.alunoDoToken("513.790.480-52").isEmpty());
        assertTrue(servico.alunoDoToken("GS1.abc.xyz").isEmpty());
        assertTrue(servico.alunoDoToken("GS1.1.a.b").isEmpty());
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
