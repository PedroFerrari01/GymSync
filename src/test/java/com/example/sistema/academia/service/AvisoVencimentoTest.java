package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Aviso;
import com.example.sistema.academia.model.Mensalidade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Aviso ao aluno de mensalidade perto do vencimento (AvisoService.avisoVencimento). */
class AvisoVencimentoTest {

    private final AvisoService service = new AvisoService();
    private final LocalDate hoje = LocalDate.of(2026, 10, 30);

    private Mensalidade mensalidade(long id, String situacao, LocalDate vencimento) {
        Mensalidade m = new Mensalidade();
        m.setId(id);
        m.setSituacao(situacao);
        m.setVencimento(vencimento);
        m.setValor(new BigDecimal("129.90"));
        return m;
    }

    @Test
    void avisaTresDiasAntesComBotaoDePagar() {
        Optional<Aviso> aviso = service.avisoVencimento(List.of(
                mensalidade(1, "PENDENTE", hoje.plusDays(3)),
                mensalidade(2, "PENDENTE", hoje.plusDays(33))), hoje);

        assertTrue(aviso.isPresent());
        assertEquals(1L, aviso.get().getMensalidadeId());
        assertEquals("R$ 129,90", aviso.get().getValor());
        assertEquals("02/11/2026", aviso.get().getVencimento());
        assertTrue(aviso.get().getTexto().contains("vence em 3 dias"));
    }

    @Test
    void escolheAMaisProximaEFalaHojeOuAmanha() {
        assertTrue(service.avisoVencimento(List.of(mensalidade(1, "PENDENTE", hoje)), hoje).get().getTexto().contains("vence hoje"));
        Aviso amanha = service.avisoVencimento(List.of(
                mensalidade(1, "PENDENTE", hoje.plusDays(2)),
                mensalidade(2, "PENDENTE", hoje.plusDays(1))), hoje).get();
        assertEquals(2L, amanha.getMensalidadeId());
        assertTrue(amanha.getTexto().contains("vence amanhã"));
    }

    @Test
    void naoAvisaForaDoPrazoNemQuemJaPagouOuInformou() {
        assertTrue(service.avisoVencimento(List.of(
                mensalidade(1, "PENDENTE", hoje.plusDays(4)),
                mensalidade(2, "PAGA", hoje.plusDays(1)),
                mensalidade(3, "EM_ANALISE", hoje.plusDays(1)),
                mensalidade(4, "ATRASADA", hoje.minusDays(2))), hoje).isEmpty());
    }
}
