package com.example.sistema.academia.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RespostaAjaxConfigTest {

    @Test
    void aplicaSoParaStatusEExcluirPostComCabecalho() {
        assertTrue(RespostaAjaxConfig.aplica("POST", "/alunos/status/7", "fetch"));
        assertTrue(RespostaAjaxConfig.aplica("POST", "/equipamentos/manutencao/excluir/3", "fetch"));
        assertFalse(RespostaAjaxConfig.aplica("POST", "/alunos/status/7", null));
        assertFalse(RespostaAjaxConfig.aplica("GET", "/alunos/status/7", "fetch"));
        assertFalse(RespostaAjaxConfig.aplica("POST", "/alunos/salvar", "fetch"));
        assertFalse(RespostaAjaxConfig.aplica("POST", "/alunos/excluir/abc", "fetch"));
        assertFalse(RespostaAjaxConfig.aplica("POST", "/logout", "fetch"));
    }

    @Test
    void jsonEscapaAspasQuebrasDeLinhaEBarras() {
        assertEquals("{\"ok\":true,\"mensagem\":\"\"}", RespostaAjaxConfig.json(true, null));
        assertEquals("{\"ok\":false,\"mensagem\":\"a \\\"b\\\" \\\\ c\\nd\"}", RespostaAjaxConfig.json(false, "a \"b\" \\ c\nd"));
    }
}
