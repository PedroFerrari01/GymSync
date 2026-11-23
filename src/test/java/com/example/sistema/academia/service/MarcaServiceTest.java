package com.example.sistema.academia.service;

import com.example.sistema.academia.model.ConfigAcademia;
import com.example.sistema.academia.repository.ConfigAcademiaLogoRepository;
import com.example.sistema.academia.repository.ConfigAcademiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/** Identidade da academia: validações e o que é (e não é) gravado, sem banco. */
class MarcaServiceTest {

    private final ConfigAcademiaRepository config = mock(ConfigAcademiaRepository.class);
    private final ConfigAcademiaLogoRepository logo = mock(ConfigAcademiaLogoRepository.class);
    private final MarcaService service = new MarcaService();

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(service, "configRepository", config);
        ReflectionTestUtils.setField(service, "logoRepository", logo);
        when(config.findById(ConfigAcademia.ID)).thenReturn(Optional.empty());
    }

    @Test
    void semConfiguracaoEOVisualPadrao() {
        var m = service.marca();
        assertFalse(m.temCor());
        assertFalse(m.temLogo());
        assertEquals("", m.css());
    }

    @Test
    void corInvalidaOuClaraNaoGravaNada() throws Exception {
        assertNotNull(service.salvar("amarelo", null, null));
        assertNotNull(service.salvar("#ffffff", null, null));
        verify(config, never()).save(any());
    }

    @Test
    void corValidaGrava() throws Exception {
        assertNull(service.salvar("#2563EB", "Academia Forte", null));
        verify(config).save(argThat(c -> "#2563eb".equals(c.getCor()) && "Academia Forte".equals(c.getNome())));
    }

    @Test
    void laranjaPadraoGravaSemCor() throws Exception {
        assertNull(service.salvar("#f97316", null, null));
        verify(config).save(argThat(c -> c.getCor() == null));
    }

    @Test
    void logoQueNaoEImagemEhRecusada() throws Exception {
        assertEquals("Envie a logo em JPG ou PNG.", service.salvar("#2563eb", null, "texto qualquer".getBytes()));
        verify(logo, never()).save(any());
        verify(config, never()).save(any());
    }

    @Test
    void logoMaiorQue2MBEhRecusada() throws Exception {
        byte[] grande = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);
        assertEquals("A logo deve ter no máximo 2 MB.", service.salvar("#2563eb", null, grande));
    }

    @Test
    void nomeComTagOuMuitoLongoEhRecusado() throws Exception {
        assertNotNull(service.salvar("#2563eb", "<script>", null));
        assertNotNull(service.salvar("#2563eb", "x".repeat(61), null));
        verify(config, never()).save(any());
    }

    private static byte[] png(int fundoArgb, int tracoArgb) throws Exception {
        var img = new java.awt.image.BufferedImage(40, 20, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 20; y++) for (int x = 0; x < 40; x++) img.setRGB(x, y, x > 10 && x < 30 && y > 5 && y < 15 ? tracoArgb : fundoArgb);
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    @Test
    void logoComLetrasBrancasSobreTransparenteEClara() throws Exception {
        assertTrue(MarcaService.logoEClara(png(0x00000000, 0xffffffff)));
    }

    @Test
    void logoEscuraOuComFundoBrancoNaoEClara() throws Exception {
        assertFalse(MarcaService.logoEClara(png(0x00000000, 0xff111827)));
        assertFalse(MarcaService.logoEClara(png(0xff2563eb, 0xff1e3a8a)));
        assertFalse(MarcaService.logoEClara("nao e imagem".getBytes()));
    }

    @Test
    void restaurarApagaSoAIdentidade() {
        service.restaurarPadrao();
        verify(config).deleteById(ConfigAcademia.ID);
        verify(logo).deleteById(ConfigAcademia.ID);
        verifyNoMoreInteractions(config, logo);
    }
}
