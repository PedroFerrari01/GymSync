package com.example.sistema.academia.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** Cor de destaque da academia: validação, contraste e CSS gerado (sem banco). */
class CorMarcaTest {

    @Test
    void normalizaOHexComOuSemJogoDaVelha() {
        assertEquals("#2563eb", CorMarca.normalizar("2563EB"));
        assertEquals("#2563eb", CorMarca.normalizar("  #2563eb "));
        assertNull(CorMarca.normalizar("azul"));
        assertNull(CorMarca.normalizar("#25f"));
        assertNull(CorMarca.normalizar("#12345g"));
        assertNull(CorMarca.normalizar(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"#f97316", "#2563eb", "#16a34a", "#7c3aed", "#dc2626", "#52525b"})
    void predefinicoesSaoAceitasEFicamLegiveisNosDoisTemas(String cor) {
        assertNull(CorMarca.erro(cor));
        int base = CorMarca.rgb(cor);
        int escuro = CorMarca.paraTemaEscuro(base);
        int claro = CorMarca.paraTemaClaro(base);
        assertTrue(CorMarca.contraste(escuro, 0x18181f) >= 4.5, "tema escuro " + cor);
        assertTrue(CorMarca.contraste(claro, 0xffffff) >= 4.5, "tema claro " + cor);
        assertTrue(CorMarca.contraste(escuro, CorMarca.textoSobre(escuro)) >= 4.5, "texto no escuro " + cor);
        assertTrue(CorMarca.contraste(claro, CorMarca.textoSobre(claro)) >= 4.5, "texto no claro " + cor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"#ffffff", "#ffff00", "#fde68a", "#e5e7eb"})
    void recusaCorClaraDemaisParaBotao(String cor) {
        String erro = CorMarca.erro(cor);
        assertNotNull(erro);
        assertTrue(erro.contains("clara demais"));
    }

    @Test
    void recusaPretoEHexInvalido() {
        assertNotNull(CorMarca.erro("#000000"));
        assertNotNull(CorMarca.erro("vermelho"));
        assertNotNull(CorMarca.erro(null));
    }

    @Test
    void laranjaPadraoNaoGeraCss() {
        assertTrue(CorMarca.ehPadrao(null));
        assertTrue(CorMarca.ehPadrao("#F97316"));
        assertFalse(CorMarca.ehPadrao("#2563eb"));
    }

    @Test
    void cssRedefineSoOsDestaquesNosDoisTemasENuncaAsCoresDeStatus() {
        String css = CorMarca.css("#2563eb");
        assertTrue(css.contains(":root[data-theme=\"dark\"]"));
        assertTrue(css.contains(":root[data-theme=\"light\"]"));
        assertTrue(css.contains("--accent:"));
        assertTrue(css.contains("--accent-hover:"));
        assertTrue(css.contains("--accent-soft:"));
        assertTrue(css.contains("--accent-texto:"));
        assertFalse(css.contains("status-"));
        assertFalse(css.contains("#047857"));
        assertFalse(css.contains("#b91c1c"));
    }

    @Test
    void textoSobreEscolheOMaisLegivel() {
        assertEquals(0xffffff, CorMarca.textoSobre(0x1d4ed8));
        assertEquals(0x0d0d10, CorMarca.textoSobre(0xf97316));
    }
}
