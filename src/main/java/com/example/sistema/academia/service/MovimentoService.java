package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Entrada;
import com.example.sistema.academia.repository.EntradaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Movimento da academia por dia da semana e hora (mapa de calor) e a sugestão de melhor horário.
 * Só números agregados: nada aqui identifica quem entrou.
 */
@Service
public class MovimentoService {

    public static final int DIAS_ANALISADOS = 90;
    public static final int HORA_INICIO = 5;
    public static final int HORA_FIM = 23; // inclusive
    private static final int HORAS = HORA_FIM - HORA_INICIO + 1;
    private static final String[] DIAS = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};
    private static final String[] DIAS_LONGOS = {"segunda", "terça", "quarta", "quinta", "sexta", "sábado", "domingo"};

    public record Celula(int hora, long quantidade, int nivel) {}
    public record Linha(String dia, List<Celula> celulas, long total) {}
    public record Mapa(List<String> horas, List<Linha> linhas, long total, long maximo, boolean vazio) {}
    public record Horario(int hora, String texto, String nivelTexto) {}
    public record Sugestao(String quando, List<Horario> melhores, Horario maisCheio, boolean semDados) {}

    @Autowired private EntradaRepository entradaRepository;

    /** Mapa dos últimos 90 dias a partir do repositório. */
    public Mapa mapa(LocalDateTime agora) {
        return mapa(matriz(datas(agora)));
    }

    public record BarraHora(int hora, String rotulo, int percentual, int nivel, boolean melhor, boolean evitar, String titulo) {}
    public record DiaAluno(String curto, String destaque, boolean selecionado, List<BarraHora> barras, String resumo, boolean semDados) {}
    public record PlanoAluno(Sugestao semana, List<DiaAluno> dias) {}

    /** Plano para o aluno: resumo da semana e um gráfico por dia (começando em hoje), só números agregados. */
    public PlanoAluno planoAluno(LocalDateTime agora) {
        return plano(matriz(datas(agora)), agora.getDayOfWeek().getValue() - 1);
    }

    /** {@code hoje}: 0 = segunda .. 6 = domingo. Os dias vêm na ordem hoje, amanhã, depois. */
    public static PlanoAluno plano(long[][] m, int hoje) {
        List<DiaAluno> dias = new ArrayList<>();
        for (int k = 0; k < 7; k++) {
            int d = (hoje + k) % 7;
            Sugestao s = sugerir(m, d);
            long max = 0;
            for (long v : m[d]) max = Math.max(max, v);
            List<BarraHora> barras = new ArrayList<>();
            for (int i = 0; i < HORAS; i++) {
                int h = HORA_INICIO + i;
                boolean melhor = s.melhores().stream().anyMatch(x -> x.hora() == h);
                boolean evitar = s.maisCheio() != null && s.maisCheio().hora() == h;
                int nv = nivel(m[d][i], max);
                String titulo = DIAS[d] + " " + h + "h: " + (nv == 0 ? "vazio" : nv <= 2 ? "tranquilo" : nv <= 4 ? "moderado" : "cheio");
                barras.add(new BarraHora(h, h % 3 == 0 ? h + "h" : "", (int) Math.round(max == 0 ? 0 : m[d][i] * 100.0 / max), nv, melhor, evitar, titulo));
            }
            String resumo;
            if (s.semDados()) {
                resumo = "Sem dados para " + DIAS_LONGOS[d] + " (a academia costuma ficar fechada).";
            } else {
                resumo = "Melhor horário: " + s.melhores().get(0).texto() + " (" + s.melhores().get(0).nivelTexto() + ")."
                        + (s.maisCheio() != null ? " Evite: " + s.maisCheio().texto() + "." : "");
            }
            String destaque = k == 0 ? "Hoje" : k == 1 ? "Amanhã" : DIAS[d];
            dias.add(new DiaAluno(DIAS[d], destaque, k == 0, barras, resumo, s.semDados()));
        }
        return new PlanoAluno(sugerir(m, -1), dias);
    }

    private List<LocalDateTime> datas(LocalDateTime agora) {
        List<LocalDateTime> datas = new ArrayList<>();
        for (Entrada e : entradaRepository.findByDataHoraEntradaAfter(agora.minusDays(DIAS_ANALISADOS))) {
            if (e.getDataHoraEntrada() != null) datas.add(e.getDataHoraEntrada());
        }
        return datas;
    }

    /** Matriz [dia 0=segunda..6=domingo][hora 5..23 -> índice 0..18]. Horas fora da faixa são ignoradas. */
    public static long[][] matriz(Collection<LocalDateTime> entradas) {
        long[][] m = new long[7][HORAS];
        if (entradas == null) return m;
        for (LocalDateTime d : entradas) {
            if (d == null) continue;
            int hora = d.getHour();
            if (hora < HORA_INICIO || hora > HORA_FIM) continue;
            m[d.getDayOfWeek().getValue() - 1][hora - HORA_INICIO]++;
        }
        return m;
    }

    /** Intensidade de 0 (sem entradas) a 5 (o horário mais cheio da grade). */
    static int nivel(long quantidade, long maximo) {
        if (quantidade <= 0 || maximo <= 0) return 0;
        return (int) Math.max(1, Math.ceil(quantidade * 5.0 / maximo));
    }

    public static Mapa mapa(long[][] m) {
        long maximo = 0;
        long total = 0;
        for (long[] linha : m) for (long v : linha) { maximo = Math.max(maximo, v); total += v; }
        List<String> horas = new ArrayList<>();
        for (int h = HORA_INICIO; h <= HORA_FIM; h++) horas.add(h + "h");
        List<Linha> linhas = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            List<Celula> celulas = new ArrayList<>();
            long soma = 0;
            for (int i = 0; i < HORAS; i++) {
                celulas.add(new Celula(HORA_INICIO + i, m[d][i], nivel(m[d][i], maximo)));
                soma += m[d][i];
            }
            linhas.add(new Linha(DIAS[d], celulas, soma));
        }
        return new Mapa(horas, linhas, total, maximo, total == 0);
    }

    /**
     * Os 3 horários mais vazios e o mais cheio. {@code dia} = 0..6 para um dia da semana, ou -1 para a semana toda.
     * Só entram horas em que a academia teve movimento em algum dia (horas sem nenhum dado são fora do funcionamento).
     */
    public static Sugestao sugerir(long[][] m, int dia) {
        String quando = dia < 0 ? "na semana" : "hoje (" + DIAS_LONGOS[dia] + ")";
        long[] semana = new long[HORAS];
        long[] alvo = new long[HORAS];
        for (int d = 0; d < 7; d++) {
            for (int i = 0; i < HORAS; i++) {
                semana[i] += m[d][i];
                if (dia < 0 || dia == d) alvo[i] += m[d][i];
            }
        }
        long totalAlvo = 0;
        for (long v : alvo) totalAlvo += v;
        if (totalAlvo == 0) return new Sugestao(quando, List.of(), null, true);

        List<Integer> candidatas = new ArrayList<>();
        long maximo = 0;
        for (int i = 0; i < HORAS; i++) {
            if (semana[i] > 0) candidatas.add(i);
            maximo = Math.max(maximo, alvo[i]);
        }
        final long max = maximo;
        List<Integer> ordem = new ArrayList<>(candidatas);
        ordem.sort(Comparator.<Integer>comparingLong(i -> alvo[i]).thenComparingInt(i -> i));
        List<Horario> melhores = new ArrayList<>();
        for (int k = 0; k < Math.min(3, ordem.size()); k++) {
            int i = ordem.get(k);
            melhores.add(horario(i, alvo[i], max));
        }
        int cheio = -1;
        for (int i : candidatas) if (cheio < 0 || alvo[i] > alvo[cheio]) cheio = i;
        Horario cheioCandidato = cheio >= 0 && alvo[cheio] > 0 && candidatas.size() > 1 ? horario(cheio, alvo[cheio], max) : null;
        // Se o mais cheio já está entre os melhores (todos iguais), não faz sentido pedir para evitar.
        Horario maisCheio = cheioCandidato != null && melhores.stream().noneMatch(h -> h.hora() == cheioCandidato.hora())
                ? cheioCandidato : null;
        return new Sugestao(quando, melhores, maisCheio, false);
    }

    private static Horario horario(int indice, long quantidade, long maximo) {
        int h = HORA_INICIO + indice;
        String nivel = quantidade == 0 ? "vazio" : nivel(quantidade, maximo) <= 2 ? "tranquilo" : nivel(quantidade, maximo) <= 4 ? "moderado" : "cheio";
        return new Horario(h, h + "h às " + (h + 1) + "h", nivel);
    }
}
