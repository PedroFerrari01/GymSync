package com.example.sistema.academia.service;

import com.example.sistema.academia.model.AulaColetiva;
import com.example.sistema.academia.model.ReservaAula;
import com.example.sistema.academia.repository.AulaColetivaRepository;
import com.example.sistema.academia.repository.ReservaAulaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import java.time.LocalDate;
import java.time.LocalTime;
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
