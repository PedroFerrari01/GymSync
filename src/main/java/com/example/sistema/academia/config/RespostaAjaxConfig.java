package com.example.sistema.academia.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Botões de alternar situação e excluir: quando o pedido vem do JavaScript (cabeçalho X-Requested-With: fetch),
 * em vez de redirecionar para a tela inicial (que renderiza a página inteira de novo), responde um JSON curto
 * com a mensagem. Sem o cabeçalho, nada muda: o formulário comum continua funcionando como antes.
 * Não decide acesso (isso é do SegurancaConfig): pedidos recusados nem chegam aqui.
 */
@Configuration
public class RespostaAjaxConfig implements WebMvcConfigurer {

    static final String CABECALHO = "X-Requested-With";
    static final String VALOR = "fetch";

    /** Só os botões da lista: /algo/status/{id} e /algo/excluir/{id}. */
    static boolean aplica(String metodo, String caminho, String cabecalho) {
        return "POST".equals(metodo) && VALOR.equals(cabecalho)
                && caminho.matches(".*/(status|excluir)/\\d+$");
    }

    static String json(boolean ok, String mensagem) {
        return "{\"ok\":" + ok + ",\"mensagem\":\"" + escapar(mensagem) + "\"}";
    }

    private static String escapar(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c < 0x20 ? " " : String.valueOf(c));
            }
        }
        return sb.toString();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView mv)
                    throws IOException {
                if (mv == null || mv.getViewName() == null || !mv.getViewName().startsWith("redirect:")) return;
                String caminho = request.getRequestURI().substring(request.getContextPath().length());
                if (!aplica(request.getMethod(), caminho, request.getHeader(CABECALHO))) return;

                FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
                Object erro = flash != null ? flash.get("erro") : null;
                Object sucesso = flash != null ? flash.get("sucesso") : null;
                String texto = erro != null ? erro.toString() : sucesso != null ? sucesso.toString() : "";

                // Sem renderizar o redirect: a mensagem vai no JSON e não fica guardada para a próxima página
                mv.clear();
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json;charset=UTF-8");
                response.getOutputStream().write(json(erro == null, texto).getBytes(StandardCharsets.UTF_8));
            }
        });
    }
}
