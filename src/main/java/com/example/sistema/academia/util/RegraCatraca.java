package com.example.sistema.academia.util;

import java.time.Duration;
import java.time.LocalDateTime;

/** Regras simples da catraca que não dependem de banco: evita registrar a mesma entrada várias vezes seguidas. */
public final class RegraCatraca {

    /** Tempo mínimo entre duas entradas do mesmo aluno; antes disso a leitura repetida não gera outro registro. */
    public static final long SEGUNDOS_ENTRE_ENTRADAS = 60;

    private RegraCatraca() {}

    /** Verdadeiro se a última entrada do aluno foi há menos de SEGUNDOS_ENTRE_ENTRADAS segundos. */
    public static boolean entradaRepetida(LocalDateTime ultimaEntrada, LocalDateTime agora) {
        if (ultimaEntrada == null || agora == null) return false;
        long segundos = Duration.between(ultimaEntrada, agora).getSeconds();
        return segundos >= 0 && segundos < SEGUNDOS_ENTRE_ENTRADAS;
    }
}
