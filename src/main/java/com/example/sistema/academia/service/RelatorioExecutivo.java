package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.Matricula;
import com.example.sistema.academia.model.Mensalidade;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Contas do relatório impresso (resumo executivo, 12 meses, planos) e gráficos em SVG. Sem banco: só listas. */
public final class RelatorioExecutivo {

    private RelatorioExecutivo() {}

    public record Resumo(long alunosAtivos, long alunosInativos, long matriculasNovasMes, long matriculasCanceladas,
                         BigDecimal receitaMes, BigDecimal receitaMesAnterior, Double variacaoPct,
                         BigDecimal emAtraso, long alunosInadimplentes, Double inadimplenciaPct,
                         long pagamentosMes, BigDecimal ticketMedio) {}

    public record ItemPlano(String plano, long matriculas, int pct) {}

    public static Resumo resumo(YearMonth mes, List<Aluno> alunos, List<Matricula> matriculas, List<Mensalidade> mensalidades) {
        long ativos = alunos.stream().filter(a -> "ATIVO".equals(a.getSituacao())).count();
        long inativos = alunos.size() - ativos;
        long novas = matriculas.stream()
                .filter(m -> m.getDataInicio() != null && YearMonth.from(m.getDataInicio()).equals(mes)).count();
        long canceladas = matriculas.stream().filter(m -> "CANCELADA".equals(m.getSituacao())).count();

        BigDecimal receita = somaPagas(mensalidades, mes);
        BigDecimal anterior = somaPagas(mensalidades, mes.minusMonths(1));
        Double variacao = anterior.signum() > 0 ? receita.subtract(anterior).doubleValue() / anterior.doubleValue() * 100 : null;
        long pagamentos = mensalidades.stream().filter(m -> pagaEm(m, mes)).count();
        BigDecimal ticket = pagamentos == 0 ? BigDecimal.ZERO
                : receita.divide(BigDecimal.valueOf(pagamentos), 2, RoundingMode.HALF_UP);

        BigDecimal atraso = BigDecimal.ZERO;
        List<Long> inadimplentes = new ArrayList<>();
        for (Mensalidade m : mensalidades) {
            if ("ATRASADA".equals(m.getSituacao())) {
                atraso = atraso.add(m.getValor() == null ? BigDecimal.ZERO : m.getValor());
                Long id = m.getMatricula().getAluno().getId();
                if (!inadimplentes.contains(id)) inadimplentes.add(id);
            }
        }
        Double inadPct = ativos == 0 ? null : inadimplentes.size() * 100.0 / ativos;
        return new Resumo(ativos, inativos, novas, canceladas, receita, anterior, variacao, atraso,
                inadimplentes.size(), inadPct, pagamentos, ticket);
    }

    /** Recebido em cada um dos {@code qtd} meses terminando em {@code ultimo}, do mais antigo ao mais novo. */
    public static List<BigDecimal> receitaPorMes(YearMonth ultimo, int qtd, List<Mensalidade> mensalidades) {
        List<BigDecimal> valores = new ArrayList<>();
        for (int i = qtd - 1; i >= 0; i--) valores.add(somaPagas(mensalidades, ultimo.minusMonths(i)));
        return valores;
    }

    public static List<String> rotulosMeses(YearMonth ultimo, int qtd) {
        List<String> r = new ArrayList<>();
        for (int i = qtd - 1; i >= 0; i--) {
            YearMonth ym = ultimo.minusMonths(i);
            r.add(String.format("%02d/%02d", ym.getMonthValue(), ym.getYear() % 100));
        }
        return r;
    }

    /** Matrículas ativas por plano, do mais contratado ao menos (barra proporcional ao maior). */
    public static List<ItemPlano> planosMaisContratados(List<Matricula> matriculas, int limite) {
        Map<String, Long> conta = new LinkedHashMap<>();
        for (Matricula m : matriculas) {
            if ("ATIVA".equals(m.getSituacao()) && m.getPlano() != null) conta.merge(m.getPlano().getNome(), 1L, Long::sum);
        }
        List<Map.Entry<String, Long>> ordem = new ArrayList<>(conta.entrySet());
        ordem.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        long max = ordem.isEmpty() ? 0 : ordem.get(0).getValue();
        List<ItemPlano> lista = new ArrayList<>();
        for (int i = 0; i < Math.min(limite, ordem.size()); i++) {
            long q = ordem.get(i).getValue();
            lista.add(new ItemPlano(ordem.get(i).getKey(), q, max == 0 ? 0 : (int) Math.round(q * 100.0 / max)));
        }
        return lista;
    }

    private static boolean pagaEm(Mensalidade m, YearMonth mes) {
        return "PAGA".equals(m.getSituacao()) && m.getDataPagamento() != null
                && YearMonth.from(m.getDataPagamento()).equals(mes);
    }

    private static BigDecimal somaPagas(List<Mensalidade> mensalidades, YearMonth mes) {
        BigDecimal soma = BigDecimal.ZERO;
        for (Mensalidade m : mensalidades) {
            if (pagaEm(m, mes)) soma = soma.add(m.getValor() == null ? BigDecimal.ZERO : m.getValor());
        }
        return soma;
    }

    /**
     * Gráfico de barras em SVG com cores fixas (imprime bem em papel branco). Os rótulos passam por escape.
     */
    public static String graficoBarras(List<String> rotulos, List<? extends Number> valores, int largura, int altura) {
        int n = rotulos.size();
        double max = valores.stream().mapToDouble(Number::doubleValue).max().orElse(0);
        int margemTopo = 16;
        int margemBase = 20;
        double area = altura - margemTopo - margemBase;
        double passo = (double) largura / Math.max(n, 1);
        double larg = passo * 0.62;
        StringBuilder s = new StringBuilder();
        s.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ").append(largura).append(' ').append(altura)
                .append("\" class=\"grafico-pdf\" role=\"img\" preserveAspectRatio=\"xMidYMid meet\">");
        s.append("<line x1=\"0\" y1=\"").append(altura - margemBase).append("\" x2=\"").append(largura)
                .append("\" y2=\"").append(altura - margemBase).append("\" stroke=\"#a1a1aa\" stroke-width=\"1\"/>");
        for (int i = 0; i < n; i++) {
            double v = valores.get(i).doubleValue();
            double h = max <= 0 ? 0 : v / max * area;
            double x = i * passo + (passo - larg) / 2;
            double y = altura - margemBase - h;
            s.append(String.format(Locale.US, "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" rx=\"2\" fill=\"#f97316\"/>", x, y, larg, h));
            if (v > 0) {
                s.append(String.format(Locale.US, "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-size=\"9\" fill=\"#3f3f46\">%s</text>",
                        x + larg / 2, y - 3, escapar(formatar(v))));
            }
            s.append(String.format(Locale.US, "<text x=\"%.1f\" y=\"%d\" text-anchor=\"middle\" font-size=\"9\" fill=\"#52525b\">%s</text>",
                    x + larg / 2, altura - 6, escapar(rotulos.get(i))));
        }
        return s.append("</svg>").toString();
    }

    private static String formatar(double v) {
        Locale br = Locale.of("pt", "BR");
        if (v >= 1000) return String.format(br, "%,.0f", v);
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(br, "%.1f", v);
    }

    private static String escapar(String t) {
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
