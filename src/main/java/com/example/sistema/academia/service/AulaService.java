package com.example.sistema.academia.service;

import com.example.sistema.academia.model.AulaColetiva;
import com.example.sistema.academia.model.ReservaAula;
import com.example.sistema.academia.repository.AulaColetivaRepository;
import com.example.sistema.academia.repository.ReservaAulaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AulaService {

    @Autowired private AulaColetivaRepository aulaColetivaRepository;
    @Autowired private ReservaAulaRepository reservaAulaRepository;

    public LocalDate proximaData(AulaColetiva aula) {
        LocalDate hoje = LocalDate.now();
        LocalDate data = hoje.with(TemporalAdjusters.nextOrSame(aula.getDiaSemana()));
        if (data.equals(hoje) && aula.getHorario() != null && LocalTime.now().isAfter(aula.getHorario())) {
            data = data.plusWeeks(1);
        }
        return data;
    }

    private static final String[] NOMES_DIAS = {"Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"};

    /** Grade semanal (seg a dom) para a tela de aulas. Não grava nada. */
    public void grade(Model model, Long alunoId, String semanaPedida) {
        LocalDateTime agora = LocalDateTime.now();
        LocalDate hoje = agora.toLocalDate();
        LocalDate segunda = GradeSemanal.semanaPedida(semanaPedida, hoje);
        LocalDate segundaHoje = GradeSemanal.segundaDa(hoje);
        List<AulaColetiva> aulas = aulaColetivaRepository.findAll();

        // monta cada célula (aula + data da semana) uma vez só
        Map<Long, GradeSemanal.CelulaAula> celulas = new HashMap<>();
        for (AulaColetiva a : aulas) {
            if (a.getDiaSemana() == null || a.getHorario() == null) continue;
            LocalDate data = segunda.plusDays(a.getDiaSemana().getValue() - 1L);
            List<ReservaAula> reservas = reservaAulaRepository.findByAulaIdAndDataAula(a.getId(), data);
            boolean minha = alunoId != null && reservas.stream().anyMatch(r -> r.getAluno().getId().equals(alunoId));
            int vagas = a.getVagas() == null ? 0 : a.getVagas();
            int livres = vagas - reservas.size();
            celulas.put(a.getId(), new GradeSemanal.CelulaAula(a, data, livres, reservas.size(), minha,
                    GradeSemanal.estado(data, a.getHorario(), agora, livres, minha)));
        }

        List<GradeSemanal.Linha> linhas = new ArrayList<>();
        for (LocalTime h : GradeSemanal.horarios(aulas)) {
            List<List<GradeSemanal.CelulaAula>> dias = new ArrayList<>();
            for (DayOfWeek d : DayOfWeek.values()) {
                List<GradeSemanal.CelulaAula> lista = new ArrayList<>();
                for (AulaColetiva a : GradeSemanal.aulasDe(aulas, d, h)) lista.add(celulas.get(a.getId()));
                dias.add(lista);
            }
            linhas.add(new GradeSemanal.Linha(h, dias));
        }

        List<GradeSemanal.Dia> dias = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate data = segunda.plusDays(i);
            List<GradeSemanal.CelulaAula> doDia = new ArrayList<>();
            for (GradeSemanal.Linha l : linhas) doDia.addAll(l.dias().get(i));
            dias.add(new GradeSemanal.Dia(data, NOMES_DIAS[i], data.equals(hoje), doDia));
        }

        DateTimeFormatter dm = DateTimeFormatter.ofPattern("dd/MM");
        model.addAttribute("gradeLinhas", linhas);
        model.addAttribute("gradeDias", dias);
        model.addAttribute("gradeTitulo", segunda.format(dm) + " a " + segunda.plusDays(6).format(dm));
        model.addAttribute("gradeAnterior", segunda.isAfter(segundaHoje.minusWeeks(8)) ? segunda.minusWeeks(1).toString() : null);
        model.addAttribute("gradeSeguinte", segunda.isBefore(segundaHoje.plusWeeks(8)) ? segunda.plusWeeks(1).toString() : null);
        model.addAttribute("gradeSemanaAtual", segunda.equals(segundaHoje));
        model.addAttribute("gradeSemana", segunda.toString());
    }

    public void preencher(Model model, Long alunoId) {
        List<AulaColetiva> aulas = new ArrayList<>(aulaColetivaRepository.findAll());
        aulas.sort(Comparator.comparing(AulaColetiva::getDiaSemana).thenComparing(AulaColetiva::getHorario));

        Map<Long, LocalDate> datas = new HashMap<>();
        Map<Long, Integer> restantes = new HashMap<>();
        Map<Long, String> nomes = new HashMap<>();
        Set<Long> reservadas = new HashSet<>();

        for (AulaColetiva aula : aulas) {
            LocalDate data = proximaData(aula);
            List<ReservaAula> reservas = reservaAulaRepository.findByAulaIdAndDataAula(aula.getId(), data);
            datas.put(aula.getId(), data);
            restantes.put(aula.getId(), aula.getVagas() - reservas.size());
            nomes.put(aula.getId(), reservas.stream().map(r -> r.getAluno().getNome()).collect(Collectors.joining(", ")));
            if (alunoId != null && reservas.stream().anyMatch(r -> r.getAluno().getId().equals(alunoId))) {
                reservadas.add(aula.getId());
            }
        }

        model.addAttribute("aulas", aulas);
        model.addAttribute("proximasDatas", datas);
        model.addAttribute("vagasRestantes", restantes);
        model.addAttribute("nomesReservas", nomes);
        model.addAttribute("aulasReservadas", reservadas);
    }
}
