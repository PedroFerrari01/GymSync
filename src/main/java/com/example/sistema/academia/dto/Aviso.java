package com.example.sistema.academia.dto;

public class Aviso {
    private final String tipo;
    private final String texto;
    // Alvo do link no painel do admin (ids vindos do servidor; null = sem link).
    // Os avisos do aluno nunca preenchem estes campos.
    private final Long alunoId;
    private final Long mensalidadeId;
    // Só no aviso de mensalidade perto do vencimento: abre o Pix direto do aviso
    private final String valor;
    private final String vencimento;
    // Só no aviso de equipamento: o link leva direto à linha do aparelho
    private final Long equipamentoId;

    public Aviso(String tipo, String texto) {
        this(tipo, texto, null, null, null, null, null);
    }

    public Aviso(String tipo, String texto, Long alunoId, Long mensalidadeId) {
        this(tipo, texto, alunoId, mensalidadeId, null, null, null);
    }

    public Aviso(String tipo, String texto, Long mensalidadeId, String valor, String vencimento) {
        this(tipo, texto, null, mensalidadeId, valor, vencimento, null);
    }

    public static Aviso doEquipamento(String tipo, String texto, Long equipamentoId) {
        return new Aviso(tipo, texto, null, null, null, null, equipamentoId);
    }

    private Aviso(String tipo, String texto, Long alunoId, Long mensalidadeId, String valor, String vencimento, Long equipamentoId) {
        this.tipo = tipo;
        this.texto = texto;
        this.alunoId = alunoId;
        this.mensalidadeId = mensalidadeId;
        this.valor = valor;
        this.vencimento = vencimento;
        this.equipamentoId = equipamentoId;
    }

    public String getTipo() { return tipo; }
    public String getTexto() { return texto; }
    public Long getAlunoId() { return alunoId; }
    public Long getMensalidadeId() { return mensalidadeId; }
    public Long getEquipamentoId() { return equipamentoId; }
    public String getValor() { return valor; }
    public String getVencimento() { return vencimento; }
}
