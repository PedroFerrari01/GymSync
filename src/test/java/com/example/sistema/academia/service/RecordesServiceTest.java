package com.example.sistema.academia.service;

import com.example.sistema.academia.model.ExercicioTreino;
import com.example.sistema.academia.model.RegistroExercicio;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecordesServiceTest {

    private final RecordesService servico = new RecordesService();
    private final LocalDate hoje = LocalDate.of(2026, 11, 20);
    private long seq = 1;

    private ExercicioTreino ex(long id, String nome) {
        ExercicioTreino e = new ExercicioTreino();
        e.setId(id);
        e.setExercicio(nome);
        return e;
    }

    private RegistroExercicio reg(ExercicioTreino e, int diasAtras, Double carga) {
        RegistroExercicio r = new RegistroExercicio();
        r.setId(seq++);
        r.setExercicio(e);
        r.setDataRegistro(hoje.minusDays(diasAtras));
        r.setCarga(carga);
        return r;
    }

    @Test
    void semRegistrosNaoHaRecordes() {
        assertTrue(servico.calcular(List.of(), hoje, 4).isEmpty());
        assertTrue(servico.calcular(null, hoje, 4).isEmpty());
    }

    @Test
    void ignoraRegistroSemCarga() {
        ExercicioTreino e = ex(1, "Supino");
        assertTrue(servico.calcular(List.of(reg(e, 1, null), reg(e, 2, 0.0)), hoje, 4).isEmpty());
    }

    @Test
    void recordeEMaiorCargaEEvolucaoFicaEmOrdemDeData() {
        ExercicioTreino e = ex(1, "Supino reto");
        List<RegistroExercicio> regs = List.of(reg(e, 3, 32.5), reg(e, 20, 25.0), reg(e, 10, 30.0)); // fora de ordem
        RecordesService.Recorde r = servico.calcular(regs, hoje, 4).get(0);
        assertEquals(32.5, r.carga());
        assertEquals(hoje.minusDays(3), r.data());
        assertEquals(3, r.registros());
        assertEquals(List.of(25.0, 30.0, 32.5), r.pontos().stream().map(RecordesService.Ponto::carga).toList());
        assertTrue(r.novo());
    }

    @Test
    void cargaIgualAoRecordeNaoEhRecordeNovo() {
        ExercicioTreino e = ex(1, "Remada");
        RecordesService.Recorde r = servico.calcular(List.of(reg(e, 9, 30.0), reg(e, 2, 30.0)), hoje, 4).get(0);
        assertFalse(r.novo());
        assertEquals(hoje.minusDays(9), r.data());
    }

    @Test
    void recordeAntigoOuUltimoMenorNaoTemSelo() {
        ExercicioTreino e = ex(1, "Rosca");
        assertFalse(servico.calcular(List.of(reg(e, 30, 10.0), reg(e, 20, 12.0)), hoje, 4).get(0).novo());
        assertFalse(servico.calcular(List.of(reg(e, 5, 14.0), reg(e, 1, 12.0)), hoje, 4).get(0).novo());
    }

    @Test
    void umSoRegistroNaoEhNovoRecorde() {
        assertFalse(servico.calcular(List.of(reg(ex(1, "Leg"), 1, 50.0)), hoje, 4).get(0).novo());
    }

    @Test
    void ordenaPorMaisRegistrosELimita() {
        ExercicioTreino a = ex(1, "A"), b = ex(2, "B"), c = ex(3, "C");
        List<RegistroExercicio> regs = List.of(reg(a, 1, 10.0), reg(b, 1, 10.0), reg(b, 2, 11.0), reg(b, 3, 9.0),
                reg(c, 1, 10.0), reg(c, 2, 11.0));
        List<RecordesService.Recorde> r = servico.calcular(regs, hoje, 2);
        assertEquals(2, r.size());
        assertEquals("B", r.get(0).exercicio());
        assertEquals("C", r.get(1).exercicio());
    }

    @Test
    void juntaExerciciosComOMesmoNomeEmFichasDiferentes() {
        List<RegistroExercicio> regs = List.of(reg(ex(1, "Supino"), 10, 20.0), reg(ex(9, " supino "), 2, 25.0));
        List<RecordesService.Recorde> r = servico.calcular(regs, hoje, 4);
        assertEquals(1, r.size());
        assertEquals(2, r.get(0).registros());
    }

    @Test
    void graficoTemUmPontoPorRegistro() {
        ExercicioTreino e = ex(1, "X");
        RecordesService.Recorde r = servico.calcular(List.of(reg(e, 9, 10.0), reg(e, 5, 20.0), reg(e, 1, 15.0)), hoje, 4).get(0);
        assertEquals(3, r.pontosSvg().split(" ").length);
    }

    @Test
    void maximosDaFichaUsaONomeDoExercicio() {
        ExercicioTreino a = ex(1, "Supino"), b = ex(2, "Remada");
        Map<Long, Double> m = servico.maximosDaFicha(List.of(a, b), List.of(reg(a, 5, 20.0), reg(a, 2, 30.0)));
        assertEquals(30.0, m.get(1L));
        assertFalse(m.containsKey(2L));
    }

    @Test
    void bateRecordeExigeCargaMaiorEHistorico() {
        assertTrue(RecordesService.bateRecorde(30.0, 32.5));
        assertFalse(RecordesService.bateRecorde(30.0, 30.0));
        assertFalse(RecordesService.bateRecorde(null, 40.0));
        assertFalse(RecordesService.bateRecorde(30.0, null));
    }
}
