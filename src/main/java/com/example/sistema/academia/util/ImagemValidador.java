package com.example.sistema.academia.util;

/** Confere imagens enviadas pelo usuário olhando os primeiros bytes, sem confiar no nome nem no tipo informado. */
public final class ImagemValidador {

    public static final long TAMANHO_MAXIMO = 2L * 1024 * 1024;

    private ImagemValidador() {}

    /** Devolve "image/jpeg" ou "image/png"; qualquer outra coisa devolve null. */
    public static String tipo(byte[] dados) {
        if (dados == null) return null;
        if (dados.length > 3
                && (dados[0] & 0xFF) == 0xFF && (dados[1] & 0xFF) == 0xD8 && (dados[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (dados.length > png.length) {
            for (int i = 0; i < png.length; i++) {
                if (dados[i] != png[i]) return null;
            }
            return "image/png";
        }
        return null;
    }
}
