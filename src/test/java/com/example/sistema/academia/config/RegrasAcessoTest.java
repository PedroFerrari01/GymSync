package com.example.sistema.academia.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Regras de acesso por perfil (RegrasAcesso), usadas pelo Spring Security.
 * Cada linha diz: perfil logado, caminho pedido e se o acesso deve ser liberado.
 */
class RegrasAcessoTest {

    @Test
    void semLoginSoAcessaRotasPublicas() {
        assertTrue(RegrasAcesso.permitido(null, "/"));
        assertTrue(RegrasAcesso.permitido(null, "/login"));
        assertTrue(RegrasAcesso.permitido(null, "/recuperar-senha"));
        assertTrue(RegrasAcesso.permitido(null, "/redefinir-senha"));
        assertTrue(RegrasAcesso.permitido(null, "/css/gymsync-tema.css"));
        // Vídeo de apresentação do "Quem somos" (fica na tela de login, antes de entrar)
        assertTrue(RegrasAcesso.permitido(null, "/video/gymsync.mp4"));
        // Modelo 3D do mapa do corpo (arquivo estático, sem dados de ninguém)
        assertTrue(RegrasAcesso.permitido(null, "/models/corpo-musculos.glb"));
        assertTrue(RegrasAcesso.permitido("ALUNO", "/models/corpo-musculos.glb"));
        // Instalar no celular: o navegador busca estes arquivos sem a sessão do usuário
        assertTrue(RegrasAcesso.permitido(null, "/manifest.json"));
        assertTrue(RegrasAcesso.permitido(null, "/sw.js"));
        assertTrue(RegrasAcesso.permitido(null, "/offline.html"));
        // Buscadores leem estes dois arquivos sem login
        assertTrue(RegrasAcesso.permitido(null, "/robots.txt"));
        assertTrue(RegrasAcesso.permitido(null, "/sitemap.xml"));
        // LGPD: a política é pública; aceite e "baixar meus dados" são só do aluno logado
        assertTrue(RegrasAcesso.permitido(null, "/privacidade"));
        assertTrue(RegrasAcesso.permitido(null, "/error"));
        assertFalse(RegrasAcesso.permitido(null, "/aluno/meus-dados"));
        assertTrue(RegrasAcesso.permitido("ALUNO", "/aluno/meus-dados"));
        assertTrue(RegrasAcesso.permitido("ALUNO", "/aluno/aceitar-privacidade"));
        assertTrue(RegrasAcesso.permitido("ALUNO", "/aluno/privacidade/revogar"));
        assertTrue(RegrasAcesso.permitido("ALUNO", "/aluno/privacidade/pedir-exclusao"));
        assertFalse(RegrasAcesso.permitido("PROFESSOR", "/aluno/privacidade/pedir-exclusao"));
        assertFalse(RegrasAcesso.visitantePode("POST", "/aluno/privacidade/pedir-exclusao"));
        assertFalse(RegrasAcesso.permitido("PROFESSOR", "/aluno/meus-dados"));
        assertFalse(RegrasAcesso.permitido("ADMIN", "/aluno/meus-dados"));
        assertFalse(RegrasAcesso.permitido(null, "/alunos/salvar"));
        assertFalse(RegrasAcesso.permitido(null, "/aluno/ficha/email"));
        assertFalse(RegrasAcesso.permitido(null, "/mensagens"));
        assertFalse(RegrasAcesso.permitido(null, "/rota-inventada/salvar"));
    }

    @Test
    void comSenhaProvisoriaSoTrocaASenhaOuSai() {
        assertTrue(RegrasAcesso.liberadaDuranteTrocaDeSenha("/"));
        assertTrue(RegrasAcesso.liberadaDuranteTrocaDeSenha("/alterar-senha"));
        assertTrue(RegrasAcesso.liberadaDuranteTrocaDeSenha("/logout"));
        assertFalse(RegrasAcesso.liberadaDuranteTrocaDeSenha("/mensagens"));
        assertFalse(RegrasAcesso.liberadaDuranteTrocaDeSenha("/aluno/exercicios/registrar"));
    }

    @Test
    void visitanteSoOlha() {
        assertTrue(RegrasAcesso.permitido(null, "/visitante"));
        assertTrue(RegrasAcesso.visitantePode("GET", "/"));
        assertTrue(RegrasAcesso.visitantePode("GET", "/mensagens"));
        assertTrue(RegrasAcesso.visitantePode("POST", "/logout"));
        assertFalse(RegrasAcesso.visitantePode("POST", "/alunos/salvar"));
        assertFalse(RegrasAcesso.visitantePode("POST", "/mensalidades/pagar/1"));
        assertFalse(RegrasAcesso.visitantePode("POST", "/alterar-senha"));
    }

    @Test
    void visitanteNaoBaixaPlanilhas() {
        assertFalse(RegrasAcesso.visitantePode("GET", "/relatorios/exportar/alunos"));
        assertFalse(RegrasAcesso.visitantePode("GET", "/relatorios/exportar/mensalidades"));
        assertTrue(RegrasAcesso.visitantePode("GET", "/relatorios"));
        assertTrue(RegrasAcesso.permitido("ADMIN", "/relatorios/exportar/alunos")); // o admin de verdade continua podendo
    }

    @Test
    void rotaDesconhecidaEDoAdministrador() {
        assertTrue(RegrasAcesso.permitido("ADMIN", "/rota-inventada/salvar"));
        assertFalse(RegrasAcesso.permitido("PROFESSOR", "/rota-inventada/salvar"));
        assertFalse(RegrasAcesso.permitido("ALUNO", "/rota-inventada/salvar"));
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
            "ADMIN, /equipamentos/imagem/1, true",
            "ADMIN, /treinos/salvar, true",
            "ADMIN, /aulas/salvar, true",
            "ADMIN, /relatorios/exportar/alunos, true",
            "PROFESSOR, /relatorios/exportar/alunos, false",
            "ALUNO, /relatorios/exportar/mensalidades, false",
            "ADMIN, /aluno/mensalidades/informar/1, false",
            "ADMIN, /aulas/reservar/1, false",
            "ADMIN, /mensagens, false",
            "ADMIN, /alterar-senha, false",

            // Professor: treinos, exercícios, avaliações, aulas e mensagens
            "PROFESSOR, /treinos/salvar, true",
            "PROFESSOR, /treinos/copiar, true",
            "ALUNO, /treinos/copiar, false",
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
            "PROFESSOR, /equipamentos/imagem/1, false",
            "ALUNO, /equipamentos/imagem/1, false",
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
    void respeitaAsPermissoesDeCadaPerfil(String perfil, String caminho, boolean esperado) {
        assertEquals(esperado, RegrasAcesso.permitido(perfil, caminho));
    }

    @Test
    void carteirinhaNaCatracaEhSoDoAdminENaoDoVisitante() {
        assertTrue(RegrasAcesso.permitido("ADMIN", "/entradas/carteirinha"));
        assertFalse(RegrasAcesso.permitido("ALUNO", "/entradas/carteirinha"));
        assertFalse(RegrasAcesso.permitido("PROFESSOR", "/entradas/carteirinha"));
        assertFalse(RegrasAcesso.permitido(null, "/entradas/carteirinha"));
        assertFalse(RegrasAcesso.visitantePode("POST", "/entradas/carteirinha"));
    }

    @Test
    void codigoNovoDaCarteirinhaEhSoDoAlunoEFuncionaParaVisitante() {
        String caminho = "/aluno/carteirinha/codigo";
        assertTrue(RegrasAcesso.permitido("ALUNO", caminho));
        assertFalse(RegrasAcesso.permitido("PROFESSOR", caminho));
        assertFalse(RegrasAcesso.permitido("ADMIN", caminho));
        assertFalse(RegrasAcesso.permitido(null, caminho));
        // O visitante só faz GET, e é o que a tela usa para renovar o QR
        assertTrue(RegrasAcesso.visitantePode("GET", caminho));
        assertFalse(RegrasAcesso.visitantePode("POST", caminho));
    }

    @Test
    void leitorPelaWebcamEhSoDoAdmin() {
        assertTrue(RegrasAcesso.permitido("ADMIN", "/catraca/leitor"));
        assertFalse(RegrasAcesso.permitido("ALUNO", "/catraca/leitor"));
        assertFalse(RegrasAcesso.permitido("PROFESSOR", "/catraca/leitor"));
        assertFalse(RegrasAcesso.permitido(null, "/catraca/leitor"));
    }

    @Test
    void cameraSoNoLeitorDaCatraca() {
        assertTrue(SegurancaConfig.politicaPermissoes("/catraca/leitor").contains("camera=(self)"));
        for (String caminho : new String[] {"/", "/aluno/meus-dados", "/catraca/leitor/", "/catraca/leitorx", "/mensagens"}) {
            String politica = SegurancaConfig.politicaPermissoes(caminho);
            assertTrue(politica.contains("camera=()"), caminho);
            assertFalse(politica.contains("camera=(self)"), caminho);
        }
        // Microfone, localização e pagamento continuam bloqueados até no leitor
        assertTrue(SegurancaConfig.politicaPermissoes("/catraca/leitor").contains("microphone=()"));
        assertTrue(SegurancaConfig.politicaPermissoes("/catraca/leitor").contains("geolocation=()"));
        assertTrue(SegurancaConfig.politicaPermissoes("/catraca/leitor").contains("payment=()"));
    }

    @Test
    void reciboEDoAdminEDoAlunoMasNaoDoProfessor() {
        assertTrue(RegrasAcesso.permitido("ADMIN", "/mensalidades/5/recibo"));
        assertTrue(RegrasAcesso.permitido("ALUNO", "/mensalidades/5/recibo"));
        assertFalse(RegrasAcesso.permitido("PROFESSOR", "/mensalidades/5/recibo"));
        assertFalse(RegrasAcesso.permitido(null, "/mensalidades/5/recibo"));
        // O resto de /mensalidades continua só do admin
        assertFalse(RegrasAcesso.permitido("ALUNO", "/mensalidades/pagar/5"));
        // Visitante só faz GET, então vê o recibo
        assertTrue(RegrasAcesso.visitantePode("GET", "/mensalidades/5/recibo"));
    }

    @Test
    void pixDaMensalidadeEhSoDoAlunoEVisitantePodeVer() {
        assertTrue(RegrasAcesso.permitido("ALUNO", "/aluno/mensalidades/5/pix"));
        assertFalse(RegrasAcesso.permitido("PROFESSOR", "/aluno/mensalidades/5/pix"));
        assertFalse(RegrasAcesso.permitido("ADMIN", "/aluno/mensalidades/5/pix"));
        assertFalse(RegrasAcesso.permitido(null, "/aluno/mensalidades/5/pix"));
        assertTrue(RegrasAcesso.visitantePode("GET", "/aluno/mensalidades/5/pix"));
    }

    @Test
    void visao360EhDeAdminEProfessorEVisitantePodeVer() {
        assertTrue(RegrasAcesso.permitido("ADMIN", "/alunos/7/visao-360"));
        assertTrue(RegrasAcesso.permitido("PROFESSOR", "/alunos/7/visao-360"));
        assertFalse(RegrasAcesso.permitido("ALUNO", "/alunos/7/visao-360"));
        assertFalse(RegrasAcesso.permitido(null, "/alunos/7/visao-360"));
        // O resto de /alunos continua só do admin
        assertFalse(RegrasAcesso.permitido("PROFESSOR", "/alunos/excluir/7"));
        assertFalse(RegrasAcesso.permitido("PROFESSOR", "/alunos/7/visao-360/../excluir"));
        assertTrue(RegrasAcesso.visitantePode("GET", "/alunos/7/visao-360"));
    }
}
