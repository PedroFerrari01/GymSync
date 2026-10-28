package com.example.sistema.academia.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AutenticacaoInterceptor autenticacaoInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autenticacaoInterceptor).addPathPatterns(
                "/alunos/**",
                "/aluno/**",
                "/professores/**",
                "/planos/**",
                "/matriculas/**",
                "/mensalidades/**",
                "/entradas/**",
                "/equipamentos/**",
                "/treinos/**",
                "/exercicios/**",
                "/avaliacoes/**",
                "/aulas/**",
                "/mensagens",
                "/mensagens/**",
                "/alterar-senha"
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
