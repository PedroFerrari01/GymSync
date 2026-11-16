package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Barra;
import com.example.sistema.academia.dto.Comparacao;
import com.example.sistema.academia.dto.MapaCorpo;
import com.example.sistema.academia.dto.MedidaCorpo;
import com.example.sistema.academia.model.AvaliacaoFisica;
import org.springframework.stereotype.Service;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

/** Evolução do aluno entre as avaliações físicas (lista em ordem de data, da mais antiga para a mais nova). */
@Service
public class EvolucaoService {

    private static final Locale BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Sentido bom de cada medida: diminuir, aumentar ou nenhum (sem cor). */
    enum Direcao { MENOR_EH_MELHOR, MAIOR_EH_MELHOR, NEUTRA }

    private record Medida(String chave, String rotulo, String unidade, Function<AvaliacaoFisica, Double> valor, Direcao direcao) {}

    // Gordura, cintura e quadril: diminuir é bom. Peito, braço e coxa: aumentar é bom. Peso e IMC: sem cor
    private static final List<Medida> MEDIDAS = List.of(
            new Medida("peso", "Peso", " kg", AvaliacaoFisica::getPeso, Direcao.NEUTRA),
            new Medida("gordura", "Gordura", "%", AvaliacaoFisica::getPercentualGordura, Direcao.MENOR_EH_MELHOR),
            new Medida("cintura", "Cintura", " cm", AvaliacaoFisica::getCintura, Direcao.MENOR_EH_MELHOR),
            new Medida("quadril", "Quadril", " cm", AvaliacaoFisica::getQuadril, Direcao.MENOR_EH_MELHOR),
            new Medida("peito", "Peito", " cm", AvaliacaoFisica::getPeito, Direcao.MAIOR_EH_MELHOR),
            new Medida("braco", "Braço", " cm", AvaliacaoFisica::getBraco, Direcao.MAIOR_EH_MELHOR),
            new Medida("coxa", "Coxa", " cm", AvaliacaoFisica::getCoxa, Direcao.MAIOR_EH_MELHOR));

    // Só no mapa do corpo (a tabela "primeira x última" continua com as sete medidas acima)
    private static final Medida IMC = new Medida("imc", "IMC", "", AvaliacaoFisica::getImc, Direcao.NEUTRA);

    private String numero(double v) {
        NumberFormat nf = NumberFormat.getNumberInstance(BR);
        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(1);
        return nf.format(v);
    }

    private static double diferenca(double primeira, double ultima) {
        return Math.round((ultima - primeira) * 10) / 10.0;
    }

    /** Regra das cores: "melhorou" quando a medida andou no sentido bom dela, "piorou" no contrário. */
    static String classe(Direcao direcao, double diferenca) {
        if (direcao == Direcao.NEUTRA || diferenca == 0) return "neutro";
        boolean diminuiu = diferenca < 0;
        return diminuiu == (direcao == Direcao.MENOR_EH_MELHOR) ? "melhorou" : "piorou";
    }

    private static String classe(Medida m, double diferenca) {
        return classe(m.direcao(), diferenca);
    }

    private String comSinal(double diferenca, String unidade) {
        return (diferenca > 0 ? "+" : "") + numero(diferenca) + unidade;
    }

    /** Primeira x última medida de cada item. Só entra o que foi medido em pelo menos duas avaliações. */
    public List<Comparacao> comparacao(List<AvaliacaoFisica> avaliacoes) {
        List<Comparacao> linhas = new ArrayList<>();
        for (Medida m : MEDIDAS) {
            List<Double> valores = avaliacoes.stream().map(m.valor()).filter(Objects::nonNull).toList();
            if (valores.size() < 2) continue;
            double primeira = valores.get(0);
            double ultima = valores.get(valores.size() - 1);
            double diferenca = diferenca(primeira, ultima);
            linhas.add(new Comparacao(m.rotulo(), numero(primeira) + m.unidade(), numero(ultima) + m.unidade(),
                    comSinal(diferenca, m.unidade()), classe(m, diferenca)));
        }
        return linhas;
    }

    /**
     * Mapa do corpo de um aluno (avaliações em ordem de data). Entra toda medida feita pelo menos uma vez;
     * a diferença e a cor só existem a partir da segunda medição, com a mesma regra da comparação.
     */
    public MapaCorpo mapaCorpo(Long alunoId, String nome, List<AvaliacaoFisica> avaliacoes) {
        List<Medida> todas = new ArrayList<>(MEDIDAS);
        todas.add(IMC);
        List<MedidaCorpo> medidas = new ArrayList<>();
        for (Medida m : todas) {
            List<Double> valores = avaliacoes.stream().map(m.valor()).filter(Objects::nonNull).toList();
            if (valores.isEmpty()) continue;
            double primeira = valores.get(0);
            double ultima = valores.get(valores.size() - 1);
            String texto = null;
            String classe = "neutro";
            if (valores.size() >= 2) {
                double diferenca = diferenca(primeira, ultima);
                texto = comSinal(diferenca, m.unidade());
                classe = classe(m, diferenca);
            }
            medidas.add(new MedidaCorpo(m.chave(), m.rotulo(), numero(primeira) + m.unidade(), numero(ultima) + m.unidade(),
                    primeira, ultima, texto, classe));
        }
        List<LocalDate> datas = avaliacoes.stream().map(AvaliacaoFisica::getDataAvaliacao).filter(Objects::nonNull).toList();
        Double alturaCm = avaliacoes.stream().map(AvaliacaoFisica::getAltura).filter(a -> a != null && a > 0)
                .reduce((antiga, nova) -> nova).map(a -> Math.round(a * 1000) / 10.0).orElse(null);
        return new MapaCorpo(alunoId, nome, avaliacoes.size(),
                datas.isEmpty() ? null : datas.get(0).format(DATA),
                datas.isEmpty() ? null : datas.get(datas.size() - 1).format(DATA),
                alturaCm, medidas);
    }

    /** Um mapa por aluno que tem avaliação (para o administrador e o professor), em ordem de nome. */
    public List<MapaCorpo> mapasCorpo(List<AvaliacaoFisica> todas) {
        Map<Long, List<AvaliacaoFisica>> porAluno = new LinkedHashMap<>();
        for (AvaliacaoFisica a : todas) {
            if (a.getAluno() == null || a.getAluno().getId() == null) continue;
            porAluno.computeIfAbsent(a.getAluno().getId(), id -> new ArrayList<>()).add(a);
        }
        List<MapaCorpo> mapas = new ArrayList<>();
        for (List<AvaliacaoFisica> doAluno : porAluno.values()) {
            doAluno.sort(Comparator.comparing(AvaliacaoFisica::getDataAvaliacao, Comparator.nullsFirst(Comparator.<LocalDate>naturalOrder()))
                    .thenComparing(AvaliacaoFisica::getId, Comparator.nullsFirst(Comparator.<Long>naturalOrder())));
            mapas.add(mapaCorpo(doAluno.get(0).getAluno().getId(), doAluno.get(0).getAluno().getNome(), doAluno));
        }
        mapas.sort(Comparator.comparing(m -> m.getNome() == null ? "" : m.getNome(), String.CASE_INSENSITIVE_ORDER));
        return mapas;
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
