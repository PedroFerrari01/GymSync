package com.example.sistema.academia.config;

import jakarta.servlet.http.HttpServletRequest;
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

@Configuration
@EnableWebSecurity
public class SegurancaConfig {

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
            // Tudo bloqueado por padrão: a decisão vem de RegrasAcesso (CSRF fica ligado)
            .authorizeHttpRequests(auth -> auth.anyRequest().access((autenticacao, contexto) -> {
                HttpServletRequest req = contexto.getRequest();
                String caminho = req.getRequestURI().substring(req.getContextPath().length());
                return new AuthorizationDecision(RegrasAcesso.permitido(perfil(autenticacao.get()), caminho));
            }))
            // Sem login ou sem permissão: volta para a tela inicial, como antes
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> res.sendRedirect(req.getContextPath() + "/"))
                .accessDeniedHandler((req, res, e) -> res.sendRedirect(req.getContextPath() + "/")))
            .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/").deleteCookies("JSESSIONID"))
            .headers(h -> h
                .frameOptions(f -> f.deny())
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
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
