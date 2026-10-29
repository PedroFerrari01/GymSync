package com.example.sistema.academia.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Envio de e-mail por um de dois caminhos:
 * - Brevo (pela web, HTTPS): usado quando gymsync.email.brevo-chave está preenchida. Funciona em hospedagens
 *   que bloqueiam as portas de e-mail, como o plano grátis do Render.
 * - Gmail (SMTP): o spring.mail.* do application.properties, para uso no computador.
 */
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String remetenteSmtp;

    @Value("${gymsync.email.brevo-url:https://api.brevo.com/v3/smtp/email}")
    private String urlBrevo;

    @Value("${gymsync.email.brevo-chave:}")
    private String chaveBrevo;

    @Value("${gymsync.email.remetente:suportegymsync@gmail.com}")
    private String remetente;

    @Value("${gymsync.email.nome-remetente:Suporte GymSync}")
    private String nomeRemetente;

    private final RestClient http = RestClient.create();

    private boolean usaBrevo() {
        return chaveBrevo != null && !chaveBrevo.isBlank();
    }

    public boolean configurado() {
        if (usaBrevo()) return true;
        return mailSender != null && remetenteSmtp != null && !remetenteSmtp.isBlank();
    }

    public void enviar(String para, String assunto, String texto) {
        if (usaBrevo()) {
            enviarPeloBrevo(para, assunto, texto);
            return;
        }
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetenteSmtp);
        mensagem.setTo(para);
        mensagem.setSubject(assunto);
        mensagem.setText(texto);
        mailSender.send(mensagem);
    }

    private void enviarPeloBrevo(String para, String assunto, String texto) {
        Map<String, Object> corpo = Map.of(
                "sender", Map.of("name", nomeRemetente, "email", remetente),
                "replyTo", Map.of("email", remetente),
                "to", List.of(Map.of("email", para)),
                "subject", assunto,
                "textContent", texto);
        // Erro de envio (chave errada, limite do dia) vira exceção, tratada por quem chamou
        http.post()
                .uri(urlBrevo)
                .header("api-key", chaveBrevo)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .toBodilessEntity();
    }
}
