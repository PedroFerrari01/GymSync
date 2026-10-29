package com.example.sistema.academia.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Regras de senha do sistema.
 * Com {@code gymsync.senhas-fortes=false} (desenvolvimento) tudo funciona como antes: senha provisória 123,
 * mínimo de 4 caracteres e senha vazia no banco aceita 123.
 * Com {@code true} (publicação): mínimo de 8 caracteres, senha provisória aleatória e troca obrigatória no primeiro acesso.
 * Senhas que já estão salvas no banco não são alteradas em nenhum dos dois modos.
 */
@Service
public class SenhaService {

    private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int TAMANHO_PROVISORIA = 10;

    private final boolean fortes;
    private final SecureRandom aleatorio = new SecureRandom();

    @Autowired
    public SenhaService(@Value("${gymsync.senhas-fortes:false}") boolean fortes) {
        this.fortes = fortes;
    }

    public boolean fortes() {
        return fortes;
    }

    public int tamanhoMinimo() {
        return fortes ? 8 : 4;
    }

    /** Mensagem de erro se a senha nova não serve, ou null se está boa. */
    public String validarNova(String senha) {
        if (senha == null || senha.length() < tamanhoMinimo()) {
            return "A senha precisa ter pelo menos " + tamanhoMinimo() + " caracteres.";
        }
        if (fortes && senha.equals("123")) return "Escolha uma senha diferente de 123.";
        return null;
    }

    /** Senha provisória para cadastro sem senha ou redefinição pelo administrador. */
    public String provisoria() {
        if (!fortes) return "123";
        StringBuilder sb = new StringBuilder(TAMANHO_PROVISORIA);
        for (int i = 0; i < TAMANHO_PROVISORIA; i++) {
            sb.append(CARACTERES.charAt(aleatorio.nextInt(CARACTERES.length())));
        }
        return sb.toString();
    }

    /** No modo de desenvolvimento, conta antiga sem senha no banco entra com 123. */
    public boolean aceitaSenhaVaziaComo123() {
        return !fortes;
    }
}
