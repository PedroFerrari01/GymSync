package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Conquista;
import com.example.sistema.academia.model.Entrada;
import com.example.sistema.academia.model.Mensalidade;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Conquistas do aluno, calculadas na hora a partir de entradas, avaliações, mensalidades e registros de exercício. */
@Service
public class ConquistasService {

    public static final int META_TREINOS_MES = 10;
    public static final int META_DIAS_SEMANA = 3;
    public static final int META_SEMANAS_SEGUIDAS = 12;
    public static final int META_MENSALIDADES = 3;
    public static final int META_MADRUGADAS = 5;
    public static final int META_REGISTROS = 5;
    public static final int HORA_MADRUGADA = 7;

    public List<Conquista> calcular(List<Entrada> entradas, int avaliacoes, List<Mensalidade> mensalidades,
                                    int registrosExercicio, LocalDate hoje) {
        List<Entrada> validas = new ArrayList<>();
        if (entradas != null) {
            for (Entrada e : entradas) if (e != null && e.getDataHoraEntrada() != null) validas.add(e);
        }

        YearMonth mesAtual = YearMonth.from(hoje);
        Set<LocalDate> diasNoMes = new HashSet<>();
        Map<LocalDate, Set<LocalDate>> diasPorSemana = new TreeMap<>();
        int madrugadas = 0;
        for (Entrada e : validas) {
            LocalDate dia = e.getDataHoraEntrada().toLocalDate();
            if (YearMonth.from(dia).equals(mesAtual)) diasNoMes.add(dia);
            diasPorSemana.computeIfAbsent(segunda(dia), k -> new HashSet<>()).add(dia);
            if (e.getDataHoraEntrada().getHour() < HORA_MADRUGADA) madrugadas++;
        }

        int melhorSemana = 0;
        for (Set<LocalDate> dias : diasPorSemana.values()) melhorSemana = Math.max(melhorSemana, dias.size());

        int semanasSeguidas = melhorSequenciaSemanas(diasPorSemana.keySet());

        int pagas = 0;
        boolean atrasada = false;
        if (mensalidades != null) {
            for (Mensalidade m : mensalidades) {
                if (m == null) continue;
                if ("PAGA".equals(m.getSituacao())) pagas++;
                if ("ATRASADA".equals(m.getSituacao())) atrasada = true;
            }
        }
        // com mensalidade em atraso a conquista não vale, mesmo com parcelas pagas
        int emDia = atrasada ? Math.min(pagas, META_MENSALIDADES - 1) : pagas;

        List<Conquista> lista = new ArrayList<>();
        lista.add(nova("treinos-mes", "10 treinos no mês", "Passe pela catraca em 10 dias do mês.", "ph-barbell",
                diasNoMes.size(), META_TREINOS_MES, " treinos"));
        lista.add(nova("semana-completa", "Semana completa", "Treine em 3 dias da mesma semana.", "ph-calendar-check",
                melhorSemana, META_DIAS_SEMANA, " dias"));
        lista.add(nova("tres-meses", "3 meses sem faltar", "Pelo menos 1 treino por semana, por 12 semanas seguidas.", "ph-flame",
                semanasSeguidas, META_SEMANAS_SEGUIDAS, " semanas"));
        lista.add(nova("primeira-avaliacao", "Primeira avaliação", "Faça sua primeira avaliação física.", "ph-ruler",
                avaliacoes > 0 ? 1 : 0, 1, " avaliação"));
        lista.add(nova("em-dia", "Mensalidades em dia", "Tenha 3 mensalidades pagas e nenhuma em atraso.", "ph-seal-check",
                emDia, META_MENSALIDADES, " pagas"));
        lista.add(nova("madrugador", "Madrugador", "Entre na academia antes das 7h em 5 dias.", "ph-sun-horizon",
                madrugadas, META_MADRUGADAS, " entradas"));
        lista.add(nova("diario", "Diário de treino", "Registre a carga de 5 exercícios da sua ficha.", "ph-notebook",
                Math.max(0, registrosExercicio), META_REGISTROS, " registros"));
        return lista;
    }

    private Conquista nova(String id, String nome, String descricao, String icone, int valor, int meta, String unidade) {
        int atual = Math.min(valor, meta);
        String progresso = atual >= meta ? "Conquistada" : atual + " de " + meta + unidade;
        return new Conquista(id, nome, descricao, icone, atual, meta, progresso);
    }

    static LocalDate segunda(LocalDate dia) {
        return dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Maior número de semanas seguidas (segundas-feiras consecutivas) com pelo menos uma entrada. */
    static int melhorSequenciaSemanas(Collection<LocalDate> segundas) {
        List<LocalDate> ordenadas = new ArrayList<>(new TreeSet<>(segundas));
        int melhor = 0, atual = 0;
        LocalDate anterior = null;
        for (LocalDate s : ordenadas) {
            atual = (anterior != null && anterior.plusWeeks(1).equals(s)) ? atual + 1 : 1;
            melhor = Math.max(melhor, atual);
            anterior = s;
        }
        return melhor;
    }
}
