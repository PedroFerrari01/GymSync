package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Aluno;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Carteirinha digital (simulada): o QR leva um código assinado (HMAC-SHA256) com o id do aluno,
 * nunca o CPF. Sem a chave do servidor não dá para inventar um código válido.
 */
@Service
public class CarteirinhaService {

    private static final String PREFIXO = "GS1";
    private final byte[] chave;

    public CarteirinhaService(@Value("${gymsync.carteirinha.chave:}") String chaveConfig) {
        if (chaveConfig == null || chaveConfig.isBlank()) {
            // Sem chave configurada, sorteia uma: os códigos valem até o servidor reiniciar.
            byte[] b = new byte[32];
            new SecureRandom().nextBytes(b);
            this.chave = b;
        } else {
            this.chave = chaveConfig.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String token(Long alunoId) {
        return PREFIXO + "." + alunoId + "." + assinatura(alunoId);
    }

    /** Devolve o id do aluno se o código for válido; vazio se for falso, alterado ou malformado. */
    public Optional<Long> alunoDoToken(String codigo) {
        if (codigo == null) return Optional.empty();
        String[] partes = codigo.trim().split("[.]");
        if (partes.length != 3 || !PREFIXO.equals(partes[0]) || !partes[1].matches("[0-9]{1,18}")) {
            return Optional.empty();
        }
        Long id = Long.valueOf(partes[1]);
        boolean ok = MessageDigest.isEqual(assinatura(id).getBytes(StandardCharsets.UTF_8),
                partes[2].getBytes(StandardCharsets.UTF_8));
        return ok ? Optional.of(id) : Optional.empty();
    }

    /** Regras da catraca: null libera; senão, o motivo do bloqueio. */
    public static String motivoBloqueio(Aluno a, boolean matriculaAtiva, boolean mensalidadeAtrasada) {
        if ("INATIVO".equals(a.getSituacao())) return "está com cadastro INATIVO";
        if (!matriculaAtiva) return "não possui matrícula ativa";
        if (mensalidadeAtrasada) return "está com mensalidade em atraso";
        return null;
    }

    /** QR em SVG (gerado no servidor, sem script). */
    public String qrSvg(String conteudo) {
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
                    + "\" shape-rendering=\"crispEdges\" role=\"img\" aria-label=\"QR code da carteirinha\">"
                    + "<rect width=\"100%\" height=\"100%\" fill=\"#fff\"/><path fill=\"#000\" d=\"" + d + "\"/></svg>";
        } catch (WriterException e) {
            throw new IllegalStateException("Não foi possível gerar o QR", e);
        }
    }

    private String assinatura(Long alunoId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chave, "HmacSHA256"));
            byte[] h = mac.doFinal(("aluno:" + alunoId).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(h).substring(0, 22);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
