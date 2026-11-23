package com.example.sistema.academia.service;

import com.example.sistema.academia.model.EsperaAula;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Regras da lista de espera (sem banco: só listas e números). A fila chega já na ordem de entrada. */
public final class EsperaFila {

    public enum Resultado { OK, NAO_ESTA_LOTADA, JA_RESERVOU, JA_NA_FILA, DATA_INVALIDA }

    private EsperaFila() {}

    /** Pode entrar na fila? Só aula lotada, em data válida, de quem não reservou nem já está na fila. */
    public static Resultado podeEntrar(boolean dataValida, int livres, boolean jaReservou, boolean jaNaFila) {
        if (!dataValida) return Resultado.DATA_INVALIDA;
        if (jaReservou) return Resultado.JA_RESERVOU;
        if (jaNaFila) return Resultado.JA_NA_FILA;
        if (livres > 0) return Resultado.NAO_ESTA_LOTADA;
        return Resultado.OK;
    }

    public static String mensagem(Resultado r) {
        return switch (r) {
            case OK -> "";
            case NAO_ESTA_LOTADA -> "Esta aula tem vaga: reserve direto pela grade.";
            case JA_RESERVOU -> "Você já tem reserva nesta aula.";
            case JA_NA_FILA -> "Você já está na lista de espera desta aula.";
            case DATA_INVALIDA -> "Esta aula já passou ou ainda não abriu para reserva.";
        };
    }

    /** Posição (1 = primeiro) do aluno na fila, ou 0 se não está. */
    public static int posicao(List<EsperaAula> fila, Long alunoId) {
        if (fila == null || alunoId == null) return 0;
        for (int i = 0; i < fila.size(); i++) {
            if (fila.get(i).getAluno().getId().equals(alunoId)) return i + 1;
        }
        return 0;
    }

    /**
     * Abriu vaga: avisa os primeiros da fila (um por vaga livre) que ainda não foram avisados.
     * Quem já foi avisado e ainda não reservou continua ocupando a vez dele. Devolve quem foi avisado agora.
     */
    public static List<EsperaAula> promover(List<EsperaAula> fila, int livres, LocalDateTime agora) {
        List<EsperaAula> novos = new ArrayList<>();
        if (fila == null || livres <= 0) return novos;
        int avisados = (int) fila.stream().filter(e -> e.getNotificadaEm() != null).count();
        for (EsperaAula e : fila) {
            if (avisados >= livres) break;
            if (e.getNotificadaEm() != null) continue;
            e.setNotificadaEm(agora);
            novos.add(e);
            avisados++;
        }
        return novos;
    }
}
