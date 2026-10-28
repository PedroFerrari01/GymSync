package com.example.sistema.academia.controller;

import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.AvaliacaoFisica;
import com.example.sistema.academia.repository.AlunoRepository;
import com.example.sistema.academia.repository.AvaliacaoFisicaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
public class AvaliacaoController {

    @Autowired private AlunoRepository alunoRepository;
    @Autowired private AvaliacaoFisicaRepository avaliacaoFisicaRepository;

    private boolean medidaInvalida(Double valor, double maximo) {
        return valor != null && (valor <= 0 || valor > maximo);
    }

    @PostMapping("/avaliacoes/salvar")
    public String salvar(@RequestParam Long alunoId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAvaliacao,
                         @RequestParam Double peso,
                         @RequestParam(required = false) Double altura,
                         @RequestParam(required = false) Double percentualGordura,
                         @RequestParam(required = false) Double cintura,
                         @RequestParam(required = false) Double quadril,
                         @RequestParam(required = false) Double peito,
                         @RequestParam(required = false) Double braco,
                         @RequestParam(required = false) Double coxa,
                         @RequestParam(required = false) String observacao,
                         HttpSession session,
                         RedirectAttributes redirect) {
        Aluno aluno = alunoRepository.findById(alunoId).orElse(null);
        if (aluno == null) {
            redirect.addFlashAttribute("erro", "Selecione o aluno.");
            return "redirect:/";
        }
        if (dataAvaliacao.isAfter(LocalDate.now()) || peso <= 0 || peso > 500
                || medidaInvalida(altura, 3) || medidaInvalida(percentualGordura, 80)
                || medidaInvalida(cintura, 300) || medidaInvalida(quadril, 300) || medidaInvalida(peito, 300)
                || medidaInvalida(braco, 150) || medidaInvalida(coxa, 200)) {
            redirect.addFlashAttribute("erro", "Confira os valores da avaliação (altura em metros, medidas em centímetros).");
            return "redirect:/";
        }

        AvaliacaoFisica a = new AvaliacaoFisica();
        a.setAluno(aluno);
        a.setDataAvaliacao(dataAvaliacao);
        a.setPeso(peso);
        a.setAltura(altura);
        a.setPercentualGordura(percentualGordura);
        a.setCintura(cintura);
        a.setQuadril(quadril);
        a.setPeito(peito);
        a.setBraco(braco);
        a.setCoxa(coxa);
        if (observacao != null && !observacao.isBlank()) {
            String limpa = observacao.trim();
            a.setObservacao(limpa.length() > 300 ? limpa.substring(0, 300) : limpa);
        }
        if ("PROFESSOR".equals(session.getAttribute("perfil"))) {
            a.setProfessorId((Long) session.getAttribute("usuarioId"));
        }
        a.setRegistradoPor((String) session.getAttribute("nomeUsuario"));
        avaliacaoFisicaRepository.save(a);
        redirect.addFlashAttribute("sucesso", "Avaliação física registrada para " + aluno.getNome() + "!");
        return "redirect:/";
    }

    @PostMapping("/avaliacoes/excluir/{id}")
    public String excluir(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        AvaliacaoFisica a = avaliacaoFisicaRepository.findById(id).orElse(null);
        if (a == null) {
            return "redirect:/";
        }
        boolean admin = "ADMIN".equals(session.getAttribute("perfil"));
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (admin || (usuarioId != null && usuarioId.equals(a.getProfessorId()))) {
            avaliacaoFisicaRepository.deleteById(id);
            redirect.addFlashAttribute("sucesso", "Avaliação excluída.");
        } else {
            redirect.addFlashAttribute("erro", "Você só pode excluir as avaliações que você registrou.");
        }
        return "redirect:/";
    }
}
