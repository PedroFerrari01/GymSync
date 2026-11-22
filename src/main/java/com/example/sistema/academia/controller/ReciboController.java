package com.example.sistema.academia.controller;

import com.example.sistema.academia.model.Mensalidade;
import com.example.sistema.academia.repository.MensalidadeRepository;
import com.example.sistema.academia.service.ReciboService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Recibo de mensalidade paga, pronto para imprimir ou salvar como PDF. */
@Controller
public class ReciboController {

    @Autowired private MensalidadeRepository mensalidadeRepository;

    @GetMapping("/mensalidades/{id}/recibo")
    public String recibo(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirect) {
        Mensalidade m = mensalidadeRepository.findById(id).orElse(null);
        String perfil = (String) session.getAttribute("perfil");
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        // Mesma resposta para "não existe" e "é de outro aluno": não revela quais ids existem.
        if (m == null || !ReciboService.podeVer(perfil, usuarioId, m.getMatricula().getAluno().getId())) {
            redirect.addFlashAttribute("erro", "Recibo não encontrado.");
            return "redirect:/";
        }
        if (!ReciboService.temRecibo(m)) {
            redirect.addFlashAttribute("erro", "O recibo só existe depois que a mensalidade é paga.");
            return "redirect:/";
        }
        model.addAttribute("m", m);
        model.addAttribute("numeroRecibo", ReciboService.numero(m));
        model.addAttribute("cpfMascarado", ReciboService.cpfMascarado(m.getMatricula().getAluno().getCpf()));
        model.addAttribute("emitidoPor", session.getAttribute("nomeUsuario"));
        return "recibo";
    }
}
