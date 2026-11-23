package com.example.sistema.academia.controller;

import com.example.sistema.academia.model.*;
import com.example.sistema.academia.repository.*;
import com.example.sistema.academia.service.AulaService;
import com.example.sistema.academia.service.GradeSemanal;
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
import java.time.LocalDateTime;
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
    @Autowired private com.example.sistema.academia.service.EsperaService esperaService; // ESPERA

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
        if (professor == null || nome.isBlank() || nome.trim().length() > 100 || vagas < 1 || vagas > 200) {
            redirect.addFlashAttribute("erro", "Informe o nome (até 100 caracteres), o professor e um número de vagas entre 1 e 200.");
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
    public String reservar(@PathVariable Long id, @RequestParam(name = "data", required = false) String dataTexto,
                           HttpSession session, RedirectAttributes redirect) {
        Long alunoId = (Long) session.getAttribute("usuarioId");
        AulaColetiva aula = aulaColetivaRepository.findById(id).orElse(null);
        Aluno aluno = alunoId != null ? alunoRepository.findById(alunoId).orElse(null) : null;
        if (aula == null || aluno == null) {
            redirect.addFlashAttribute("erro", "Aula não encontrada.");
            return "redirect:/";
        }
        LocalDate pedida = dataPedida(dataTexto);
        String volta = voltaGrade(pedida);
        if (!"ATIVO".equals(aluno.getSituacao()) || !matriculaRepository.existsByAlunoIdAndSituacao(alunoId, "ATIVA")) {
            redirect.addFlashAttribute("erro", "Para reservar aulas você precisa estar ativo e com matrícula ativa.");
            return volta;
        }
        LocalDate data = pedida != null ? pedida : aulaService.proximaData(aula);
        if (pedida != null && !GradeSemanal.dataValida(aula, data, LocalDateTime.now())) {
            redirect.addFlashAttribute("erro", "Esta aula já passou ou ainda não abriu para reserva.");
            return volta;
        }
        if (reservaAulaRepository.findFirstByAulaIdAndAlunoIdAndDataAula(id, alunoId, data).isPresent()) {
            redirect.addFlashAttribute("erro", "Você já reservou esta aula.");
            return volta;
        }
        if (reservaAulaRepository.countByAulaIdAndDataAula(id, data) >= aula.getVagas()) {
            redirect.addFlashAttribute("erro", "Não há mais vagas nesta aula.");
            return volta;
        }
        ReservaAula reserva = new ReservaAula();
        reserva.setAula(aula);
        reserva.setAluno(aluno);
        reserva.setDataAula(data);
        reservaAulaRepository.save(reserva);
        esperaService.aoReservar(id, alunoId, data); // ESPERA: quem reservou sai da fila
        redirect.addFlashAttribute("sucesso", "Vaga reservada em " + aula.getNome() + "!");
        return volta;
    }

    @PostMapping("/aulas/cancelar-reserva/{id}")
    public String cancelarReserva(@PathVariable Long id, @RequestParam(name = "data", required = false) String dataTexto,
                                  HttpSession session, RedirectAttributes redirect) {
        Long alunoId = (Long) session.getAttribute("usuarioId");
        AulaColetiva aula = aulaColetivaRepository.findById(id).orElse(null);
        if (aula == null || alunoId == null) {
            return "redirect:/";
        }
        LocalDate pedida = dataPedida(dataTexto);
        LocalDate data = pedida != null ? pedida : aulaService.proximaData(aula);
        if (pedida != null && pedida.isBefore(LocalDate.now())) {
            redirect.addFlashAttribute("erro", "Aula que já passou não pode ser cancelada.");
            return voltaGrade(pedida);
        }
        boolean tinha = reservaAulaRepository.findFirstByAulaIdAndAlunoIdAndDataAula(id, alunoId, data).isPresent();
        reservaAulaRepository.findFirstByAulaIdAndAlunoIdAndDataAula(id, alunoId, data).ifPresent(reservaAulaRepository::delete);
        if (tinha) esperaService.aoCancelarReserva(aula, data); // ESPERA: abriu vaga, avisa o primeiro da fila
        redirect.addFlashAttribute("sucesso", "Reserva cancelada.");
        return voltaGrade(pedida);
    }

    /** Data "aaaa-mm-dd" enviada pela grade; nula se não veio ou veio inválida. */
    private static LocalDate dataPedida(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return LocalDate.parse(texto.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Volta para a mesma semana da grade (com a data), ou para a tela inicial. */
    private static String voltaGrade(LocalDate data) {
        return data == null ? "redirect:/" : "redirect:/?semana=" + GradeSemanal.segundaDa(data);
    }
}
