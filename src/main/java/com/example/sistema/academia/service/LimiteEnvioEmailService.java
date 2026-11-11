package com.example.sistema.academia.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita quantos e-mails cada usuário pode disparar por hora (padrão: 3), para o botão de enviar a ficha
 * não virar uma forma de encher a caixa de alguém ou gastar a cota do serviço de e-mail. Só na memória.
 */
@Service
public class LimiteEnvioEmailService {

    private final Map<String, Deque<Instant>> envios = new ConcurrentHashMap<>();
    private final Clock relogio;
    private final int maximo;
    private final Duration janela;

    @Autowired
    public LimiteEnvioEmailService(@Value("${gymsync.email.max-por-hora:3}") int maximo) {
        this(maximo, Duration.ofHours(1), Clock.systemUTC());
    }

    public LimiteEnvioEmailService(int maximo, Duration janela, Clock relogio) {
        this.maximo = maximo;
        this.janela = janela;
        this.relogio = relogio;
    }

    /** Se ainda cabe um envio para a chave, conta o envio e devolve true; senão devolve false. */
    public boolean permitir(String chave) {
        Instant agora = relogio.instant();
        boolean[] ok = {false};
        envios.compute(chave, (k, fila) -> {
            Deque<Instant> f = fila != null ? fila : new ArrayDeque<>();
            while (!f.isEmpty() && f.peekFirst().plus(janela).isBefore(agora)) f.pollFirst();
            if (f.size() < maximo) {
                f.addLast(agora);
                ok[0] = true;
            }
            return f;
        });
        if (envios.size() > 1000) {
            envios.entrySet().removeIf(e -> e.getValue().isEmpty() || e.getValue().peekLast().plus(janela).isBefore(agora));
        }
        return ok[0];
    }
}
