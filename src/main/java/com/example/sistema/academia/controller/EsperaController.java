package com.example.sistema.academia.controller;

import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.AulaColetiva;
import com.example.sistema.academia.repository.AlunoRepository;
import com.example.sistema.academia.repository.AulaColetivaRepository;
import com.example.sistema.academia.repository.MatriculaRepository;
import com.example.sistema.academia.service.EsperaFila;
import com.example.sistema.academia.service.EsperaService;
import com.example.sistema.academia.service.GradeSemanal;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Lista de espera das aulas lotadas. Só o aluno logado mexe, e só na própria vaga da fila. */
@Controller
public class EsperaController {

    @Autowired private AlunoRepository alunoRepository;
    @Autowired private AulaColetivaRepository aulaColetivaRepository;
    @Autowired private MatriculaRepository matriculaRepository;
    @Autowired private EsperaService esperaService;

    @PostMapping("/aulas/espera/entrar/{id}")
    public String entrar(@PathVariable Long id, @RequestParam(name = "data", required = false) String dataTexto,
                         HttpSession session, RedirectAttributes redirect) {
        Long alunoId = (Long) session.getAttribute("usuarioId");
        AulaColetiva aula = aulaColetivaRepository.findById(id).orElse(null);
        Aluno aluno = alunoId != null ? alunoRepository.findById(alunoId).orElse(null) : null;
        LocalDate data = dataPedida(dataTexto);
        if (aula == null || aluno == null || data == null) {
            redirect.addFlashAttribute("erro", "Aula não encontrada.");
            return "redirect:/";
        }
        String volta = "redirect:/?semana=" + GradeSemanal.segundaDa(data);
        if (!"ATIVO".equals(aluno.getSituacao()) || !matriculaRepository.existsByAlunoIdAndSituacao(alunoId, "ATIVA")) {
            redirect.addFlashAttribute("erro", "Para entrar na lista de espera você precisa estar ativo e com matrícula ativa.");
            return volta;
        }
        EsperaFila.Resultado r = esperaService.entrar(aula, aluno, data, GradeSemanal.dataValida(aula, data, LocalDateTime.now()));
        if (r == EsperaFila.Resultado.OK) {
            redirect.addFlashAttribute("sucesso", "Você entrou na lista de espera de " + aula.getNome() + ". Avisamos no sininho quando abrir vaga.");
        } else {
            redirect.addFlashAttribute("erro", EsperaFila.mensagem(r));
        }
        return volta;
    }

    @PostMapping("/aulas/espera/sair/{id}")
    public String sair(@PathVariable Long id, @RequestParam(name = "data", required = false) String dataTexto,
                       HttpSession session, RedirectAttributes redirect) {
        Long alunoId = (Long) session.getAttribute("usuarioId");
        AulaColetiva aula = aulaColetivaRepository.findById(id).orElse(null);
        LocalDate data = dataPedida(dataTexto);
        if (aula == null || alunoId == null || data == null) return "redirect:/";
        esperaService.sair(aula, alunoId, data);
        redirect.addFlashAttribute("sucesso", "Você saiu da lista de espera.");
        return "redirect:/?semana=" + GradeSemanal.segundaDa(data);
    }

    private static LocalDate dataPedida(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return LocalDate.parse(texto.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
