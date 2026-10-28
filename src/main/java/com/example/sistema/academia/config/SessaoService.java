package com.example.sistema.academia.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Abre a sessão do usuário logado: renova o id da sessão e registra o perfil no Spring Security. */
@Service
public class SessaoService {

    private final HttpSessionSecurityContextRepository repositorio = new HttpSessionSecurityContextRepository();

    public void iniciar(HttpServletRequest req, HttpServletResponse res, String perfil, String nome, Long usuarioId) {
        HttpSession sessao = req.getSession(true);
        req.changeSessionId(); // evita fixação de sessão
        sessao.setAttribute("perfil", perfil);
        sessao.setAttribute("nomeUsuario", nome);
        if (usuarioId != null) sessao.setAttribute("usuarioId", usuarioId);

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                nome, null, List.of(new SimpleGrantedAuthority("ROLE_" + perfil))));
        SecurityContextHolder.setContext(contexto);
        repositorio.saveContext(contexto, req, res);
    }
}
