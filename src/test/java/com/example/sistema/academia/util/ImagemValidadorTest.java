package com.example.sistema.academia.util;

import org.junit.jupiter.api.Test;
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

    @Test
    void limiteDeTamanhoEDeDoisMegas() {
        assertEquals(2 * 1024 * 1024, ImagemValidador.TAMANHO_MAXIMO);
    }
}
