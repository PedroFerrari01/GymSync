package com.example.sistema.academia.service;

import com.example.sistema.academia.model.ExercicioTreino;
import com.example.sistema.academia.model.RegistroExercicio;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Recordes pessoais de carga. Calcula tudo na hora a partir dos registros de exercício (sem tabela nova).
 * O exercício é identificado pelo nome (sem diferenciar maiúsculas), então vale mesmo que a ficha mude de treino.
 */
@Service
public class RecordesService {

    public static final int DIAS_SELO = 7;
    public static final int LARGURA_GRAFICO = 120;
    public static final int ALTURA_GRAFICO = 44;

    public record Ponto(LocalDate data, double carga) {}

    public record Recorde(String exercicio, double carga, LocalDate data, int registros,
                          List<Ponto> pontos, boolean novo, String pontosSvg, String ultimoX, String ultimoY) {}

    private static final Comparator<RegistroExercicio> ORDEM = Comparator
            .comparing(RegistroExercicio::getDataRegistro)
            .thenComparing(r -> r.getId() == null ? 0L : r.getId());

    private static String chave(String nome) {
        return nome == null ? "" : nome.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean valido(RegistroExercicio r) {
        return r != null && r.getCarga() != null && r.getCarga() > 0 && r.getDataRegistro() != null
                && r.getExercicio() != null && r.getExercicio().getExercicio() != null;
    }

    /** Os exercícios com mais registros (até {@code limite}), cada um com recorde, evolução e selo "novo". */
    public List<Recorde> calcular(List<RegistroExercicio> registros, LocalDate hoje, int limite) {
        Map<String, List<RegistroExercicio>> porExercicio = new LinkedHashMap<>();
        if (registros != null) {
            for (RegistroExercicio r : registros) {
                if (valido(r)) porExercicio.computeIfAbsent(chave(r.getExercicio().getExercicio()), k -> new ArrayList<>()).add(r);
            }
        }
        List<Recorde> lista = new ArrayList<>();
        for (List<RegistroExercicio> grupo : porExercicio.values()) {
            grupo.sort(ORDEM);
            lista.add(montar(grupo, hoje));
        }
        lista.sort(Comparator.comparingInt(Recorde::registros).reversed()
                .thenComparing(Recorde::data, Comparator.reverseOrder())
                .thenComparing(Recorde::exercicio));
        return lista.size() > limite ? new ArrayList<>(lista.subList(0, Math.max(0, limite))) : lista;
    }

    private Recorde montar(List<RegistroExercicio> grupo, LocalDate hoje) {
        double max = 0;
        LocalDate dataMax = null;
        List<Ponto> pontos = new ArrayList<>();
        for (RegistroExercicio r : grupo) {
            pontos.add(new Ponto(r.getDataRegistro(), r.getCarga()));
            if (r.getCarga() > max) { max = r.getCarga(); dataMax = r.getDataRegistro(); }
        }
        RegistroExercicio ultimo = grupo.get(grupo.size() - 1);
        double maxAnterior = 0;
        for (int i = 0; i < grupo.size() - 1; i++) maxAnterior = Math.max(maxAnterior, grupo.get(i).getCarga());
        boolean recente = hoje != null && !ultimo.getDataRegistro().isBefore(hoje.minusDays(DIAS_SELO));
        boolean novo = grupo.size() > 1 && recente && ultimo.getCarga() > maxAnterior;
        String nome = ultimo.getExercicio().getExercicio().trim();

        // mini-gráfico: x espaçado igualmente, y proporcional (com folga nas bordas)
        StringBuilder sb = new StringBuilder();
        double min = pontos.stream().mapToDouble(Ponto::carga).min().orElse(0);
        double faixa = Math.max(max - min, 0.0001);
        String ux = "0", uy = "0";
        for (int i = 0; i < pontos.size(); i++) {
            double x = pontos.size() == 1 ? LARGURA_GRAFICO / 2.0 : 6 + (LARGURA_GRAFICO - 12) * i / (pontos.size() - 1.0);
            double y = max == min ? ALTURA_GRAFICO / 2.0 : 6 + (ALTURA_GRAFICO - 12) * (1 - (pontos.get(i).carga() - min) / faixa);
            ux = fmt(x);
            uy = fmt(y);
            if (i > 0) sb.append(' ');
            sb.append(ux).append(',').append(uy);
        }
        return new Recorde(nome, max, dataMax, grupo.size(), List.copyOf(pontos), novo, sb.toString(), ux, uy);
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    /** Maior carga já registrada, por item da ficha (id do ExercicioTreino), olhando o exercício pelo nome. */
    public Map<Long, Double> maximosDaFicha(List<ExercicioTreino> ficha, List<RegistroExercicio> registros) {
        Map<String, Double> porNome = new HashMap<>();
        if (registros != null) {
            for (RegistroExercicio r : registros) {
                if (valido(r)) porNome.merge(chave(r.getExercicio().getExercicio()), r.getCarga(), Math::max);
            }
        }
        Map<Long, Double> mapa = new HashMap<>();
        if (ficha != null) {
            for (ExercicioTreino e : ficha) {
                Double max = porNome.get(chave(e.getExercicio()));
                if (e.getId() != null && max != null) mapa.put(e.getId(), max);
            }
        }
        return mapa;
    }

    /** Carga igual ao recorde não conta; sem histórico (recorde nulo ou zero) também não. */
    public static boolean bateRecorde(Double recordeAtual, Double carga) {
        return recordeAtual != null && recordeAtual > 0 && carga != null && carga > recordeAtual;
    }
}
