package com.example.sistema.academia.dto;

/** Uma linha do sininho: texto, cor (danger/warning/info) e para onde o clique leva (aba, linha ou página). */
public record Notificacao(String chave, String tipo, String texto, String aba, String alvo, String href) {

    public static Notificacao naAba(String tipo, String texto, String aba, String alvo) {
        return new Notificacao(chaveDe(tipo, texto), tipo, texto, aba, alvo, null);
    }

    public static Notificacao paraPagina(String tipo, String texto, String href) {
        return new Notificacao(chaveDe(tipo, texto), tipo, texto, null, null, href);
    }

    /** Identificador estável: o mesmo aviso gera a mesma chave, para a "lida" do navegador. */
    private static String chaveDe(String tipo, String texto) {
        return Integer.toHexString((tipo + "|" + texto).hashCode());
    }
}
