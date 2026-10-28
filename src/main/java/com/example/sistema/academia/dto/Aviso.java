package com.example.sistema.academia.dto;

public class Aviso {
    private final String tipo;
    private final String texto;

    public Aviso(String tipo, String texto) {
        this.tipo = tipo;
        this.texto = texto;
    }

    public String getTipo() { return tipo; }
    public String getTexto() { return texto; }
}
