package com.example.sistema.academia.service;

import com.example.sistema.academia.model.ConfigAcademia;
import com.example.sistema.academia.model.ConfigAcademiaLogo;
import com.example.sistema.academia.repository.ConfigAcademiaLogoRepository;
import com.example.sistema.academia.repository.ConfigAcademiaRepository;
import com.example.sistema.academia.util.CorMarca;
import com.example.sistema.academia.util.ImagemValidador;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.zip.CRC32;

/**
 * Identidade da academia (cor de destaque, nome e logo). A configuração fica em memória
 * depois da primeira leitura, porque todas as telas consultam a cada página.
 */
@Service
public class MarcaService {

    /** Foto pronta para uso nas telas. cor null = laranja padrão (nenhum CSS extra). */
    public record Marca(String cor, String nome, boolean temLogo, boolean logoClara, String versao, String css) {
        public boolean temCor() { return cor != null; }
    }

    public record Logo(String tipo, byte[] dados, String etag) {}

    @Autowired private ConfigAcademiaRepository configRepository;
    @Autowired private ConfigAcademiaLogoRepository logoRepository;

    private volatile Marca cache;

    public Marca marca() {
        Marca atual = cache;
        if (atual == null) {
            atual = montar(configRepository.findById(ConfigAcademia.ID).orElse(null));
            cache = atual;
        }
        return atual;
    }

    private Marca montar(ConfigAcademia c) {
        if (c == null) return new Marca(null, null, false, false, "0", "");
        String cor = CorMarca.ehPadrao(c.getCor()) ? null : CorMarca.normalizar(c.getCor());
        String nome = c.getNome() == null || c.getNome().isBlank() ? null : c.getNome().trim();
        String versao = c.getAtualizadaEm() == null ? "0" : Long.toString(c.getAtualizadaEm().hashCode() & 0xffffffffL, 36);
        return new Marca(cor, nome, c.isTemLogo(), c.isLogoClara(), versao, cor == null ? "" : CorMarca.css(cor));
    }

    /**
     * Logo "clara" = os pixels visíveis são, em média, claros (letras brancas sobre fundo transparente, por exemplo).
     * Aí ela vai sobre um quadro escuro; as demais vão sobre o quadro claro.
     */
    static boolean logoEClara(byte[] dados) {
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(dados));
            if (img == null) return false;
            int passoX = Math.max(1, img.getWidth() / 60), passoY = Math.max(1, img.getHeight() / 60);
            double soma = 0, peso = 0;
            for (int y = 0; y < img.getHeight(); y += passoY) {
                for (int x = 0; x < img.getWidth(); x += passoX) {
                    int argb = img.getRGB(x, y);
                    double alfa = ((argb >>> 24) & 0xff) / 255.0;
                    if (alfa < 0.1) continue;
                    soma += alfa * com.example.sistema.academia.util.CorMarca.luminancia(argb & 0xffffff);
                    peso += alfa;
                }
            }
            return peso > 0 && soma / peso > 0.6;
        } catch (Exception | Error e) {
            return false;
        }
    }

    /** Valida o nome: até 60 caracteres, sem os símbolos de tag. Devolve a mensagem de erro ou null. */
    static String erroNome(String nome) {
        if (nome == null || nome.isBlank()) return null;
        if (nome.trim().length() > 60) return "O nome da academia pode ter no máximo 60 caracteres.";
        if (nome.contains("<") || nome.contains(">")) return "O nome da academia não pode ter os símbolos < e >.";
        return null;
    }

    /** Valida a logo (JPG ou PNG, até 2 MB, tamanho em pixels aceitável). Devolve a mensagem de erro ou null. */
    static String erroLogo(byte[] dados) {
        if (dados.length > ImagemValidador.TAMANHO_MAXIMO) return "A logo deve ter no máximo 2 MB.";
        if (ImagemValidador.tipo(dados) == null) return "Envie a logo em JPG ou PNG.";
        if (!ImagemValidador.dimensoesAceitaveis(dados)) {
            return "A logo é grande demais (máximo " + ImagemValidador.LADO_LIMITE_ENVIO + " px de largura ou altura).";
        }
        return null;
    }

    /**
     * Grava a identidade. Devolve null se deu certo ou a mensagem de erro (nada é gravado se algo for inválido).
     * logo null ou vazia = mantém a logo atual.
     */
    public String salvar(String cor, String nome, byte[] logo) throws IOException {
        String corNorm = CorMarca.normalizar(cor);
        String erro = CorMarca.erro(cor);
        if (erro == null) erro = erroNome(nome);
        if (erro == null && logo != null && logo.length > 0) erro = erroLogo(logo);
        if (erro != null) return erro;

        ConfigAcademia c = configRepository.findById(ConfigAcademia.ID).orElseGet(ConfigAcademia::new);
        c.setCor(CorMarca.ehPadrao(corNorm) ? null : corNorm);
        c.setNome(nome == null || nome.isBlank() ? null : nome.trim());
        if (logo != null && logo.length > 0) {
            String tipo = ImagemValidador.tipo(logo);
            logoRepository.save(new ConfigAcademiaLogo(tipo, ImagemValidador.reduzir(logo, tipo)));
            c.setTemLogo(true);
            c.setLogoClara(logoEClara(logo));
        }
        c.setAtualizadaEm(LocalDateTime.now());
        configRepository.save(c);
        cache = null;
        return null;
    }

    /** Remove cor, nome e logo: volta ao visual padrão. Não mexe em nenhum outro dado do sistema. */
    public void restaurarPadrao() {
        logoRepository.deleteById(ConfigAcademia.ID);
        configRepository.deleteById(ConfigAcademia.ID);
        cache = null;
    }

    public Logo logo() {
        if (!marca().temLogo()) return null;
        return logoRepository.findById(ConfigAcademia.ID).map(l -> {
            CRC32 crc = new CRC32();
            crc.update(l.getDados());
            return new Logo(l.getTipo(), l.getDados(), Long.toHexString(crc.getValue()) + "-" + l.getDados().length);
        }).orElse(null);
    }
}
