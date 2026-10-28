package com.example.sistema.academia.controller;

import com.example.sistema.academia.model.*;
import com.example.sistema.academia.repository.*;
import com.example.sistema.academia.service.AulaService;
import com.example.sistema.academia.service.ExclusaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Controller
public class AulaController {

    @Autowired private AlunoRepository alunoRepository;
    @Autowired private ProfessorRepository professorRepository;
    @Autowired private AulaColetivaRepository aulaColetivaRepository;
    @Autowired private ReservaAulaRepository reservaAulaRepository;
    @Autowired private MatriculaRepository matriculaRepository;
    @Autowired private AulaService aulaService;
    @Autowired private ExclusaoService exclusaoService;

    @PostMapping("/aulas/salvar")
    public String salvar(@RequestParam String nome,
                         @RequestParam(required = false) Long professorId,
                         @RequestParam DayOfWeek diaSemana,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horario,
                         @RequestParam Integer vagas,
                         HttpSession session,
                         RedirectAttributes redirect) {
        if ("PROFESSOR".equals(session.getAttribute("perfil"))) {
            professorId = (Long) session.getAttribute("usuarioId");
        }
        Professor professor = professorId != null ? professorRepository.findById(professorId).orElse(null) : null;
        if (professor == null || nome.isBlank() || vagas < 1 || vagas > 200) {
            redirect.addFlashAttribute("erro", "Informe o nome, o professor e um número de vagas entre 1 e 200.");
            return "redirect:/";
        }
        AulaColetiva aula = new AulaColetiva();
        aula.setNome(nome.trim());
        aula.setProfessor(professor);
        aula.setDiaSemana(diaSemana);
        aula.setHorario(horario);
        aula.setVagas(vagas);
        aulaColetivaRepository.save(aula);
        redirect.addFlashAttribute("sucesso", "Aula coletiva cadastrada!");
        return "redirect:/";
    }

    @PostMapping("/aulas/excluir/{id}")
    public String excluir(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        AulaColetiva aula = aulaColetivaRepository.findById(id).orElse(null);
        if (aula == null) {
            return "redirect:/";
        }
        boolean admin = "ADMIN".equals(session.getAttribute("perfil"));
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (admin || (usuarioId != null && usuarioId.equals(aula.getProfessor().getId()))) {
            exclusaoService.excluirAula(id);
            redirect.addFlashAttribute("sucesso", "Aula e reservas excluídas.");
        } else {
            redirect.addFlashAttribute("erro", "Você só pode excluir as suas aulas.");
        }
        return "redirect:/";
    }

    @PostMapping("/aulas/reservar/{id}")
    public String reservar(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        Long alunoId = (Long) session.getAttribute("usuarioId");
        AulaColetiva aula = aulaColetivaRepository.findById(id).orElse(null);
        Aluno aluno = alunoId != null ? alunoRepository.findById(alunoId).orElse(null) : null;
        if (aula == null || aluno == null) {
            redirect.addFlashAttribute("erro", "Aula não encontrada.");
            return "redirect:/";
        }
        if (!"ATIVO".equals(aluno.getSituacao()) || !matriculaRepository.existsByAlunoIdAndSituacao(alunoId, "ATIVA")) {
            redirect.addFlashAttribute("erro", "Para reservar aulas você precisa estar ativo e com matrícula ativa.");
            return "redirect:/";
        }
        LocalDate data = aulaService.proximaData(aula);
        if (reservaAulaRepository.findFirstByAulaIdAndAlunoIdAndDataAula(id, alunoId, data).isPresent()) {
            redirect.addFlashAttribute("erro", "Você já reservou esta aula.");
            return "redirect:/";
        }
        if (reservaAulaRepository.countByAulaIdAndDataAula(id, data) >= aula.getVagas()) {
            redirect.addFlashAttribute("erro", "Não há mais vagas nesta aula.");
            return "redirect:/";
        }
        ReservaAula reserva = new ReservaAula();
        reserva.setAula(aula);
        reserva.setAluno(aluno);
        reserva.setDataAula(data);
        reservaAulaRepository.save(reserva);
        redirect.addFlashAttribute("sucesso", "Vaga reservada em " + aula.getNome() + "!");
        return "redirect:/";
    }

    @PostMapping("/aulas/cancelar-reserva/{id}")
    public String cancelarReserva(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        Long alunoId = (Long) session.getAttribute("usuarioId");
        AulaColetiva aula = aulaColetivaRepository.findById(id).orElse(null);
        if (aula == null || alunoId == null) {
            return "redirect:/";
        }
        LocalDate data = aulaService.proximaData(aula);
        reservaAulaRepository.findFirstByAulaIdAndAlunoIdAndDataAula(id, alunoId, data).ifPresent(reservaAulaRepository::delete);
        redirect.addFlashAttribute("sucesso", "Reserva cancelada.");
        return "redirect:/";
    }
}
