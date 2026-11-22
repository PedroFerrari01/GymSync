package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Visao360;
import com.example.sistema.academia.dto.Visao360.Evento;
import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.AvaliacaoFisica;
import com.example.sistema.academia.model.Entrada;
import com.example.sistema.academia.model.Matricula;
import com.example.sistema.academia.model.Mensagem;
import com.example.sistema.academia.model.Mensalidade;
import com.example.sistema.academia.repository.AlunoRepository;
import com.example.sistema.academia.repository.AvaliacaoFisicaRepository;
import com.example.sistema.academia.repository.EntradaRepository;
import com.example.sistema.academia.repository.MatriculaRepository;
import com.example.sistema.academia.repository.MensagemRepository;
import com.example.sistema.academia.repository.MensalidadeRepository;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Visão 360° do aluno: cabeçalho, indicadores e linha do tempo (mais recente primeiro). */
@Service
public class Visao360Service {

    static final int MAX_EVENTOS = 60;
    private static final Locale BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private final AlunoRepository alunoRepository;
    private final MatriculaRepository matriculaRepository;
    private final MensalidadeRepository mensalidadeRepository;
    private final AvaliacaoFisicaRepository avaliacaoRepository;
    private final EntradaRepository entradaRepository;
    private final MensagemRepository mensagemRepository;

    public Visao360Service(AlunoRepository alunoRepository, MatriculaRepository matriculaRepository,
                           MensalidadeRepository mensalidadeRepository, AvaliacaoFisicaRepository avaliacaoRepository,
                           EntradaRepository entradaRepository, MensagemRepository mensagemRepository) {
        this.alunoRepository = alunoRepository;
        this.matriculaRepository = matriculaRepository;
        this.mensalidadeRepository = mensalidadeRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.entradaRepository = entradaRepository;
        this.mensagemRepository = mensagemRepository;
    }

    /** Admin vê qualquer aluno; professor só os dele (ficha ou conversa); o resto não entra. */
    public static boolean podeVer(String perfil, Set<Long> alunosDoProfessor, Long alunoId) {
        if (alunoId == null) return false;
        if ("ADMIN".equals(perfil)) return true;
        return "PROFESSOR".equals(perfil) && alunosDoProfessor != null && alunosDoProfessor.contains(alunoId);
    }

    public Optional<Visao360> montar(Long alunoId, LocalDateTime agora) {
        Optional<Aluno> aluno = alunoRepository.findById(alunoId);
        if (aluno.isEmpty()) return Optional.empty();
        return Optional.of(montar(aluno.get(),
                matriculaRepository.findByAlunoId(alunoId),
                mensalidadeRepository.findByMatriculaAlunoId(alunoId),
                avaliacaoRepository.findByAlunoId(alunoId),
                entradaRepository.findByAlunoId(alunoId),
                mensagemRepository.findByAlunoId(alunoId),
                agora));
    }

    /** Parte pura (sem banco): recebe tudo já carregado. */
    public static Visao360 montar(Aluno aluno, List<Matricula> matriculas, List<Mensalidade> mensalidades,
                                  List<AvaliacaoFisica> avaliacoes, List<Entrada> entradas,
                                  List<Mensagem> mensagens, LocalDateTime agora) {
        LocalDate hoje = agora.toLocalDate();
        List<Evento> eventos = new ArrayList<>();

        // A matrícula "atual" é a ativa; sem ativa, a mais recente.
        Matricula atual = null;
        for (Matricula m : matriculas) {
            if (atual == null) {
                atual = m;
            } else if (!"ATIVA".equals(atual.getSituacao())
                    && ("ATIVA".equals(m.getSituacao()) || maisRecente(m, atual))) {
                atual = m;
            }
            if (m.getDataInicio() != null) {
                String plano = m.getPlano() != null ? m.getPlano().getNome() : "plano";
                eventos.add(ev(m.getDataInicio(), "MATRICULA", "Matrícula no plano " + plano,
                        "Situação: " + texto(m.getSituacao())));
            }
        }

        int atraso = 0;
        for (Mensalidade m : mensalidades) {
            String s = m.getSituacao();
            boolean emAtraso = "ATRASADA".equals(s) || "VENCIDA".equals(s);
            if (emAtraso) atraso++;
            if ("PAGA".equals(s) && m.getDataPagamento() != null) {
                eventos.add(ev(m.getDataPagamento(), "PAGAMENTO", "Mensalidade paga", dinheiro(m)));
            } else if (emAtraso && m.getVencimento() != null) {
                eventos.add(ev(m.getVencimento(), "ATRASO", "Mensalidade em atraso", dinheiro(m)));
            } else if ("EM_ANALISE".equals(s) && m.getVencimento() != null) {
                eventos.add(ev(m.getVencimento(), "ANALISE", "Pagamento em análise", dinheiro(m)));
            }
        }

        AvaliacaoFisica ultimaAv = null;
        for (AvaliacaoFisica a : avaliacoes) {
            if (a.getDataAvaliacao() == null) continue;
            if (ultimaAv == null || a.getDataAvaliacao().isAfter(ultimaAv.getDataAvaliacao())) ultimaAv = a;
            List<String> partes = new ArrayList<>();
            if (a.getPeso() != null) partes.add("peso " + num(a.getPeso()) + " kg");
            if (a.getPercentualGordura() != null) partes.add("gordura " + num(a.getPercentualGordura()) + "%");
            eventos.add(ev(a.getDataAvaliacao(), "AVALIACAO", "Avaliação física",
                    capitalizar(String.join(" · ", partes))));
        }

        // Entradas: uma linha por semana (segunda a domingo), para a linha do tempo não ficar enorme.
        List<LocalDate> diasEntrada = entradas.stream().map(Entrada::getDataHoraEntrada)
                .filter(d -> d != null).map(LocalDateTime::toLocalDate).sorted().toList();
        Map<LocalDate, List<LocalDate>> porSemana = new LinkedHashMap<>();
        for (LocalDate d : diasEntrada) {
            porSemana.computeIfAbsent(d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    k -> new ArrayList<>()).add(d);
        }
        for (Map.Entry<LocalDate, List<LocalDate>> e : porSemana.entrySet()) {
            List<LocalDate> dias = e.getValue();
            int n = dias.size();
            eventos.add(ev(dias.get(n - 1), "ENTRADAS",
                    n == 1 ? "1 entrada na catraca" : n + " entradas na catraca",
                    "Semana de " + e.getKey().format(DIA_MES)));
        }

        // Mensagens: só data, quem enviou e quantas (nunca o texto). Agrupa por dia, sentido e professor.
        Map<String, Integer> contagem = new LinkedHashMap<>();
        Map<String, Mensagem> amostra = new LinkedHashMap<>();
        for (Mensagem m : mensagens) {
            if (m.getDataHora() == null) continue;
            String chave = m.getDataHora().toLocalDate() + "|" + m.getRemetente() + "|"
                    + (m.getProfessor() != null ? m.getProfessor().getId() : "");
            contagem.merge(chave, 1, Integer::sum);
            amostra.putIfAbsent(chave, m);
        }
        for (Map.Entry<String, Integer> e : contagem.entrySet()) {
            Mensagem m = amostra.get(e.getKey());
            String prof = m.getProfessor() != null && m.getProfessor().getNome() != null
                    ? m.getProfessor().getNome() : "professor";
            boolean doAluno = "ALUNO".equals(m.getRemetente());
            int n = e.getValue();
            String titulo = (n == 1 ? "Mensagem " : n + " mensagens ") + (doAluno ? "do aluno" : "do professor");
            eventos.add(ev(m.getDataHora().toLocalDate(), "MENSAGEM", titulo,
                    (doAluno ? "Para " : "De ") + prof));
        }

        eventos.sort(Comparator.comparing(Evento::data).reversed());
        if (eventos.size() > MAX_EVENTOS) eventos = new ArrayList<>(eventos.subList(0, MAX_EVENTOS));

        Set<LocalDate> dias30 = new HashSet<>();
        LocalDate corte = hoje.minusDays(29);
        for (LocalDate d : diasEntrada) if (!d.isBefore(corte) && !d.isAfter(hoje)) dias30.add(d);

        String ultimaEntrada = "Nenhuma";
        if (!diasEntrada.isEmpty()) {
            LocalDate u = diasEntrada.get(diasEntrada.size() - 1);
            long dd = ChronoUnit.DAYS.between(u, hoje);
            ultimaEntrada = dd <= 0 ? "Hoje" : dd == 1 ? "Ontem" : "Há " + dd + " dias (" + u.format(DIA_MES) + ")";
        }

        return new Visao360(
                aluno.getNome(),
                atual != null && atual.getPlano() != null ? atual.getPlano().getNome() : "Sem plano",
                atual != null ? texto(atual.getSituacao()) : "Sem matrícula",
                aluno.getEmail(), aluno.getContato(),
                dias30.size(), atraso,
                ultimaAv != null ? ultimaAv.getDataAvaliacao().format(DATA) : "Nenhuma",
                ultimaEntrada, eventos);
    }

    private static boolean maisRecente(Matricula a, Matricula b) {
        return a.getDataInicio() != null && (b.getDataInicio() == null || a.getDataInicio().isAfter(b.getDataInicio()));
    }

    private static Evento ev(LocalDate data, String tipo, String titulo, String detalhe) {
        return new Evento(data, data.format(DATA), tipo, titulo, detalhe);
    }

    private static String dinheiro(Mensalidade m) {
        if (m.getValor() == null) return "";
        NumberFormat nf = NumberFormat.getNumberInstance(BR);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "R$ " + nf.format(m.getValor());
    }

    private static String num(Double v) {
        NumberFormat nf = NumberFormat.getNumberInstance(BR);
        nf.setMaximumFractionDigits(1);
        return nf.format(v);
    }

    private static String texto(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.charAt(0) + s.substring(1).toLowerCase(BR).replace('_', ' ');
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
