package com.example.sistema.academia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.sistema.academia.model.Mensalidade;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReciboServiceTest {

    private Mensalidade mensalidade(Long id, String situacao, LocalDate pago, LocalDate venc) {
        Mensalidade m = new Mensalidade();
        m.setId(id);
        m.setSituacao(situacao);
        m.setDataPagamento(pago);
        m.setVencimento(venc);
        return m;
    }

    @Test
    void numeroUsaAnoDoPagamentoEIdComZeros() {
        assertEquals("GS-2026-000123", ReciboService.numero(mensalidade(123L, "PAGA", LocalDate.of(2026, 11, 3), LocalDate.of(2025, 12, 1))));
        assertEquals("GS-2025-000007", ReciboService.numero(mensalidade(7L, "PAGA", null, LocalDate.of(2025, 12, 1))));
    }

    @Test
    void soMensalidadePagaTemRecibo() {
        assertTrue(ReciboService.temRecibo(mensalidade(1L, "PAGA", null, null)));
        assertFalse(ReciboService.temRecibo(mensalidade(1L, "EM_ANALISE", null, null)));
        assertFalse(ReciboService.temRecibo(mensalidade(1L, "PENDENTE", null, null)));
        assertFalse(ReciboService.temRecibo(null));
    }

    @Test
    void cpfSaiMascarado() {
        assertEquals("***.456.789-**", ReciboService.cpfMascarado("123.456.789-09"));
        assertEquals("***.456.789-**", ReciboService.cpfMascarado("12345678909"));
        assertEquals("-", ReciboService.cpfMascarado(null));
        assertEquals("-", ReciboService.cpfMascarado("123"));
    }

    @Test
    void alunoSoVeOProprioRecibo() {
        assertTrue(ReciboService.podeVer("ADMIN", 1L, 99L));
        assertTrue(ReciboService.podeVer("ALUNO", 5L, 5L));
        assertFalse(ReciboService.podeVer("ALUNO", 6L, 5L));
        assertFalse(ReciboService.podeVer("ALUNO", null, 5L));
        assertFalse(ReciboService.podeVer("PROFESSOR", 5L, 5L));
        assertFalse(ReciboService.podeVer(null, null, 5L));
    }
}
