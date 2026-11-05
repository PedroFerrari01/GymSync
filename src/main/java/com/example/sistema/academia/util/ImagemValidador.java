package com.example.sistema.academia.util;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/** Confere imagens enviadas pelo usuário olhando os primeiros bytes, sem confiar no nome nem no tipo informado. */
public final class ImagemValidador {

    public static final long TAMANHO_MAXIMO = 2L * 1024 * 1024;
    /** Maior lado guardado, em pixels: foto de celular grande vira uma imagem leve para a tela. */
    public static final int LADO_MAXIMO = 800;

    private ImagemValidador() {}

    /** Diminui a imagem se ela passar de LADO_MAXIMO; se algo falhar, devolve a original. */
    public static byte[] reduzir(byte[] dados, String tipo) {
        try {
            BufferedImage origem = ImageIO.read(new ByteArrayInputStream(dados));
            if (origem == null) return dados;
            int maior = Math.max(origem.getWidth(), origem.getHeight());
            if (maior <= LADO_MAXIMO) return dados;
            double fator = (double) LADO_MAXIMO / maior;
            int largura = Math.max(1, (int) Math.round(origem.getWidth() * fator));
            int altura = Math.max(1, (int) Math.round(origem.getHeight() * fator));
            boolean png = "image/png".equals(tipo);
            BufferedImage saida = new BufferedImage(largura, altura, png ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            Graphics2D g = saida.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            if (!png) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, largura, altura);
            }
            g.drawImage(origem, 0, 0, largura, altura, null);
            g.dispose();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            if (!ImageIO.write(saida, png ? "png" : "jpg", bytes)) return dados;
            byte[] reduzida = bytes.toByteArray();
            return reduzida.length < dados.length ? reduzida : dados;
        } catch (Exception | Error e) {
            return dados;
        }
    }

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
