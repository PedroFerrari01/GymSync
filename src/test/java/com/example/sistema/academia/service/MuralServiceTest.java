package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Notificacao;
import com.example.sistema.academia.model.MuralAviso;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MuralServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 22);

    private MuralAviso aviso(String titulo, LocalDate termino, boolean encerrado) {
        MuralAviso a = new MuralAviso();
        a.setTitulo(titulo);
        a.setTexto("Texto");
        a.setDataTermino(termino);
        a.setEncerrado(encerrado);
        return a;
    }

    @Test
    void semTerminoFicaNoArAteEncerrar() {
        assertEquals(1, MuralService.filtrarAtivos(List.of(aviso("A", null, false)), HOJE).size());
        assertTrue(MuralService.filtrarAtivos(List.of(aviso("A", null, true)), HOJE).isEmpty());
    }

    @Test
    void diaDoTerminoAindaVale_diaSeguinteNao() {
        assertEquals(1, MuralService.filtrarAtivos(List.of(aviso("A", HOJE, false)), HOJE).size());
        assertTrue(MuralService.filtrarAtivos(List.of(aviso("A", HOJE.minusDays(1), false)), HOJE).isEmpty());
    }

    @Test
    void listaNulaViraVazia() {
        assertTrue(MuralService.filtrarAtivos(null, HOJE).isEmpty());
    }

    @Test
    void validacao() {
        assertNull(MuralService.validar("Feriado", "Abre às 8h", null, HOJE));
        assertNull(MuralService.validar("Feriado", "Abre às 8h", HOJE, HOJE));
        assertNotNull(MuralService.validar("  ", "texto", null, HOJE));
        assertNotNull(MuralService.validar("ab", "texto", null, HOJE));
        assertNotNull(MuralService.validar("Feriado", "   ", null, HOJE));
        assertNotNull(MuralService.validar("Feriado", "x".repeat(301), null, HOJE));
        assertNotNull(MuralService.validar("x".repeat(81), "texto", null, HOJE));
        assertNotNull(MuralService.validar("Feriado", "texto", HOJE.minusDays(1), HOJE));
    }

    @Test
    void sininhoLevaAbaInicialDoPerfilEFicaNaFrente() {
        List<MuralAviso> avisos = List.of(aviso("Feriado", null, false));
        assertEquals("btn-aba-portal", MuralService.notificacoes(avisos, "ALUNO").get(0).aba());
        assertEquals("btn-aba-treinos", MuralService.notificacoes(avisos, "PROFESSOR").get(0).aba());
        assertEquals("btn-aba-painel", MuralService.notificacoes(avisos, "ADMIN").get(0).aba());
        Notificacao outra = Notificacao.naAba("info", "Outra", "btn-aba-aulas", null);
        List<Notificacao> junta = MuralService.naFrente(List.of(outra), avisos, "ALUNO");
        assertEquals(2, junta.size());
        assertTrue(junta.get(0).texto().startsWith("Mural: Feriado"));
        assertEquals("mural-academia", junta.get(0).alvo());
        assertEquals(1, MuralService.naFrente(null, avisos, "ALUNO").size());
    }
}
