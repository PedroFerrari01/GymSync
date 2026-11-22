package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Mensalidade;

/** Regras do recibo de mensalidade (número, CPF mascarado e quem pode ver). Sem banco. */
public final class ReciboService {

    private ReciboService() {}

    /** Só mensalidade paga tem recibo. */
    public static boolean temRecibo(Mensalidade m) {
        return m != null && "PAGA".equals(m.getSituacao());
    }

    /** Número derivado do ano do pagamento (ou do vencimento) e do id, sem tabela nova: GS-2026-000123. */
    public static String numero(Mensalidade m) {
        int ano = m.getDataPagamento() != null ? m.getDataPagamento().getYear()
                : m.getVencimento() != null ? m.getVencimento().getYear() : 0;
        return String.format("GS-%04d-%06d", ano, m.getId());
    }

    /** 123.456.789-09 vira ***.456.789-**; o recibo nunca mostra o CPF inteiro. */
    public static String cpfMascarado(String cpf) {
        if (cpf == null) return "-";
        String d = cpf.replaceAll("[^0-9]", "");
        if (d.length() != 11) return "-";
        return "***." + d.substring(3, 6) + "." + d.substring(6, 9) + "-**";
    }

    /** Admin vê qualquer recibo; aluno só os próprios; professor e anônimo, nenhum. */
    public static boolean podeVer(String perfil, Long usuarioId, Long donoId) {
        if ("ADMIN".equals(perfil)) return true;
        return "ALUNO".equals(perfil) && usuarioId != null && usuarioId.equals(donoId);
    }
}
