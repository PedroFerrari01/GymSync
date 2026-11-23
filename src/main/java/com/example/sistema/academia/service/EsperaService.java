package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Notificacao;
import com.example.sistema.academia.model.*;
import com.example.sistema.academia.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Lista de espera das aulas lotadas (melhoria 18): entrar, sair, aviso quando abre vaga e dados da grade. */
@Service
public class EsperaService {

    @Autowired private EsperaAulaRepository esperaRepository;
    @Autowired private ReservaAulaRepository reservaAulaRepository;

    /** Chave usada pelas células da grade: "idDaAula|aaaa-mm-dd". */
    public static String chave(Long aulaId, LocalDate data) {
        return aulaId + "|" + data;
    }

    private int livres(AulaColetiva aula, LocalDate data) {
        int vagas = aula.getVagas() == null ? 0 : aula.getVagas();
        return vagas - (int) reservaAulaRepository.countByAulaIdAndDataAula(aula.getId(), data);
    }

    /** Tenta pôr o aluno na fila. Devolve OK ou o motivo de recusa. */
    @Transactional
    public EsperaFila.Resultado entrar(AulaColetiva aula, Aluno aluno, LocalDate data, boolean dataValida) {
        boolean reservou = reservaAulaRepository.findFirstByAulaIdAndAlunoIdAndDataAula(aula.getId(), aluno.getId(), data).isPresent();
        boolean naFila = esperaRepository.findFirstByAulaIdAndAlunoIdAndDataAula(aula.getId(), aluno.getId(), data).isPresent();
        EsperaFila.Resultado r = EsperaFila.podeEntrar(dataValida, livres(aula, data), reservou, naFila);
        if (r != EsperaFila.Resultado.OK) return r;
        EsperaAula e = new EsperaAula();
        e.setAula(aula);
        e.setAluno(aluno);
        e.setDataAula(data);
        e.setCriadaEm(LocalDateTime.now());
        esperaRepository.save(e);
        return r;
    }

    /** Sai da fila (a busca é pelo aluno logado: só mexe na própria vaga). Se estava avisado, a vez passa ao próximo. */
    @Transactional
    public boolean sair(AulaColetiva aula, Long alunoId, LocalDate data) {
        Optional<EsperaAula> e = esperaRepository.findFirstByAulaIdAndAlunoIdAndDataAula(aula.getId(), alunoId, data);
        if (e.isEmpty()) return false;
        esperaRepository.delete(e.get());
        promoverVaga(aula, data);
        return true;
    }

    /** O aluno reservou: ele deixa a fila dessa aula e data. */
    @Transactional
    public void aoReservar(Long aulaId, Long alunoId, LocalDate data) {
        esperaRepository.findFirstByAulaIdAndAlunoIdAndDataAula(aulaId, alunoId, data).ifPresent(esperaRepository::delete);
    }

    /** Alguém cancelou a reserva: se abriu vaga, avisa o primeiro da fila (não reserva sozinho). */
    @Transactional
    public void aoCancelarReserva(AulaColetiva aula, LocalDate data) {
        promoverVaga(aula, data);
    }

    private void promoverVaga(AulaColetiva aula, LocalDate data) {
        List<EsperaAula> fila = esperaRepository.findByAulaIdAndDataAulaOrderByIdAsc(aula.getId(), data);
        esperaRepository.saveAll(EsperaFila.promover(fila, livres(aula, data), LocalDateTime.now()));
    }

    /**
     * Dados para a grade da semana que o AulaService já pôs no modelo: posição do aluno na fila
     * ("esperaPos") e quantos há na fila ("esperaTotal"). Sempre põe os dois mapas (vazios se não houver fila).
     */
    public void preencherGrade(Model model, Long alunoId) {
        Map<String, Integer> pos = new HashMap<>();
        Map<String, Integer> total = new HashMap<>();
        Object semana = model.asMap().get("gradeSemana");
        if (semana != null) {
            LocalDate segunda = LocalDate.parse(semana.toString());
            Map<String, List<EsperaAula>> porCelula = new LinkedHashMap<>();
            for (EsperaAula e : esperaRepository.findByDataAulaBetweenOrderByIdAsc(segunda, segunda.plusDays(6))) {
                porCelula.computeIfAbsent(chave(e.getAula().getId(), e.getDataAula()), k -> new ArrayList<>()).add(e);
            }
            porCelula.forEach((k, fila) -> {
                total.put(k, fila.size());
                int p = EsperaFila.posicao(fila, alunoId);
                if (p > 0) pos.put(k, p);
            });
        }
        model.addAttribute("esperaPos", pos);
        model.addAttribute("esperaTotal", total);
    }

    /** Avisos do sininho do aluno: vaga aberta nas aulas em que ele foi avisado e ainda não reservou. */
    public List<Notificacao> sininho(Long alunoId, LocalDateTime agora) {
        List<Notificacao> lista = new ArrayList<>();
        DateTimeFormatter dm = DateTimeFormatter.ofPattern("dd/MM");
        for (EsperaAula e : esperaRepository.findByAlunoId(alunoId)) {
            if (e.getNotificadaEm() == null || e.getAula().getHorario() == null) continue;
            if (e.getDataAula().atTime(e.getAula().getHorario()).isBefore(agora)) continue;
            if (livres(e.getAula(), e.getDataAula()) <= 0) continue; // alguém já ocupou a vaga
            lista.add(Notificacao.naAba("info", textoVaga(e.getAula().getNome(), e.getDataAula().format(dm)), "btn-aba-aulas", null));
        }
        return lista;
    }

    public static String textoVaga(String aula, String dataDiaMes) {
        return "Abriu uma vaga na aula " + aula + " (" + dataDiaMes + "): reserve agora.";
    }
}
