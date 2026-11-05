package com.example.sistema.academia.util;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class ImagemValidadorTest {

    @Test
    void reconheceJpegPeloCabecalho() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
        assertEquals("image/jpeg", ImagemValidador.tipo(jpeg));
    }

    @Test
    void reconhecePngPeloCabecalho() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};
        assertEquals("image/png", ImagemValidador.tipo(png));
    }

    @Test
    void recusaArquivoQueNaoEImagem() {
        // Um script com nome de imagem não passa: só vale o conteúdo, não a extensão
        assertNull(ImagemValidador.tipo("<script>alert(1)</script>".getBytes()));
        assertNull(ImagemValidador.tipo("GIF89a".getBytes()));
        assertNull(ImagemValidador.tipo(new byte[0]));
        assertNull(ImagemValidador.tipo(null));
    }

    private static byte[] gerar(int largura, int altura, String formato) throws Exception {
        BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) img.setRGB(x, y, (x * 7 + y * 13) & 0xFFFFFF);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, formato, out);
        return out.toByteArray();
    }

    @Test
    void imagemGrandeFicaComNoMaximo800PixelsMantendoAProporcao() throws Exception {
        byte[] original = gerar(2000, 1000, "png");
        byte[] reduzida = ImagemValidador.reduzir(original, "image/png");
        BufferedImage lida = ImageIO.read(new ByteArrayInputStream(reduzida));
        assertEquals(800, lida.getWidth());
        assertEquals(400, lida.getHeight());
        assertEquals("image/png", ImagemValidador.tipo(reduzida));
        assertTrue(reduzida.length < original.length);
    }

    @Test
    void jpegGrandeContinuaJpegDepoisDeReduzir() throws Exception {
        byte[] reduzida = ImagemValidador.reduzir(gerar(1600, 1200, "jpg"), "image/jpeg");
        assertEquals("image/jpeg", ImagemValidador.tipo(reduzida));
        assertEquals(800, Math.max(ImageIO.read(new ByteArrayInputStream(reduzida)).getWidth(), 0));
    }

    @Test
    void imagemPequenaNaoEMexida() throws Exception {
        byte[] pequena = gerar(300, 200, "png");
        assertSame(pequena, ImagemValidador.reduzir(pequena, "image/png"));
    }

    @Test
    void limiteDeTamanhoEDeDoisMegas() {
        assertEquals(2 * 1024 * 1024, ImagemValidador.TAMANHO_MAXIMO);
    }
}
