package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Conquista;
import com.example.sistema.academia.model.Entrada;
import com.example.sistema.academia.model.Mensalidade;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConquistasServiceTest {

    private final ConquistasService service = new ConquistasService();
    // quarta-feira
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 18);

    private static Entrada entrada(LocalDate dia, int hora) {
        Entrada e = new Entrada();
        e.setDataHoraEntrada(dia.atTime(hora, 15));
        return e;
    }

    private static Mensalidade mensalidade(String situacao) {
        Mensalidade m = new Mensalidade();
        m.setSituacao(situacao);
        return m;
    }

    private Conquista achar(List<Conquista> lista, String id) {
        return lista.stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void alunoSemNadaNaoTemConquistas() {
        List<Conquista> lista = service.calcular(List.of(), 0, List.of(), 0, HOJE);
        assertEquals(7, lista.size());
        assertTrue(lista.stream().noneMatch(Conquista::conquistada));
        assertEquals("0 de 10 treinos", achar(lista, "treinos-mes").progresso());
    }

    @Test
    void listasNulasNaoQuebram() {
        List<Conquista> lista = service.calcular(null, 0, null, -3, HOJE);
        assertTrue(lista.stream().noneMatch(Conquista::conquistada));
    }

    @Test
    void dezTreinosNoMesConquista() {
        List<Entrada> entradas = new ArrayList<>();
        for (int d = 1; d <= 10; d++) entradas.add(entrada(LocalDate.of(2026, 11, d), 18));
        // duas entradas no mesmo dia contam uma vez
        entradas.add(entrada(LocalDate.of(2026, 11, 3), 7));
        Conquista c = achar(service.calcular(entradas, 0, List.of(), 0, HOJE), "treinos-mes");
        assertTrue(c.conquistada());
        assertEquals("Conquistada", c.progresso());
    }

    @Test
    void viradaDeMesNaoSomaTreinosDoMesAnterior() {
        List<Entrada> entradas = new ArrayList<>();
        for (int d = 20; d <= 30; d++) entradas.add(entrada(LocalDate.of(2026, 10, d), 18));
        entradas.add(entrada(LocalDate.of(2026, 11, 2), 18));
        Conquista c = achar(service.calcular(entradas, 0, List.of(), 0, HOJE), "treinos-mes");
        assertEquals(1, c.atual());
        assertFalse(c.conquistada());
    }

    @Test
    void semanaCompletaPrecisaDeTresDiasNaMesmaSemana() {
        // segunda a quarta
        List<Entrada> boa = List.of(entrada(LocalDate.of(2026, 11, 16), 18), entrada(LocalDate.of(2026, 11, 17), 18),
                entrada(LocalDate.of(2026, 11, 18), 18));
        assertTrue(achar(service.calcular(boa, 0, List.of(), 0, HOJE), "semana-completa").conquistada());
        // domingo e segunda são semanas diferentes (semana começa na segunda)
        List<Entrada> viradaSemana = List.of(entrada(LocalDate.of(2026, 11, 14), 18), entrada(LocalDate.of(2026, 11, 15), 18),
                entrada(LocalDate.of(2026, 11, 16), 18));
        Conquista c = achar(service.calcular(viradaSemana, 0, List.of(), 0, HOJE), "semana-completa");
        assertFalse(c.conquistada());
        assertEquals(2, c.atual());
    }

    @Test
    void tresMesesExigemDozeSemanasSeguidas() {
        LocalDate segunda = LocalDate.of(2026, 11, 16);
        List<Entrada> doze = new ArrayList<>();
        for (int i = 0; i < 12; i++) doze.add(entrada(segunda.minusWeeks(i), 18));
        assertTrue(achar(service.calcular(doze, 0, List.of(), 0, HOJE), "tres-meses").conquistada());

        // uma semana faltando no meio quebra a sequência
        List<Entrada> furo = new ArrayList<>();
        for (int i = 0; i < 12; i++) if (i != 5) furo.add(entrada(segunda.minusWeeks(i), 18));
        Conquista c = achar(service.calcular(furo, 0, List.of(), 0, HOJE), "tres-meses");
        assertFalse(c.conquistada());
        assertEquals(6, c.atual());
    }

    @Test
    void sequenciaDeSemanasAtravessaViradaDeAno() {
        List<LocalDate> segundas = List.of(LocalDate.of(2025, 12, 22), LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 5));
        assertEquals(3, ConquistasService.melhorSequenciaSemanas(segundas));
    }

    @Test
    void primeiraAvaliacao() {
        assertFalse(achar(service.calcular(List.of(), 0, List.of(), 0, HOJE), "primeira-avaliacao").conquistada());
        assertTrue(achar(service.calcular(List.of(), 2, List.of(), 0, HOJE), "primeira-avaliacao").conquistada());
    }

    @Test
    void mensalidadesEmDiaPerdemValorComAtraso() {
        List<Mensalidade> pagas = List.of(mensalidade("PAGA"), mensalidade("PAGA"), mensalidade("PAGA"), mensalidade("PENDENTE"));
        assertTrue(achar(service.calcular(List.of(), 0, pagas, 0, HOJE), "em-dia").conquistada());

        List<Mensalidade> comAtraso = List.of(mensalidade("PAGA"), mensalidade("PAGA"), mensalidade("PAGA"), mensalidade("ATRASADA"));
        Conquista c = achar(service.calcular(List.of(), 0, comAtraso, 0, HOJE), "em-dia");
        assertFalse(c.conquistada());
        assertEquals(2, c.atual());
    }

    @Test
    void madrugadorContaSoAntesDasSete() {
        List<Entrada> entradas = new ArrayList<>();
        for (int d = 1; d <= 5; d++) entradas.add(entrada(LocalDate.of(2026, 11, d), 6));
        entradas.add(entrada(LocalDate.of(2026, 11, 6), 7)); // 7h em ponto não conta
        Conquista c = achar(service.calcular(entradas, 0, List.of(), 0, HOJE), "madrugador");
        assertTrue(c.conquistada());
        assertEquals(5, c.atual());
        assertFalse(achar(service.calcular(entradas.subList(0, 4), 0, List.of(), 0, HOJE), "madrugador").conquistada());
    }

    @Test
    void diarioDeTreinoEPercentual() {
        Conquista c = achar(service.calcular(List.of(), 0, List.of(), 3, HOJE), "diario");
        assertEquals(3, c.atual());
        assertEquals(60, c.percentual());
        assertTrue(achar(service.calcular(List.of(), 0, List.of(), 9, HOJE), "diario").conquistada());
    }

    @Test
    void entradaSemDataEhIgnorada() {
        Entrada sem = new Entrada();
        sem.setDataHoraEntrada(null);
        assertDoesNotThrow(() -> service.calcular(List.of(sem), 0, List.of(), 0, HOJE));
    }
}
