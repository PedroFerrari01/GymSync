package com.example.sistema.academia.view;

import com.example.sistema.academia.model.RegistroExercicio;
import org.springframework.stereotype.Component;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component("fmt")
public class FormatadorView {

    private static final Locale BR = Locale.of("pt", "BR");

    public String dinheiro(Number valor) {
        if (valor == null) return "-";
        NumberFormat nf = NumberFormat.getNumberInstance(BR);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(valor);
    }

    public String numero(Number valor, int casas) {
        if (valor == null) return "-";
        NumberFormat nf = NumberFormat.getNumberInstance(BR);
        nf.setMinimumFractionDigits(casas);
        nf.setMaximumFractionDigits(casas);
        return nf.format(valor);
    }

    public String statusClasse(String situacao) {
        if (situacao == null) return "status-off";
        switch (situacao) {
            case "PAGA": return "status-ok";
            case "ATRASADA": return "status-err";
            case "EM_ANALISE": return "status-info";
            case "CANCELADA": return "status-off";
            default: return "status-wait";
        }
    }

    public String statusTexto(String situacao) {
        if (situacao == null) return "";
        return "EM_ANALISE".equals(situacao) ? "EM ANÁLISE" : situacao;
    }

    public String data(LocalDate data) {
        return data == null ? "" : data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String execucao(RegistroExercicio r) {
        if (r == null || r.getDataRegistro() == null) return "-";
        String data = r.getDataRegistro().format(DateTimeFormatter.ofPattern("dd/MM"));
        return r.getCarga() != null ? data + " - " + numero(r.getCarga(), 1) + " kg" : data + " - sem carga";
    }
}
