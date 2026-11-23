package com.example.sistema.academia.controller;

import com.example.sistema.academia.service.SituacaoService;
import com.example.sistema.academia.service.TrancamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Trancar e destrancar matrícula. Rotas /matriculas/** não casam com nenhuma regra específica: só o ADMIN entra (visitante só GET). */
@Controller
public class TrancamentoController {

    @Autowired private TrancamentoService trancamentoService;
    @Autowired private SituacaoService situacaoService;

    @PostMapping("/matriculas/trancar/{id}")
    public String trancar(@PathVariable Long id,
                          @RequestParam(required = false) String inicio,
                          @RequestParam(required = false) String retorno,
                          @RequestParam(required = false) String motivo,
                          RedirectAttributes redirect) {
        LocalDate hoje = LocalDate.now();
        LocalDate ini;
        LocalDate ret;
        try {
            ini = data(inicio);
            ret = data(retorno);
        } catch (DateTimeParseException e) {
            redirect.addFlashAttribute("erro", "Data inválida no trancamento.");
            return "redirect:/";
        }
        situacaoService.atualizar(); // marca atrasos antes de decidir se pode trancar
        TrancamentoService.Resultado r = trancamentoService.trancarPorId(id, ini, ret, motivo, hoje);
        redirect.addFlashAttribute(r.ok() ? "sucesso" : "erro", r.ok() ? r.sucesso() : r.erro());
        return "redirect:/";
    }

    @PostMapping("/matriculas/destrancar/{id}")
    public String destrancar(@PathVariable Long id, RedirectAttributes redirect) {
        TrancamentoService.Resultado r = trancamentoService.destrancarPorId(id, LocalDate.now());
        redirect.addFlashAttribute(r.ok() ? "sucesso" : "erro", r.ok() ? r.sucesso() : r.erro());
        return "redirect:/";
    }

    private static LocalDate data(String s) {
        return s == null || s.isBlank() ? null : LocalDate.parse(s.trim());
    }
}
