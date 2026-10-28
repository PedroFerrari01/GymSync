package com.example.sistema.academia.dto;

public class Barra {
    private final String rotulo;
    private final String texto;
    private final int percentual;

    public Barra(String rotulo, String texto, int percentual) {
        this.rotulo = rotulo;
        this.texto = texto;
        this.percentual = percentual;
    }

    public String getRotulo() { return rotulo; }
    public String getTexto() { return texto; }
    public int getPercentual() { return percentual; }
}
