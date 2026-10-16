package com.example.sistema.academia.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AutenticacaoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String caminho = request.getRequestURI();
        HttpSession session = request.getSession(false);
        String perfil = session != null ? (String) session.getAttribute("perfil") : null;

        if (perfil == null) {
            response.sendRedirect("/");
            return false;
        }

        boolean admin = "ADMIN".equals(perfil);
        boolean professor = "PROFESSOR".equals(perfil);
        boolean permitido;

        if (caminho.startsWith("/treinos") || caminho.startsWith("/exercicios")) {
            permitido = admin || professor;
        } else if (caminho.equals("/alterar-senha")) {
            permitido = !admin;
        } else {
            permitido = admin;
        }

        if (!permitido) {
            response.sendRedirect("/");
            return false;
        }
        return true;
    }
}
