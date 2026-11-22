package com.example.sistema.academia.controller;

import com.example.sistema.academia.service.AlunosDoProfessorService;
import com.example.sistema.academia.service.Visao360Service;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Set;

/** Visão 360° do aluno (só leitura): admin vê todos; professor só os alunos dele. */
@Controller
public class Visao360Controller {

    private final Visao360Service servico;
    private final AlunosDoProfessorService alunosDoProfessor;

    public Visao360Controller(Visao360Service servico, AlunosDoProfessorService alunosDoProfessor) {
        this.servico = servico;
        this.alunosDoProfessor = alunosDoProfessor;
    }

    @GetMapping("/alunos/{id}/visao-360")
    public String visao(@PathVariable Long id, HttpSession session, Model model) {
        String perfil = (String) session.getAttribute("perfil");
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        Set<Long> meus = "PROFESSOR".equals(perfil) ? alunosDoProfessor.ids(usuarioId) : Set.of();
        if (!Visao360Service.podeVer(perfil, meus, id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        model.addAttribute("visao", servico.montar(id, LocalDateTime.now()).orElse(null));
        return "fragments/visao360 :: conteudo";
    }
}
