package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Barra;
import com.example.sistema.academia.dto.Comparacao;
import com.example.sistema.academia.model.AvaliacaoFisica;
import org.springframework.stereotype.Service;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.Function;

/** Evolução do aluno entre as avaliações físicas (lista em ordem de data, da mais antiga para a mais nova). */
@Service
public class EvolucaoService {

    private static final Locale BR = Locale.of("pt", "BR");

    private record Medida(String rotulo, String unidade, Function<AvaliacaoFisica, Double> valor, boolean menorEhMelhor) {}

    // Peso, peito, braço e coxa dependem do objetivo do aluno: ficam sem cor de "melhorou" ou "piorou"
    private static final List<Medida> MEDIDAS = List.of(
            new Medida("Peso", " kg", AvaliacaoFisica::getPeso, false),
            new Medida("Gordura", "%", AvaliacaoFisica::getPercentualGordura, true),
            new Medida("Cintura", " cm", AvaliacaoFisica::getCintura, true),
            new Medida("Quadril", " cm", AvaliacaoFisica::getQuadril, true),
            new Medida("Peito", " cm", AvaliacaoFisica::getPeito, false),
            new Medida("Braço", " cm", AvaliacaoFisica::getBraco, false),
            new Medida("Coxa", " cm", AvaliacaoFisica::getCoxa, false));

    private static final Set<String> COM_COR = Set.of("Gordura", "Cintura", "Quadril");

    private String numero(double v) {
        NumberFormat nf = NumberFormat.getNumberInstance(BR);
        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(1);
        return nf.format(v);
    }

    /** Primeira x última medida de cada item. Só entra o que foi medido em pelo menos duas avaliações. */
    public List<Comparacao> comparacao(List<AvaliacaoFisica> avaliacoes) {
        List<Comparacao> linhas = new ArrayList<>();
        for (Medida m : MEDIDAS) {
            List<Double> valores = avaliacoes.stream().map(m.valor()).filter(Objects::nonNull).toList();
            if (valores.size() < 2) continue;
            double primeira = valores.get(0);
            double ultima = valores.get(valores.size() - 1);
            double diferenca = Math.round((ultima - primeira) * 10) / 10.0;

            String classe = "neutro";
            if (COM_COR.contains(m.rotulo()) && diferenca != 0) {
                classe = (diferenca < 0) == m.menorEhMelhor() ? "melhorou" : "piorou";
            }
            String sinal = diferenca > 0 ? "+" : "";
            linhas.add(new Comparacao(m.rotulo(), numero(primeira) + m.unidade(), numero(ultima) + m.unidade(),
                    sinal + numero(diferenca) + m.unidade(), classe));
        }
        return linhas;
    }

    /** Barras do percentual de gordura em cada avaliação (mesmo formato do gráfico de peso). */
    public List<Barra> barrasGordura(List<AvaliacaoFisica> avaliacoes) {
        List<AvaliacaoFisica> comGordura = avaliacoes.stream().filter(a -> a.getPercentualGordura() != null).toList();
        double max = comGordura.stream().mapToDouble(AvaliacaoFisica::getPercentualGordura).max().orElse(0);
        List<Barra> barras = new ArrayList<>();
        for (AvaliacaoFisica a : comGordura) {
            String rotulo = a.getDataAvaliacao() != null
                    ? String.format("%02d/%02d", a.getDataAvaliacao().getDayOfMonth(), a.getDataAvaliacao().getMonthValue()) : "-";
            int percentual = max <= 0 ? 0 : (int) Math.round(a.getPercentualGordura() / max * 100);
            barras.add(new Barra(rotulo, numero(a.getPercentualGordura()) + "%", percentual));
        }
        return barras;
    }
}
