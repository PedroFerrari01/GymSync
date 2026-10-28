package com.example.sistema.academia.dto;

public class Contato {
    private final Long id;
    private final String nome;
    private final String detalhe;
    private final long naoLidas;
    private final String whatsapp;

    public Contato(Long id, String nome, String detalhe, long naoLidas, String whatsapp) {
        this.id = id;
        this.nome = nome;
        this.detalhe = detalhe;
        this.naoLidas = naoLidas;
        this.whatsapp = whatsapp;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDetalhe() { return detalhe; }
    public long getNaoLidas() { return naoLidas; }
    public String getWhatsapp() { return whatsapp; }
}
