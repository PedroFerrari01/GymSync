package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Barra;
import com.example.sistema.academia.dto.Comparacao;
import com.example.sistema.academia.model.AvaliacaoFisica;
import com.example.sistema.academia.model.Entrada;
import com.example.sistema.academia.model.Matricula;
import com.example.sistema.academia.model.Mensalidade;
import com.example.sistema.academia.repository.AlunoRepository;
import com.example.sistema.academia.repository.EntradaRepository;
import com.example.sistema.academia.repository.HistoricoAcaoRepository;
import com.example.sistema.academia.repository.MatriculaRepository;
import com.example.sistema.academia.repository.MensalidadeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RelatorioService {

    private static final Locale BR = Locale.of("pt", "BR");
    private static final String[] DIAS = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};

    @Autowired private EntradaRepository entradaRepository;
    @Autowired private MensalidadeRepository mensalidadeRepository;
    @Autowired private AlunoRepository alunoRepository;
    @Autowired private MatriculaRepository matriculaRepository;
    @Autowired private HistoricoAcaoRepository historicoAcaoRepository;

    private String dinheiro(BigDecimal valor) {
        NumberFormat nf = NumberFormat.getNumberInstance(BR);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "R$ " + nf.format(valor);
    }

    private int percentual(double valor, double maximo) {
        return maximo <= 0 ? 0 : (int) Math.round(valor / maximo * 100);
    }

    private BigDecimal valorOuZero(Mensalidade m) {
        return m.getValor() != null ? m.getValor() : BigDecimal.ZERO;
    }

    public void montar(Model model) {
        LocalDate hoje = LocalDate.now();
        YearMonth mesAtual = YearMonth.now();
        // Desde o começo do mês passado: cobre os últimos 30 dias e a comparação mês a mês
        List<Entrada> entradas = entradaRepository.findByDataHoraEntradaAfter(mesAtual.minusMonths(1).atDay(1).atStartOfDay().minusSeconds(1));
        LocalDateTime limite30 = LocalDateTime.now().minusDays(30);
        List<Entrada> ultimos30 = entradas.stream().filter(e -> e.getDataHoraEntrada().isAfter(limite30)).collect(Collectors.toList());

        model.addAttribute("relEntradasHoje", entradas.stream().filter(e -> e.getDataHoraEntrada().toLocalDate().equals(hoje)).count());
        model.addAttribute("relEntradasMes", entradas.stream().filter(e -> YearMonth.from(e.getDataHoraEntrada()).equals(mesAtual)).count());
        model.addAttribute("relAlunosAtivos", alunoRepository.findAll().stream().filter(a -> "ATIVO".equals(a.getSituacao())).count());

        List<Mensalidade> mensalidades = mensalidadeRepository.findAll();
        model.addAttribute("relInadimplentes", mensalidades.stream()
                .filter(m -> "ATRASADA".equals(m.getSituacao()))
                .map(m -> m.getMatricula().getAluno().getId()).distinct().count());

        BigDecimal receitaMes = BigDecimal.ZERO;
        BigDecimal aReceber = BigDecimal.ZERO;
        BigDecimal atrasado = BigDecimal.ZERO;
        for (Mensalidade m : mensalidades) {
            if ("PAGA".equals(m.getSituacao()) && m.getDataPagamento() != null && YearMonth.from(m.getDataPagamento()).equals(mesAtual)) {
                receitaMes = receitaMes.add(valorOuZero(m));
            } else if ("PENDENTE".equals(m.getSituacao()) || "EM_ANALISE".equals(m.getSituacao())) {
                aReceber = aReceber.add(valorOuZero(m));
            } else if ("ATRASADA".equals(m.getSituacao())) {
                atrasado = atrasado.add(valorOuZero(m));
            }
        }
        model.addAttribute("relReceitaMes", dinheiro(receitaMes));
        model.addAttribute("relAReceber", dinheiro(aReceber));
        model.addAttribute("relAtrasado", dinheiro(atrasado));

        long[] porHora = new long[24];
        long[] porDia = new long[7];
        for (Entrada e : ultimos30) {
            porHora[e.getDataHoraEntrada().getHour()]++;
            porDia[e.getDataHoraEntrada().getDayOfWeek().getValue() - 1]++;
        }

        long maxHora = Arrays.stream(porHora).max().orElse(0);
        List<Barra> horas = new ArrayList<>();
        int horaPico = -1;
        for (int h = 5; h <= 23; h++) {
            horas.add(new Barra(h + "h", String.valueOf(porHora[h]), percentual(porHora[h], maxHora)));
            if (porHora[h] > 0 && (horaPico < 0 || porHora[h] > porHora[horaPico])) horaPico = h;
        }
        model.addAttribute("relHoras", horas);
        model.addAttribute("relHoraPico", horaPico < 0 ? null : horaPico + "h às " + (horaPico + 1) + "h (" + porHora[horaPico] + " entradas)");

        long maxDia = Arrays.stream(porDia).max().orElse(0);
        List<Barra> dias = new ArrayList<>();
        int diaPico = -1;
        for (int d = 0; d < 7; d++) {
            dias.add(new Barra(DIAS[d], String.valueOf(porDia[d]), percentual(porDia[d], maxDia)));
            if (porDia[d] > 0 && (diaPico < 0 || porDia[d] > porDia[diaPico])) diaPico = d;
        }
        model.addAttribute("relDias", dias);
        model.addAttribute("relDiaPico", diaPico < 0 ? null : DIAS[diaPico] + " (" + porDia[diaPico] + " entradas)");

        Map<String, Long> porAluno = ultimos30.stream()
                .collect(Collectors.groupingBy(e -> e.getAluno().getNome(), Collectors.counting()));
        List<Map.Entry<String, Long>> ranking = new ArrayList<>(porAluno.entrySet());
        ranking.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        long maxAluno = ranking.isEmpty() ? 0 : ranking.get(0).getValue();
        List<Barra> top = new ArrayList<>();
        for (int i = 0; i < Math.min(5, ranking.size()); i++) {
            Map.Entry<String, Long> item = ranking.get(i);
            top.add(new Barra(item.getKey(), item.getValue() + " entradas", percentual(item.getValue(), maxAluno)));
        }
        model.addAttribute("relTopAlunos", top);

        List<String> rotulos = new ArrayList<>();
        List<BigDecimal> valores = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = mesAtual.minusMonths(i);
            BigDecimal soma = BigDecimal.ZERO;
            for (Mensalidade m : mensalidades) {
                if ("PAGA".equals(m.getSituacao()) && m.getDataPagamento() != null && YearMonth.from(m.getDataPagamento()).equals(ym)) {
                    soma = soma.add(valorOuZero(m));
                }
            }
            rotulos.add(String.format("%02d/%02d", ym.getMonthValue(), ym.getYear() % 100));
            valores.add(soma);
        }
        double maxReceita = valores.stream().mapToDouble(BigDecimal::doubleValue).max().orElse(0);
        List<Barra> receita = new ArrayList<>();
        for (int i = 0; i < rotulos.size(); i++) {
            receita.add(new Barra(rotulos.get(i), dinheiro(valores.get(i)), percentual(valores.get(i).doubleValue(), maxReceita)));
        }
        model.addAttribute("relReceita", receita);

        model.addAttribute("relMesAMes", mesAMes(hoje, entradas, matriculaRepository.findAll(), mensalidades));
        model.addAttribute("relMesAMesTitulo", "Dia 1 a " + hoje.getDayOfMonth() + " de cada mês");
        model.addAttribute("historico", historicoAcaoRepository.findTop50ByOrderByDataHoraDescIdDesc());
    }

    /** Números de um mês, do dia 1 até {@code ateDia} (para comparar períodos iguais). */
    public record ResumoMes(long entradas, long alunosQueTreinaram, long novasMatriculas, BigDecimal recebido) {}

    public ResumoMes resumoMes(YearMonth mes, int ateDia, List<Entrada> entradas, List<Matricula> matriculas, List<Mensalidade> mensalidades) {
        LocalDate inicio = mes.atDay(1);
        LocalDate fim = mes.atDay(Math.min(ateDia, mes.lengthOfMonth()));
        List<Entrada> doPeriodo = entradas.stream()
                .filter(e -> e.getDataHoraEntrada() != null && dentro(e.getDataHoraEntrada().toLocalDate(), inicio, fim))
                .toList();
        long alunos = doPeriodo.stream().filter(e -> e.getAluno() != null).map(e -> e.getAluno().getId()).distinct().count();
        long novas = matriculas.stream().filter(m -> m.getDataInicio() != null && dentro(m.getDataInicio(), inicio, fim)).count();
        BigDecimal recebido = mensalidades.stream()
                .filter(m -> "PAGA".equals(m.getSituacao()) && m.getDataPagamento() != null && dentro(m.getDataPagamento(), inicio, fim))
                .map(this::valorOuZero).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResumoMes(doPeriodo.size(), alunos, novas, recebido);
    }

    private boolean dentro(LocalDate dia, LocalDate inicio, LocalDate fim) {
        return !dia.isBefore(inicio) && !dia.isAfter(fim);
    }

    /** Este mês (até hoje) x o mesmo período do mês passado. Primeira coluna = mês passado, última = este mês. */
    public List<Comparacao> mesAMes(LocalDate hoje, List<Entrada> entradas, List<Matricula> matriculas, List<Mensalidade> mensalidades) {
        YearMonth atual = YearMonth.from(hoje);
        ResumoMes agora = resumoMes(atual, hoje.getDayOfMonth(), entradas, matriculas, mensalidades);
        ResumoMes antes = resumoMes(atual.minusMonths(1), hoje.getDayOfMonth(), entradas, matriculas, mensalidades);
        return List.of(
                linhaMes("Entradas na catraca", antes.entradas(), agora.entradas(), v -> Long.toString(v)),
                linhaMes("Alunos que treinaram", antes.alunosQueTreinaram(), agora.alunosQueTreinaram(), v -> Long.toString(v)),
                linhaMes("Novas matrículas", antes.novasMatriculas(), agora.novasMatriculas(), v -> Long.toString(v)),
                linhaMes("Recebido", antes.recebido().doubleValue(), agora.recebido().doubleValue(), v -> dinheiro(BigDecimal.valueOf(v))));
    }

    private <T extends Number> Comparacao linhaMes(String rotulo, T antes, T agora, java.util.function.Function<T, String> formato) {
        double a = antes.doubleValue();
        double b = agora.doubleValue();
        String variacao;
        String classe;
        if (a == 0 && b == 0) {
            variacao = "igual";
            classe = "neutro";
        } else if (a == 0) {
            variacao = "novo";
            classe = "melhorou";
        } else {
            long pct = Math.round((b - a) / a * 100);
            variacao = (pct > 0 ? "+" : "") + pct + "%";
            classe = pct > 0 ? "melhorou" : pct < 0 ? "piorou" : "neutro";
        }
        return new Comparacao(rotulo, formato.apply(antes), formato.apply(agora), variacao, classe);
    }

    public List<Barra> barrasPeso(List<AvaliacaoFisica> avaliacoes) {
        List<AvaliacaoFisica> comPeso = avaliacoes.stream().filter(a -> a.getPeso() != null).collect(Collectors.toList());
        double max = comPeso.stream().mapToDouble(AvaliacaoFisica::getPeso).max().orElse(0);
        NumberFormat nf = NumberFormat.getNumberInstance(BR);
        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(1);
        List<Barra> barras = new ArrayList<>();
        for (AvaliacaoFisica a : comPeso) {
            String rotulo = a.getDataAvaliacao() != null
                    ? String.format("%02d/%02d", a.getDataAvaliacao().getDayOfMonth(), a.getDataAvaliacao().getMonthValue()) : "-";
            barras.add(new Barra(rotulo, nf.format(a.getPeso()), percentual(a.getPeso(), max)));
        }
        return barras;
    }

    public String resumoPeso(List<AvaliacaoFisica> avaliacoes) {
        List<AvaliacaoFisica> comPeso = avaliacoes.stream().filter(a -> a.getPeso() != null).collect(Collectors.toList());
        if (comPeso.size() < 2) return null;
        double diferenca = comPeso.get(comPeso.size() - 1).getPeso() - comPeso.get(0).getPeso();
        NumberFormat nf = NumberFormat.getNumberInstance(BR);
        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(1);
        String sinal = diferenca > 0 ? "+" : "";
        return sinal + nf.format(diferenca) + " kg desde a primeira avaliação";
    }
}
