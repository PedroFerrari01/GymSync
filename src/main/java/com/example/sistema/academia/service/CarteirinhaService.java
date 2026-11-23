package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Aluno;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Carteirinha digital (simulada): o QR leva um código assinado (HMAC-SHA256) com o id do aluno e o
 * horário de emissão, nunca o CPF. Formato: GS2.{id}.{emitido em segundos}.{assinatura}.
 * O código vale por poucos minutos (a tela do aluno pede um novo antes de vencer), então foto ou
 * print do QR deixa de servir logo depois. Sem a chave do servidor não dá para inventar um código.
 */
@Service
public class CarteirinhaService {

    private static final Logger log = LoggerFactory.getLogger(CarteirinhaService.class);

    private static final String PREFIXO = "GS2";
    private static final String PREFIXO_ANTIGO = "GS1"; // sem horário: nunca expirava, hoje é recusado
    /** Quanto tempo um código vale depois de emitido. */
    public static final long VALIDADE_SEGUNDOS = 300;
    /** Diferença de relógio aceita para um código "do futuro". */
    private static final long TOLERANCIA_RELOGIO_SEGUNDOS = 30;

    public enum Situacao { VALIDO, EXPIRADO, NO_FUTURO, FORMATO_ANTIGO, INVALIDO }

    /** Resultado da leitura de um código; o id do aluno só vem quando o código é válido. */
    public record Leitura(Situacao situacao, Long alunoId) {
        public boolean valido() { return situacao == Situacao.VALIDO; }

        /** Mensagem para a catraca quando o código é recusado. */
        public String motivoRecusa() {
            return switch (situacao) {
                case EXPIRADO -> "carteirinha vencida: peça para o aluno abrir a carteirinha de novo no celular.";
                case NO_FUTURO -> "horário do código inválido (data no futuro): confira o relógio do aparelho.";
                case FORMATO_ANTIGO -> "carteirinha desatualizada: peça para o aluno abrir a carteirinha de novo no celular.";
                default -> "código da carteirinha inválido.";
            };
        }
    }

    private final byte[] chave;
    private final Clock relogio;

    @Autowired
    public CarteirinhaService(@Value("${gymsync.carteirinha.chave:}") String chaveConfig) {
        this(chaveConfig, Clock.systemUTC());
    }

    CarteirinhaService(String chaveConfig, Clock relogio) {
        this.relogio = relogio;
        if (chaveConfig == null || chaveConfig.isBlank()) {
            // Sem chave configurada, sorteia uma: os códigos valem até o servidor reiniciar.
            log.warn("gymsync.carteirinha.chave não configurada: o sistema sorteou uma chave, e as "
                    + "carteirinhas abertas deixam de valer quando o servidor reiniciar. Defina a chave (no site publicado: CARTEIRINHA_CHAVE).");
            byte[] b = new byte[32];
            new SecureRandom().nextBytes(b);
            this.chave = b;
        } else {
            if (chaveConfig.length() < 16) {
                log.warn("gymsync.carteirinha.chave tem menos de 16 caracteres: use uma chave longa e aleatória.");
            }
            this.chave = chaveConfig.getBytes(StandardCharsets.UTF_8);
        }
    }

    /** Código novo, emitido agora; vale por {@link #VALIDADE_SEGUNDOS} segundos. */
    public String token(Long alunoId) {
        long emitido = relogio.instant().getEpochSecond();
        return PREFIXO + "." + alunoId + "." + emitido + "." + assinatura(alunoId, emitido);
    }

    /** Confere assinatura e horário do código (primeiro a assinatura, para um horário forjado não dar pistas). */
    public Leitura ler(String codigo) {
        Leitura invalido = new Leitura(Situacao.INVALIDO, null);
        if (codigo == null) return invalido;
        String[] partes = codigo.trim().split("[.]", -1);
        if (partes.length == 3 && PREFIXO_ANTIGO.equals(partes[0])) {
            return new Leitura(Situacao.FORMATO_ANTIGO, null);
        }
        if (partes.length != 4 || !PREFIXO.equals(partes[0])
                || !partes[1].matches("[0-9]{1,18}") || !partes[2].matches("[0-9]{1,12}")) {
            return invalido;
        }
        Long id = Long.valueOf(partes[1]);
        long emitido = Long.parseLong(partes[2]);
        boolean assinaturaOk = MessageDigest.isEqual(assinatura(id, emitido).getBytes(StandardCharsets.UTF_8),
                partes[3].getBytes(StandardCharsets.UTF_8));
        if (!assinaturaOk) return invalido;

        long agora = relogio.instant().getEpochSecond();
        if (emitido > agora + TOLERANCIA_RELOGIO_SEGUNDOS) return new Leitura(Situacao.NO_FUTURO, null);
        if (agora - emitido > VALIDADE_SEGUNDOS) return new Leitura(Situacao.EXPIRADO, null);
        return new Leitura(Situacao.VALIDO, id);
    }

    /** Devolve o id do aluno se o código for válido e estiver dentro do prazo; vazio em qualquer outro caso. */
    public Optional<Long> alunoDoToken(String codigo) {
        Leitura l = ler(codigo);
        return l.valido() ? Optional.of(l.alunoId()) : Optional.empty();
    }

    /** Regras da catraca: null libera; senão, o motivo do bloqueio. */
    public static String motivoBloqueio(Aluno a, boolean matriculaAtiva, boolean mensalidadeAtrasada) {
        if ("INATIVO".equals(a.getSituacao())) return "está com cadastro INATIVO";
        if (!matriculaAtiva) return "não possui matrícula ativa";
        if (mensalidadeAtrasada) return "está com mensalidade em atraso";
        return null;
    }

    /** Igual à regra acima, mas com o trancamento: aluno com matrícula trancada é barrado com o motivo (e a data de retorno). */
    public static String motivoBloqueio(Aluno a, boolean matriculaAtiva, boolean mensalidadeAtrasada, String motivoTrancada) {
        if ("INATIVO".equals(a.getSituacao())) return "está com cadastro INATIVO";
        if (motivoTrancada != null) return motivoTrancada;
        return motivoBloqueio(a, matriculaAtiva, mensalidadeAtrasada);
    }

    /** QR em SVG (gerado no servidor, sem script). */
    public String qrSvg(String conteudo) {
        return qrSvg(conteudo, "QR code da carteirinha");
    }

    /** Mesmo QR, com outro texto de acessibilidade (ex.: Pix). */
    public String qrSvg(String conteudo, String descricao) {
        try {
            BitMatrix m = new QRCodeWriter().encode(conteudo, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 2));
            StringBuilder d = new StringBuilder();
            for (int y = 0; y < m.getHeight(); y++) {
                for (int x = 0; x < m.getWidth(); x++) {
                    if (m.get(x, y)) d.append('M').append(x).append(' ').append(y).append("h1v1h-1z");
                }
            }
            return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + m.getWidth() + " " + m.getHeight()
                    + "\" shape-rendering=\"crispEdges\" role=\"img\" aria-label=\"" + descricao + "\">"
                    + "<rect width=\"100%\" height=\"100%\" fill=\"#fff\"/><path fill=\"#000\" d=\"" + d + "\"/></svg>";
        } catch (WriterException e) {
            throw new IllegalStateException("Não foi possível gerar o QR", e);
        }
    }

    private String assinatura(Long alunoId, long emitido) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chave, "HmacSHA256"));
            byte[] h = mac.doFinal(("aluno:" + alunoId + ":" + emitido).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(h).substring(0, 22);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
