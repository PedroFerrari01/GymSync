package com.example.sistema.academia.controller;

import com.example.sistema.academia.service.MuralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

/** Mural da academia: só o administrador publica e encerra (rotas /mural/** caem na regra padrão: só admin). */
@Controller
public class MuralController {

    @Autowired private MuralService muralService;

    @PostMapping("/mural/publicar")
    public String publicar(@RequestParam String titulo, @RequestParam String texto,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataTermino,
                           RedirectAttributes redirect) {
        LocalDate hoje = LocalDate.now();
        String erro = MuralService.validar(titulo, texto, dataTermino, hoje);
        if (erro != null) {
            redirect.addFlashAttribute("erro", erro);
            return "redirect:/";
        }
        muralService.publicar(titulo, texto, dataTermino, hoje);
        redirect.addFlashAttribute("sucesso", "Aviso publicado no mural!");
        return "redirect:/";
    }

    @PostMapping("/mural/encerrar/{id}")
    public String encerrar(@PathVariable Long id, RedirectAttributes redirect) {
        if (muralService.encerrar(id)) redirect.addFlashAttribute("sucesso", "Aviso encerrado: saiu do mural.");
        else redirect.addFlashAttribute("erro", "Aviso não encontrado.");
        return "redirect:/";
    }
}
