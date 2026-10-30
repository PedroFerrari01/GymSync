package com.example.sistema.academia.dto;

public class Aviso {
    private final String tipo;
    private final String texto;
    // Só no aviso de mensalidade perto do vencimento: abre o Pix direto do aviso
    private final Long mensalidadeId;
    private final String valor;
    private final String vencimento;

    public Aviso(String tipo, String texto) {
        this(tipo, texto, null, null, null);
    }

    public Aviso(String tipo, String texto, Long mensalidadeId, String valor, String vencimento) {
        this.tipo = tipo;
        this.texto = texto;
        this.mensalidadeId = mensalidadeId;
        this.valor = valor;
        this.vencimento = vencimento;
    }

    public String getTipo() { return tipo; }
    public String getTexto() { return texto; }
    public Long getMensalidadeId() { return mensalidadeId; }
    public String getValor() { return valor; }
    public String getVencimento() { return vencimento; }
}
