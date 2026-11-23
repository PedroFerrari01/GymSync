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
    void sanearTiraScriptColadoDepoisDaImagem() throws Exception {
        byte[] png = gerar(40, 40, "png");
        byte[] polig = java.util.Arrays.copyOf(png, png.length + 25);
        byte[] script = "<script>alert(1)</script>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        System.arraycopy(script, 0, polig, png.length, script.length);
        byte[] limpa = ImagemValidador.sanear(polig, "image/png");
        assertNotNull(limpa);
        assertEquals("image/png", ImagemValidador.tipo(limpa));
        assertFalse(new String(limpa, java.nio.charset.StandardCharsets.ISO_8859_1).contains("<script>"));
    }

    @Test
    void saneaTambemImagemPequenaEJpeg() throws Exception {
        assertNotNull(ImageIO.read(new ByteArrayInputStream(ImagemValidador.sanear(gerar(300, 200, "png"), "image/png"))));
        byte[] jpg = ImagemValidador.sanear(gerar(300, 200, "jpg"), "image/jpeg");
        assertEquals("image/jpeg", ImagemValidador.tipo(jpg));
    }

    @Test
    void sanearDevolveNuloQuandoNaoEImagemLegivel() {
        assertNull(ImagemValidador.sanear("MZ nao e imagem".getBytes(java.nio.charset.StandardCharsets.UTF_8), "image/png"));
        assertNull(ImagemValidador.sanear(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3}, "image/png"));
    }

    @Test
    void limiteDeTamanhoEDeDoisMegas() {
        assertEquals(2 * 1024 * 1024, ImagemValidador.TAMANHO_MAXIMO);
    }

    @Test
    void recusaImagemComLadoMaiorQue4000Pixels() throws Exception {
        assertTrue(ImagemValidador.dimensoesAceitaveis(gerar(4000, 100, "png")));
        assertFalse(ImagemValidador.dimensoesAceitaveis(gerar(4001, 100, "png")));
        assertFalse(ImagemValidador.dimensoesAceitaveis(gerar(100, 4500, "jpg")));
    }

    @Test
    void dimensoesDeArquivoQueNaoEImagemSaoRecusadas() {
        assertFalse(ImagemValidador.dimensoesAceitaveis("texto qualquer".getBytes()));
    }
}
