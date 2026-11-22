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
    @Autowired private com.example.sistema.academia.repository.AulaColetivaRepository aulaColetivaRepository;
    @Autowired private com.example.sistema.academia.repository.ReservaAulaRepository reservaAulaRepository;
    @Autowired private com.example.sistema.academia.repository.EquipamentoRepository equipamentoRepository;
    @Autowired private AulaService aulaService;

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
        model.addAttribute("relSituacoes", porSituacao(mensalidades));
        model.addAttribute("relSituacoesDetalhe", detalheSituacoes(mensalidades));
        FaturamentoDetalhe fat = detalheFaturamento(rotulos, valores);
        model.addAttribute("relFat", fat);
        model.addAttribute("relDiasDetalhe", detalheDias(porDia));

        model.addAttribute("relMesAMes", mesAMes(hoje, entradas, matriculaRepository.findAll(), mensalidades));
        model.addAttribute("relMesAMesTitulo", "Dia 1 a " + hoje.getDayOfMonth() + " de cada mês");
        montarRelatorioImpresso(model, mesAtual, mensalidades, porDia, horas);
        model.addAttribute("historico", historicoAcaoRepository.findTop50ByOrderByDataHoraDescIdDesc());
        model.addAttribute("simBase", baseSimulador(matriculaRepository.findAll(), mensalidades));
    }

    /** SIMULADOR: números de partida (só leitura). Receita = soma do valor do plano das matrículas ativas. */
    public record BaseSimulador(int ativos, BigDecimal ticket, BigDecimal receita, double inadimplenciaPct) {}

    public BaseSimulador baseSimulador(List<Matricula> matriculas, List<Mensalidade> mensalidades) {
        List<Matricula> ativas = matriculas.stream()
                .filter(m -> "ATIVA".equals(m.getSituacao()) && m.getPlano() != null && m.getPlano().getValor() != null)
                .toList();
        BigDecimal receita = ativas.stream().map(m -> m.getPlano().getValor()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ticket = ativas.isEmpty() ? BigDecimal.ZERO
                : receita.divide(BigDecimal.valueOf(ativas.size()), 2, java.math.RoundingMode.HALF_UP);
        long inadimplentes = mensalidades.stream()
                .filter(m -> "ATRASADA".equals(m.getSituacao()) && m.getMatricula() != null && m.getMatricula().getAluno() != null)
                .map(m -> m.getMatricula().getAluno().getId()).distinct().count();
        double pct = ativas.isEmpty() ? 0 : Math.min(100.0, Math.round(inadimplentes * 1000.0 / ativas.size()) / 10.0);
        return new BaseSimulador(ativas.size(), ticket, receita, pct);
    }

    public record OcupacaoAula(String nome, String quando, int reservas, int vagas, int pct) {}

    /** Dados do relatório impresso (PDF): resumo executivo, 12 meses, planos, ocupação das aulas e manutenção. */
    private void montarRelatorioImpresso(Model model, YearMonth mes, List<Mensalidade> mensalidades, long[] porDia, List<Barra> horas) {
        List<Matricula> matriculas = matriculaRepository.findAll();
        model.addAttribute("pdfResumo", RelatorioExecutivo.resumo(mes, alunoRepository.findAll(), matriculas, mensalidades));

        List<String> rot12 = RelatorioExecutivo.rotulosMeses(mes, 12);
        List<BigDecimal> val12 = RelatorioExecutivo.receitaPorMes(mes, 12, mensalidades);
        model.addAttribute("pdfReceita12", detalheFaturamento(rot12, val12));
        model.addAttribute("pdfGraficoReceita", RelatorioExecutivo.graficoBarras(rot12, val12, 640, 150));

        List<Long> valDias = Arrays.stream(porDia).boxed().toList();
        model.addAttribute("pdfGraficoDias", RelatorioExecutivo.graficoBarras(Arrays.asList(DIAS), valDias, 300, 130));
        List<String> rotHoras = horas.stream().map(Barra::getRotulo).toList();
        List<Integer> valHoras = horas.stream().map(b -> Integer.parseInt(b.getTexto())).toList();
        model.addAttribute("pdfGraficoHoras", RelatorioExecutivo.graficoBarras(rotHoras, valHoras, 340, 130));

        model.addAttribute("pdfPlanos", RelatorioExecutivo.planosMaisContratados(matriculas, 5));

        List<OcupacaoAula> aulas = new ArrayList<>();
        for (com.example.sistema.academia.model.AulaColetiva a : aulaColetivaRepository.findAll()) {
            LocalDate data = aulaService.proximaData(a);
            int reservas = reservaAulaRepository.findByAulaIdAndDataAula(a.getId(), data).size();
            int vagas = a.getVagas() == null ? 0 : a.getVagas();
            aulas.add(new OcupacaoAula(a.getNome(), DIAS[a.getDiaSemana().getValue() - 1] + " " + a.getHorario(),
                    reservas, vagas, vagas == 0 ? 0 : (int) Math.round(reservas * 100.0 / vagas)));
        }
        model.addAttribute("pdfAulas", aulas);
        model.addAttribute("pdfManutencao", equipamentoRepository.findAll().stream()
                .filter(e -> "EM_MANUTENCAO".equals(e.getSituacao())).map(e -> e.getNome()).toList());
        model.addAttribute("pdfEquipamentos", equipamentoRepository.count());
    }

    private static final String[] SITUACOES = {"PAGA", "PENDENTE", "EM_ANALISE", "ATRASADA", "VENCIDA"};

    /** Quantidade de mensalidades em cada situação (barra proporcional à maior). */
    public List<Barra> porSituacao(List<Mensalidade> mensalidades) {
        long[] total = new long[SITUACOES.length];
        for (Mensalidade m : mensalidades) {
            for (int i = 0; i < SITUACOES.length; i++) {
                if (SITUACOES[i].equals(m.getSituacao())) total[i]++;
            }
        }
        long max = Arrays.stream(total).max().orElse(0);
        List<Barra> lista = new ArrayList<>();
        for (int i = 0; i < SITUACOES.length; i++) {
            lista.add(new Barra(SITUACOES[i], String.valueOf(total[i]), percentual(total[i], max)));
        }
        return lista;
    }

    public record MesFaturamento(String rotulo, BigDecimal valor, BigDecimal variacaoValor, Double variacaoPct, boolean melhor) {}
    public record FaturamentoDetalhe(List<MesFaturamento> meses, BigDecimal total, BigDecimal media, String melhorMes) {}
    public record DiaDetalhe(String rotulo, long entradas, int pct, boolean pico, boolean vazio) {}
    public record SituacaoDetalhe(String situacao, long quantidade, BigDecimal valor, int pct) {}

    /** Mês a mês: variação em relação ao anterior (nula quando não há base), melhor mês, total e média. */
    public FaturamentoDetalhe detalheFaturamento(List<String> rotulos, List<BigDecimal> valores) {
        BigDecimal total = BigDecimal.ZERO;
        int idxMelhor = -1;
        for (int i = 0; i < valores.size(); i++) {
            total = total.add(valores.get(i));
            if (valores.get(i).signum() > 0 && (idxMelhor < 0 || valores.get(i).compareTo(valores.get(idxMelhor)) > 0)) idxMelhor = i;
        }
        List<MesFaturamento> meses = new ArrayList<>();
        for (int i = 0; i < valores.size(); i++) {
            BigDecimal v = valores.get(i);
            BigDecimal difValor = null;
            Double difPct = null;
            if (i > 0) {
                BigDecimal ant = valores.get(i - 1);
                difValor = v.subtract(ant);
                if (ant.signum() > 0) difPct = difValor.doubleValue() / ant.doubleValue() * 100;
            }
            meses.add(new MesFaturamento(rotulos.get(i), v, difValor, difPct, i == idxMelhor));
        }
        BigDecimal media = valores.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(valores.size()), 2, java.math.RoundingMode.HALF_UP);
        return new FaturamentoDetalhe(meses, total, media, idxMelhor < 0 ? null : rotulos.get(idxMelhor));
    }

    /** Entradas por dia da semana, com a participação de cada dia e o mais e o menos movimentado. */
    public List<DiaDetalhe> detalheDias(long[] porDia) {
        long total = Arrays.stream(porDia).sum();
        long max = Arrays.stream(porDia).max().orElse(0);
        long min = Arrays.stream(porDia).min().orElse(0);
        int idxPico = -1, idxVazio = -1;
        for (int d = 0; d < porDia.length; d++) {
            if (total > 0 && idxPico < 0 && porDia[d] == max) idxPico = d;
            if (total > 0 && idxVazio < 0 && porDia[d] == min) idxVazio = d;
        }
        List<DiaDetalhe> lista = new ArrayList<>();
        for (int d = 0; d < porDia.length; d++) {
            lista.add(new DiaDetalhe(DIAS[d], porDia[d], percentual(porDia[d], total), d == idxPico, d == idxVazio && idxVazio != idxPico));
        }
        return lista;
    }

    /** Quantidade e valor em R$ de cada situação de mensalidade. */
    public List<SituacaoDetalhe> detalheSituacoes(List<Mensalidade> mensalidades) {
        long[] qtd = new long[SITUACOES.length];
        BigDecimal[] valor = new BigDecimal[SITUACOES.length];
        Arrays.fill(valor, BigDecimal.ZERO);
        for (Mensalidade m : mensalidades) {
            for (int i = 0; i < SITUACOES.length; i++) {
                if (SITUACOES[i].equals(m.getSituacao())) {
                    qtd[i]++;
                    valor[i] = valor[i].add(valorOuZero(m));
                }
            }
        }
        long total = Arrays.stream(qtd).sum();
        List<SituacaoDetalhe> lista = new ArrayList<>();
        for (int i = 0; i < SITUACOES.length; i++) {
            lista.add(new SituacaoDetalhe(SITUACOES[i], qtd[i], valor[i], percentual(qtd[i], total)));
        }
        return lista;
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
