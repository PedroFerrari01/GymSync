package com.example.sistema.academia.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

@Configuration
@EnableWebSecurity
public class SegurancaConfig {

    /**
     * O navegador só roda scripts vindos do próprio site (nada de script escrito dentro do HTML nem de outro domínio).
     * Por isso os templates não usam onclick: os cliques ficam no gymsync.js.
     */
    static final String CSP = "default-src 'self'; "
            + "script-src 'self'; "
            + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
            + "font-src 'self' https://fonts.gstatic.com; "
            + "img-src 'self' data: blob:; "
            + "connect-src 'self'; "
            + "object-src 'none'; "
            + "base-uri 'self'; "
            + "form-action 'self'; "
            + "frame-ancestors 'none'";

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
            // Tudo bloqueado por padrão: a decisão vem de RegrasAcesso (CSRF fica ligado)
            .authorizeHttpRequests(auth -> auth.anyRequest().access((autenticacao, contexto) -> {
                HttpServletRequest req = contexto.getRequest();
                String caminho = req.getRequestURI().substring(req.getContextPath().length());
                HttpSession sessao = req.getSession(false);
                boolean trocarSenha = sessao != null && Boolean.TRUE.equals(sessao.getAttribute("trocarSenha"));
                if (trocarSenha && !RegrasAcesso.liberadaDuranteTrocaDeSenha(caminho)) return new AuthorizationDecision(false);
                boolean visitante = sessao != null && Boolean.TRUE.equals(sessao.getAttribute("visitante"));
                if (visitante && !RegrasAcesso.visitantePode(req.getMethod(), caminho)) return new AuthorizationDecision(false);
                return new AuthorizationDecision(RegrasAcesso.permitido(perfil(autenticacao.get()), caminho));
            }))
            // Sem login ou sem permissão: volta para a tela inicial, como antes
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> res.sendRedirect(req.getContextPath() + "/"))
                .accessDeniedHandler((req, res, e) -> {
                    HttpSession sessao = req.getSession(false);
                    if (sessao != null && Boolean.TRUE.equals(sessao.getAttribute("visitante"))) {
                        FlashMap aviso = new FlashMap();
                        aviso.put("erro", "Você está no modo visitante: dá para ver tudo, mas nada é salvo.");
                        new SessionFlashMapManager().saveOutputFlashMap(aviso, req, res);
                    }
                    res.sendRedirect(req.getContextPath() + "/");
                }))
            .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/").deleteCookies("JSESSIONID"))
            .headers(h -> h
                .frameOptions(f -> f.deny())
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                .contentSecurityPolicy(c -> c.policyDirectives(CSP))
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()")));
        return http.build();
    }

    private static String perfil(Authentication autenticacao) {
        if (autenticacao == null) return null;
        for (GrantedAuthority a : autenticacao.getAuthorities()) {
            String nome = a.getAuthority();
            if (nome.startsWith("ROLE_") && !nome.equals("ROLE_ANONYMOUS")) return nome.substring(5);
        }
        return null;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
