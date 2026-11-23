package com.example.sistema.academia.service;

import com.example.sistema.academia.service.PresencaCalculo.Linha;
import com.example.sistema.academia.service.PresencaCalculo.Marca;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresencaCalculoTest {

    private final LocalDate hoje = LocalDate.of(2026, 11, 23);

    private Marca m(long aluno, String nome, int diasAtras, boolean presente) {
        return new Marca(aluno, nome, hoje.minusDays(diasAtras), presente);
    }

    @Test
    void semMarcacoesNaoHaLinhas() {
        assertTrue(PresencaCalculo.ranking(List.of(), hoje, 30).isEmpty());
        Linha l = PresencaCalculo.doAluno(1L, List.of(), hoje, 30);
        assertEquals(0, l.total());
        assertEquals(0, l.percentual());
    }

    @Test
    void tudoPresenteDaCemPorCento() {
        List<Linha> r = PresencaCalculo.ranking(List.of(m(1, "Ana", 2, true), m(1, "Ana", 9, true)), hoje, 30);
        assertEquals(1, r.size());
        assertEquals(2, r.get(0).presentes());
        assertEquals(0, r.get(0).faltas());
        assertEquals(100, r.get(0).percentual());
    }

    @Test
    void tudoFaltouDaZeroPorCento() {
        List<Linha> r = PresencaCalculo.ranking(List.of(m(2, "Bruno", 2, false), m(2, "Bruno", 9, false)), hoje, 30);
        assertEquals(2, r.get(0).faltas());
        assertEquals(0, r.get(0).percentual());
    }

    @Test
    void soContaOsUltimosTrintaDiasEIgnoraFuturo() {
        List<Marca> marcas = List.of(
                m(1, "Ana", 29, true),   // dentro
                m(1, "Ana", 30, false),  // fora (limite exclusivo)
                m(1, "Ana", 60, false),  // fora
                m(1, "Ana", -3, false)); // futuro
        Linha l = PresencaCalculo.ranking(marcas, hoje, 30).get(0);
        assertEquals(1, l.total());
        assertEquals(1, l.presentes());
    }

    @Test
    void maioresFaltososVemPrimeiro() {
        List<Marca> marcas = List.of(
                m(1, "Ana", 1, true), m(1, "Ana", 8, true), m(1, "Ana", 15, false),
                m(2, "Bruno", 1, false), m(2, "Bruno", 8, false), m(2, "Bruno", 15, true),
                m(3, "Carla", 1, true));
        List<Linha> r = PresencaCalculo.ranking(marcas, hoje, 30);
        assertEquals("Bruno", r.get(0).nome());
        assertEquals(33, r.get(0).percentual());
        assertEquals("Ana", r.get(1).nome());
        assertEquals(67, r.get(1).percentual());
        assertEquals("Carla", r.get(2).nome());
    }
}
