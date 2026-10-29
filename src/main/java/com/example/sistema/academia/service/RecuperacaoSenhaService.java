package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.Professor;
import com.example.sistema.academia.model.TokenSenha;
import com.example.sistema.academia.repository.AlunoRepository;
import com.example.sistema.academia.repository.ProfessorRepository;
import com.example.sistema.academia.repository.TokenSenhaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Recuperação de senha por e-mail: gera um link de uso único que vale {@link #VALIDADE_MINUTOS} minutos.
 * No banco fica só o hash SHA-256 do código, então quem ler o banco não consegue usar o link.
 */
@Service
public class RecuperacaoSenhaService {

    public static final int VALIDADE_MINUTOS = 30;
    private static final Logger log = LoggerFactory.getLogger(RecuperacaoSenhaService.class);

    @Autowired private AlunoRepository alunoRepository;
    @Autowired private ProfessorRepository professorRepository;
    @Autowired private TokenSenhaRepository tokenRepository;
    @Autowired private EmailService emailService;
    @Autowired private SenhaService senhaService;
    @Autowired private PasswordEncoder encoder;

    @Value("${gymsync.url-base:http://localhost:8080}") private String urlBase;

    private final SecureRandom aleatorio = new SecureRandom();

    public boolean disponivel() {
        return emailService.configurado();
    }

    /**
     * Envia o link se achar a conta. Não diz se achou ou não, para ninguém descobrir quem é cadastrado.
     */
    public void solicitar(String perfil, String identificador) {
        String id = identificador == null ? "" : identificador.trim();
        if (id.isEmpty()) return;

        Long usuarioId = null;
        String nome = null;
        String email = null;
        if ("ALUNO".equals(perfil)) {
            Aluno a = alunoRepository.findFirstByCpfOrEmailIgnoreCase(id, id).orElse(null);
            if (a != null) { usuarioId = a.getId(); nome = a.getNome(); email = a.getEmail(); }
        } else if ("PROFESSOR".equals(perfil)) {
            Professor p = professorRepository.findFirstByCpfOrEmailIgnoreCase(id, id).orElse(null);
            if (p != null) { usuarioId = p.getId(); nome = p.getNome(); email = p.getEmail(); }
        }
        if (usuarioId == null || email == null || email.isBlank()) return;

        invalidarPendentes(perfil, usuarioId);
        String codigo = novoCodigo();
        TokenSenha token = new TokenSenha();
        token.setPerfil(perfil);
        token.setUsuarioId(usuarioId);
        token.setHash(hash(codigo));
        token.setExpiraEm(LocalDateTime.now().plusMinutes(VALIDADE_MINUTOS));
        tokenRepository.save(token);

        String link = urlBase.replaceAll("/+$", "") + "/redefinir-senha?token=" + codigo;
        String texto = "Olá, " + nome + "!\n\n"
                + "Recebemos um pedido para criar uma nova senha no GymSync.\n"
                + "Para continuar, abra o link abaixo (vale por " + VALIDADE_MINUTOS + " minutos e só pode ser usado uma vez):\n\n"
                + link + "\n\n"
                + "Se não foi você, ignore este e-mail: sua senha continua a mesma.\n\n"
                + "Suporte GymSync";
        try {
            emailService.enviar(email, "GymSync - redefinir sua senha", texto);
        } catch (Exception e) {
            log.warn("Falha ao enviar o e-mail de recuperação de senha: {}", e.getMessage());
        }
    }

    /** O token ainda serve? (existe, não foi usado e não venceu) */
    public TokenSenha valido(String codigo) {
        if (codigo == null || codigo.isBlank()) return null;
        TokenSenha t = tokenRepository.findByHash(hash(codigo)).orElse(null);
        if (t == null || t.isUsado() || t.getExpiraEm().isBefore(LocalDateTime.now())) return null;
        return t;
    }

    /** Troca a senha pelo link. Devolve uma mensagem de erro, ou null se deu certo. */
    public String redefinir(String codigo, String novaSenha) {
        TokenSenha t = valido(codigo);
        if (t == null) return "Este link é inválido ou já venceu. Peça um novo em \"Esqueci minha senha\".";
        String erro = senhaService.validarNova(novaSenha);
        if (erro != null) return erro;

        if ("ALUNO".equals(t.getPerfil())) {
            Aluno a = alunoRepository.findById(t.getUsuarioId()).orElse(null);
            if (a == null) return "Conta não encontrada.";
            a.setSenha(encoder.encode(novaSenha));
            a.setTrocarSenha(false);
            alunoRepository.save(a);
        } else {
            Professor p = professorRepository.findById(t.getUsuarioId()).orElse(null);
            if (p == null) return "Conta não encontrada.";
            p.setSenha(encoder.encode(novaSenha));
            p.setTrocarSenha(false);
            professorRepository.save(p);
        }
        invalidarPendentes(t.getPerfil(), t.getUsuarioId());
        return null;
    }

    private void invalidarPendentes(String perfil, Long usuarioId) {
        for (TokenSenha antigo : tokenRepository.findByPerfilAndUsuarioIdAndUsadoFalse(perfil, usuarioId)) {
            antigo.setUsado(true);
            tokenRepository.save(antigo);
        }
    }

    private String novoCodigo() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String codigo) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(codigo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
