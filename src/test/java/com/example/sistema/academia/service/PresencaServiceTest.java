package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.AulaColetiva;
import com.example.sistema.academia.model.PresencaAula;
import com.example.sistema.academia.model.Professor;
import com.example.sistema.academia.model.ReservaAula;
import com.example.sistema.academia.repository.AulaColetivaRepository;
import com.example.sistema.academia.repository.PresencaAulaRepository;
import com.example.sistema.academia.repository.ReservaAulaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PresencaServiceTest {

    private final LocalDate hoje = LocalDate.of(2026, 11, 23);
    private PresencaAulaRepository presencas;
    private ReservaAulaRepository reservas;
    private PresencaService service;
    private ReservaAula reserva;

    @BeforeEach
    void preparar() {
        presencas = mock(PresencaAulaRepository.class);
        reservas = mock(ReservaAulaRepository.class);
        service = new PresencaService(mock(AulaColetivaRepository.class), reservas, presencas);

        Professor dono = new Professor();
        dono.setId(7L);
        AulaColetiva aula = new AulaColetiva();
        aula.setId(3L);
        aula.setProfessor(dono);
        Aluno aluno = new Aluno();
        aluno.setId(9L);
        reserva = new ReservaAula();
        reserva.setId(1L);
        reserva.setAula(aula);
        reserva.setAluno(aluno);
        reserva.setDataAula(hoje.minusDays(2));
        when(reservas.findById(1L)).thenReturn(Optional.of(reserva));
        when(presencas.findFirstByAulaIdAndAlunoIdAndDataAula(any(), any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void professorDaAulaMarca() {
        assertNull(service.marcar(1L, true, "PROFESSOR", 7L, "Prof", hoje));
        verify(presencas).save(any(PresencaAula.class));
    }

    @Test
    void adminMarca() {
        assertNull(service.marcar(1L, false, "ADMIN", null, "Admin", hoje));
        verify(presencas).save(any(PresencaAula.class));
    }

    @Test
    void outroProfessorNaoMarca() {
        assertNotNull(service.marcar(1L, true, "PROFESSOR", 8L, "Outro", hoje));
        verify(presencas, never()).save(any());
    }

    @Test
    void alunoNaoMarca() {
        assertNotNull(service.marcar(1L, true, "ALUNO", 9L, "Aluno", hoje));
        verify(presencas, never()).save(any());
    }

    @Test
    void aulaFuturaNaoPodeSerMarcada() {
        reserva.setDataAula(hoje.plusDays(1));
        assertEquals("Esta aula ainda não aconteceu.", service.marcar(1L, true, "ADMIN", null, "Admin", hoje));
        verify(presencas, never()).save(any());
    }

    @Test
    void marcarDeNovoTrocaSemCriarOutra() {
        PresencaAula existente = new PresencaAula();
        existente.setPresente(true);
        when(presencas.findFirstByAulaIdAndAlunoIdAndDataAula(any(), any(), any())).thenReturn(Optional.of(existente));
        assertNull(service.marcar(1L, false, "ADMIN", null, "Admin", hoje));
        assertEquals(false, existente.isPresente());
    }
}
