package com.example.sistema.academia.config;

/**
 * Quem pode acessar o quê. Perfil null (sem login) só acessa as rotas públicas.
 * Rota que não casar com nenhuma regra específica é só do administrador (bloqueado por padrão).
 */
public final class RegrasAcesso {

    private RegrasAcesso() {}

    public static boolean publica(String caminho) {
        return caminho.equals("/")
                || caminho.equals("/login")
                || caminho.equals("/recuperar-senha")
                || caminho.equals("/error")
                || caminho.startsWith("/css/")
                || caminho.startsWith("/js/")
                || caminho.startsWith("/img/")
                || caminho.startsWith("/vendor/");
    }

    public static boolean permitido(String perfil, String caminho) {
        if (publica(caminho)) return true;
        if (perfil == null) return false;

        boolean admin = "ADMIN".equals(perfil);
        boolean professor = "PROFESSOR".equals(perfil);
        boolean aluno = "ALUNO".equals(perfil);

        if (caminho.startsWith("/aluno/")
                || caminho.startsWith("/aulas/reservar")
                || caminho.startsWith("/aulas/cancelar-reserva")) {
            return aluno;
        }
        if (caminho.startsWith("/treinos")
                || caminho.startsWith("/exercicios")
                || caminho.startsWith("/avaliacoes")
                || caminho.startsWith("/aulas")) {
            return admin || professor;
        }
        if (caminho.equals("/alterar-senha") || caminho.startsWith("/mensagens")) {
            return aluno || professor;
        }
        return admin;
    }
}
