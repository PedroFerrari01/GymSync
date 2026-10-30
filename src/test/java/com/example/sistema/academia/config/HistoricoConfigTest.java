package com.example.sistema.academia.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/** O que entra no histórico de ações (HistoricoConfig.descrever). */
class HistoricoConfigTest {

    @Test
    void guardaAMensagemDeSucessoDosFormularios() {
        assertEquals("Ficha copiada para Ana com 3 exercício(s).",
                HistoricoConfig.descrever("POST", "/treinos/copiar", "Ficha copiada para Ana com 3 exercício(s).", null));
    }

    @Test
    void nuncaGuardaSenha() {
        String descricao = HistoricoConfig.descrever("POST", "/alunos/salvar",
                "Aluno cadastrado com sucesso! Senha provisória: Xk29abQw71 (será trocada no primeiro acesso).", null);
        assertEquals("Aluno cadastrado com sucesso!", descricao);
        assertFalse(descricao.contains("Xk29"));
        assertNull(HistoricoConfig.descrever("POST", "/alunos/redefinir-senha/3", "A senha de Ana voltou para 123.", null));
        assertNull(HistoricoConfig.descrever("POST", "/alterar-senha", "Sua senha foi atualizada com sucesso!", null));
    }

    @Test
    void exclusaoSemMensagemGanhaDescricao() {
        assertEquals("Excluiu exercício #12.", HistoricoConfig.descrever("POST", "/exercicios/excluir/12", null, null));
        assertEquals("Excluiu aluno #7.", HistoricoConfig.descrever("POST", "/alunos/excluir/7", null, null));
    }

    @Test
    void ignoraLeituraErroLoginECatraca() {
        assertNull(HistoricoConfig.descrever("GET", "/", "qualquer", null));
        assertNull(HistoricoConfig.descrever("POST", "/planos/salvar", null, "Informe o nome do plano."));
        assertNull(HistoricoConfig.descrever("POST", "/login", null, null));
        assertNull(HistoricoConfig.descrever("POST", "/entradas/registrar", "Entrada liberada!", null));
        assertNull(HistoricoConfig.descrever("POST", "/planos/salvar", null, null));
    }
}
