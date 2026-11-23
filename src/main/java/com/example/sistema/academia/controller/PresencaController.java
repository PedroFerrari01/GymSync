package com.example.sistema.academia.controller;

import com.example.sistema.academia.model.ReservaAula;
import com.example.sistema.academia.repository.ReservaAulaRepository;
import com.example.sistema.academia.service.GradeSemanal;
import com.example.sistema.academia.service.PresencaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/** Presença em aula coletiva: marcar Presente/Faltou (professor da aula ou admin). */
@Controller
public class PresencaController {

    private final PresencaService presencaService;
    private final ReservaAulaRepository reservaRepository;

    public PresencaController(PresencaService presencaService, ReservaAulaRepository reservaRepository) {
        this.presencaService = presencaService;
        this.reservaRepository = reservaRepository;
    }

    @PostMapping("/aulas/presenca")
    public String marcar(@RequestParam Long reservaId, @RequestParam boolean presente,
                         HttpSession session, RedirectAttributes redirect) {
        String perfil = (String) session.getAttribute("perfil");
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        String quem = (String) session.getAttribute("nomeUsuario");
        ReservaAula reserva = reservaRepository.findById(reservaId).orElse(null);
        String erro = presencaService.marcar(reservaId, presente, perfil, usuarioId, quem == null ? perfil : quem, LocalDate.now());
        if (erro != null || reserva == null) {
            redirect.addFlashAttribute("erro", erro != null ? erro : "Reserva não encontrada.");
            return "redirect:/";
        }
        redirect.addFlashAttribute("sucesso", presente ? "Presença marcada." : "Falta marcada.");
        redirect.addFlashAttribute("presencaAberta", reserva.getAula().getId() + "_" + reserva.getDataAula());
        return "redirect:/?semana=" + GradeSemanal.segundaDa(reserva.getDataAula());
    }

    /** Põe os dados da presença na tela inicial (só GET /), sem mexer no AcademiaController. */
    @ControllerAdvice(assignableTypes = AcademiaController.class)
    public static class DadosDaTela {

        private final PresencaService presencaService;

        public DadosDaTela(PresencaService presencaService) {
            this.presencaService = presencaService;
        }

        @ModelAttribute
        public void presenca(HttpServletRequest request, HttpSession session, Model model) {
            if (!"GET".equals(request.getMethod()) || !"/".equals(request.getRequestURI())) return;
            String perfil = (String) session.getAttribute("perfil");
            Long usuarioId = (Long) session.getAttribute("usuarioId");
            if (perfil == null) return;
            LocalDate hoje = LocalDate.now();
            if ("ALUNO".equals(perfil)) {
                if (usuarioId != null) model.addAttribute("presencaAluno", presencaService.doAluno(usuarioId, hoje));
            } else {
                Long dono = "PROFESSOR".equals(perfil) ? usuarioId : null;
                if ("PROFESSOR".equals(perfil) && dono == null) return;
                model.addAttribute("presencaSessoes", presencaService.sessoes(dono, hoje));
                model.addAttribute("presencaRelatorio", presencaService.relatorio(dono, hoje));
            }
        }
    }
}
