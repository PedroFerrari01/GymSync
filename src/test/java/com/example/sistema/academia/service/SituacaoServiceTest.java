package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Matricula;
import com.example.sistema.academia.model.Mensalidade;
import com.example.sistema.academia.repository.MatriculaRepository;
import com.example.sistema.academia.repository.MensalidadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/** Situação automática: mensalidade pendente vencida vira ATRASADA e matrícula ativa terminada vira VENCIDA. */
class SituacaoServiceTest {

    private final MensalidadeRepository mensalidades = mock(MensalidadeRepository.class);
    private final MatriculaRepository matriculas = mock(MatriculaRepository.class);
    private final SituacaoService service = new SituacaoService();

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(service, "mensalidadeRepository", mensalidades);
        ReflectionTestUtils.setField(service, "matriculaRepository", matriculas);
    }

    @Test
    void pendenteVencidaViraAtrasadaEMatriculaTerminadaViraVencida() {
        Mensalidade m = new Mensalidade();
        m.setSituacao("PENDENTE");
        m.setVencimento(LocalDate.now().minusDays(1));
        Matricula mat = new Matricula();
        mat.setSituacao("ATIVA");
        mat.setDataFim(LocalDate.now().minusDays(1));
        when(mensalidades.findBySituacaoAndVencimentoBefore("PENDENTE", LocalDate.now())).thenReturn(List.of(m));
        when(matriculas.findBySituacaoAndDataFimBefore("ATIVA", LocalDate.now())).thenReturn(List.of(mat));

        service.atualizar();

        assertEquals("ATRASADA", m.getSituacao());
        assertEquals("VENCIDA", mat.getSituacao());
        verify(mensalidades).saveAll(List.of(m));
        verify(matriculas).saveAll(List.of(mat));
    }

    @Test
    void soConsultaPendentesAntesDeHoje() {
        // "Antes de hoje": quem vence hoje ainda está em dia; pagas, em análise e atrasadas não são tocadas
        when(mensalidades.findBySituacaoAndVencimentoBefore(anyString(), any())).thenReturn(List.of());
        when(matriculas.findBySituacaoAndDataFimBefore(anyString(), any())).thenReturn(List.of());

        service.atualizar();

        verify(mensalidades).findBySituacaoAndVencimentoBefore("PENDENTE", LocalDate.now());
        verify(matriculas).findBySituacaoAndDataFimBefore("ATIVA", LocalDate.now());
        verify(mensalidades, never()).findBySituacao(anyString());
    }
}
