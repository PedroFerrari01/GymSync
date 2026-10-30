package com.example.sistema.academia.config;

import com.example.sistema.academia.model.HistoricoAcao;
import com.example.sistema.academia.repository.HistoricoAcaoRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.support.RequestContextUtils;
import java.util.Map;

/**
 * Histórico de ações: depois de cada formulário enviado (POST) que deu certo, guarda a mensagem verde
 * de sucesso que a tela mostra, com quem fez e quando. Só registra; não decide acesso (isso é do SegurancaConfig).
 */
@Configuration
public class HistoricoConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(HistoricoConfig.class);

    private static final Map<String, String> NOMES = Map.ofEntries(
            Map.entry("alunos", "aluno"), Map.entry("professores", "professor"), Map.entry("planos", "plano"),
            Map.entry("matriculas", "matrícula"), Map.entry("mensalidades", "mensalidade"), Map.entry("treinos", "ficha de treino"),
            Map.entry("exercicios", "exercício"), Map.entry("avaliacoes", "avaliação física"), Map.entry("aulas", "aula coletiva"),
            Map.entry("equipamentos", "equipamento"), Map.entry("manutencoes", "manutenção"), Map.entry("mensagens", "mensagem"));

    private final HistoricoAcaoRepository repositorio;

    public HistoricoConfig(HistoricoAcaoRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * O que vai para o histórico, ou null para não guardar. Fora: login, senhas e catraca
     * (a catraca já tem a lista de entradas) e qualquer ação que terminou em erro.
     */
    static String descrever(String metodo, String caminho, Object sucesso, Object erro) {
        if (!"POST".equals(metodo) || erro != null) return null;
        if (caminho.equals("/login") || caminho.equals("/logout") || caminho.equals("/visitante")
                || caminho.contains("senha") || caminho.startsWith("/entradas/")) {
            return null;
        }
        if (sucesso != null && !sucesso.toString().isBlank()) {
            // No cadastro com senhas fortes a mensagem traz a senha provisória: nada de senha no histórico
            String texto = sucesso.toString();
            int senha = texto.toLowerCase().indexOf("senha");
            if (senha >= 0) texto = texto.substring(0, senha);
            texto = texto.strip();
            return texto.isEmpty() ? null : texto;
        }

        String[] partes = caminho.split("/");
        if (partes.length >= 4 && "excluir".equals(partes[2])) {
            return "Excluiu " + NOMES.getOrDefault(partes[1], partes[1]) + " #" + partes[3] + ".";
        }
        return null;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView mv) {
                FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
                String descricao = descrever(request.getMethod(), request.getRequestURI(),
                        flash != null ? flash.get("sucesso") : null, flash != null ? flash.get("erro") : null);
                HttpSession sessao = request.getSession(false);
                if (descricao == null || sessao == null) return;
                try {
                    HistoricoAcao acao = new HistoricoAcao();
                    acao.setUsuario(String.valueOf(sessao.getAttribute("nomeUsuario")));
                    acao.setPerfil(String.valueOf(sessao.getAttribute("perfil")));
                    acao.setDescricao(descricao.length() > 300 ? descricao.substring(0, 300) : descricao);
                    repositorio.save(acao);
                } catch (RuntimeException e) {
                    // O histórico nunca derruba a ação que já foi feita
                    log.warn("Não foi possível guardar no histórico: {}", e.getMessage());
                }
            }
        });
    }
}
