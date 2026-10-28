package com.example.sistema.academia.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Regras de acesso por perfil do AutenticacaoInterceptor.
 * Cada linha diz: perfil logado, caminho pedido e se o acesso deve ser liberado.
 */
class AutenticacaoInterceptorTest {

    private final AutenticacaoInterceptor interceptor = new AutenticacaoInterceptor();

    private boolean acessar(String perfil, String caminho, MockHttpServletResponse resposta) throws Exception {
        MockHttpServletRequest requisicao = new MockHttpServletRequest("POST", caminho);
        if (perfil != null) {
            MockHttpSession sessao = new MockHttpSession();
            sessao.setAttribute("perfil", perfil);
            requisicao.setSession(sessao);
        }
        return interceptor.preHandle(requisicao, resposta, new Object());
    }

    @Test
    void semLoginVoltaParaATelaInicial() throws Exception {
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        assertFalse(acessar(null, "/alunos/salvar", resposta));
        assertEquals("/", resposta.getRedirectedUrl());
    }

    @ParameterizedTest(name = "{0} em {1} -> {2}")
    @CsvSource({
            // Administrador: cadastros, financeiro, catraca, treinos e aulas
            "ADMIN, /alunos/salvar, true",
            "ADMIN, /professores/excluir/1, true",
            "ADMIN, /planos/salvar, true",
            "ADMIN, /matriculas/salvar, true",
            "ADMIN, /mensalidades/pagar/1, true",
            "ADMIN, /entradas/registrar, true",
            "ADMIN, /equipamentos/salvar, true",
            "ADMIN, /treinos/salvar, true",
            "ADMIN, /aulas/salvar, true",
            "ADMIN, /aluno/mensalidades/informar/1, false",
            "ADMIN, /aulas/reservar/1, false",
            "ADMIN, /mensagens, false",
            "ADMIN, /alterar-senha, false",

            // Professor: treinos, exercícios, avaliações, aulas e mensagens
            "PROFESSOR, /treinos/salvar, true",
            "PROFESSOR, /exercicios/salvar, true",
            "PROFESSOR, /avaliacoes/salvar, true",
            "PROFESSOR, /aulas/salvar, true",
            "PROFESSOR, /mensagens/enviar, true",
            "PROFESSOR, /alterar-senha, true",
            "PROFESSOR, /alunos/salvar, false",
            "PROFESSOR, /alunos/redefinir-senha/1, false",
            "PROFESSOR, /planos/salvar, false",
            "PROFESSOR, /mensalidades/pagar/1, false",
            "PROFESSOR, /entradas/registrar, false",
            "PROFESSOR, /equipamentos/salvar, false",
            "PROFESSOR, /aluno/ficha/email, false",
            "PROFESSOR, /aulas/reservar/1, false",

            // Aluno: área do aluno, reserva de aula, mensagens e senha
            "ALUNO, /aluno/mensalidades/informar/1, true",
            "ALUNO, /aluno/exercicios/registrar, true",
            "ALUNO, /aulas/reservar/1, true",
            "ALUNO, /aulas/cancelar-reserva/1, true",
            "ALUNO, /mensagens/enviar, true",
            "ALUNO, /alterar-senha, true",
            "ALUNO, /alunos/salvar, false",
            "ALUNO, /alunos/status/1, false",
            "ALUNO, /mensalidades/pagar/1, false",
            "ALUNO, /treinos/salvar, false",
            "ALUNO, /avaliacoes/salvar, false",
            "ALUNO, /aulas/salvar, false",
            "ALUNO, /entradas/registrar, false",

            // Perfil desconhecido na sessão não acessa nada
            "VISITANTE, /alunos/salvar, false",
            "VISITANTE, /aluno/ficha/email, false"
    })
    void respeitaAsPermissoesDeCadaPerfil(String perfil, String caminho, boolean esperado) throws Exception {
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        assertEquals(esperado, acessar(perfil, caminho, resposta));
        if (!esperado) {
            assertEquals("/", resposta.getRedirectedUrl());
        }
    }
}
