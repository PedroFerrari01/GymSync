package com.example.sistema.academia.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;

/**
 * Pix SIMULADO: monta o "copia e cola" no padrão BR Code (EMV, Pix estático) com uma chave de
 * demonstração. Nenhum banco é consultado e nenhum valor é cobrado. Classe pura, sem estado.
 */
@Service
public class PixService {

    /** Chave de demonstração (não é de ninguém). */
    public static final String CHAVE_DEMO = "demonstracao@gymsync.example";
    public static final String RECEBEDOR = "GYMSYNC ACADEMIA";
    public static final String CIDADE = "SAO PAULO";

    /** Código Pix (copia e cola) de uma mensalidade. */
    public String payload(Long mensalidadeId, BigDecimal valor) {
        String conta = campo("00", "br.gov.bcb.pix") + campo("01", CHAVE_DEMO);
        String semCrc = campo("00", "01")
                + campo("01", "11")
                + campo("26", conta)
                + campo("52", "0000")
                + campo("53", "986")
                + campo("54", valor.setScale(2, RoundingMode.HALF_UP).toPlainString())
                + campo("58", "BR")
                + campo("59", RECEBEDOR)
                + campo("60", CIDADE)
                + campo("62", campo("05", "GS" + mensalidadeId))
                + "6304";
        return semCrc + crc16(semCrc);
    }

    /** Campo EMV: id (2) + tamanho (2 dígitos) + valor. */
    static String campo(String id, String valor) {
        return id + String.format("%02d", valor.length()) + valor;
    }

    /** CRC16-CCITT/FALSE (poly 0x1021, init 0xFFFF), 4 hex maiúsculos. */
    static String crc16(String texto) {
        int crc = 0xFFFF;
        for (byte b : texto.getBytes(StandardCharsets.US_ASCII)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
}
