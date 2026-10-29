package com.example.sistema.academia.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueia temporariamente quem erra a senha várias vezes seguidas.
 * Cada chave (conta ou endereço de IP) tem o seu contador; os dados ficam só na memória.
 */
@Service
public class LimiteTentativasService {

    private record Registro(int falhas, Instant bloqueadoAte, Instant ultimaFalha) {}

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();
    private final Clock relogio;
    private final Duration bloqueio;

    @Autowired
    public LimiteTentativasService(@Value("${gymsync.login.bloqueio-minutos:15}") long bloqueioMinutos) {
        this(Duration.ofMinutes(bloqueioMinutos), Clock.systemUTC());
    }

    public LimiteTentativasService(Duration bloqueio, Clock relogio) {
        this.bloqueio = bloqueio;
        this.relogio = relogio;
    }

    /** Minutos que faltam para a chave voltar a poder tentar; 0 se ela não está bloqueada. */
    public long minutosRestantes(String chave) {
        Registro r = registros.get(chave);
        if (r == null || r.bloqueadoAte() == null) return 0;
        Duration resta = Duration.between(relogio.instant(), r.bloqueadoAte());
        if (resta.isNegative() || resta.isZero()) return 0;
        return Math.max(1, (resta.toSeconds() + 59) / 60);
    }

    public boolean bloqueada(String chave) {
        return minutosRestantes(chave) > 0;
    }

    /** Registra um erro de senha; a chave bloqueia ao chegar em {@code maxFalhas} erros seguidos. */
    public void registrarFalha(String chave, int maxFalhas) {
        Instant agora = relogio.instant();
        registros.compute(chave, (k, antigo) -> {
            // Sem histórico, bloqueio já cumprido ou falha antiga demais: recomeça a contagem
            boolean recomeca = antigo == null
                    || (antigo.bloqueadoAte() != null && !antigo.bloqueadoAte().isAfter(agora))
                    || antigo.ultimaFalha().plus(bloqueio).isBefore(agora);
            int falhas = recomeca ? 1 : antigo.falhas() + 1;
            return new Registro(falhas, falhas >= maxFalhas ? agora.plus(bloqueio) : null, agora);
        });
        limparAntigos(agora);
    }

    public void registrarSucesso(String chave) {
        registros.remove(chave);
    }

    private void limparAntigos(Instant agora) {
        if (registros.size() < 1000) return;
        registros.entrySet().removeIf(e -> e.getValue().ultimaFalha().plus(bloqueio).isBefore(agora));
    }
}
