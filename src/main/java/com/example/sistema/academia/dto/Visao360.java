package com.example.sistema.academia.dto;

import java.time.LocalDate;
import java.util.List;

/** Retrato do aluno para admin e professor. Só leitura; sem senha, CPF nem texto de mensagem. */
public record Visao360(
        String nome,
        String plano,
        String situacaoMatricula,
        String email,
        String contato,
        int diasTreinados30,
        int mensalidadesEmAtraso,
        String ultimaAvaliacao,
        String ultimaEntrada,
        List<Evento> linhaDoTempo) {

    /** tipo: MATRICULA, PAGAMENTO, ATRASO, ANALISE, AVALIACAO, ENTRADAS, MENSAGEM. */
    public record Evento(LocalDate data, String dataTexto, String tipo, String titulo, String detalhe) {}
}
