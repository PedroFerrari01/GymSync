package com.example.sistema.academia.controller;

import com.example.sistema.academia.service.MarcaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Identidade da academia: cor de destaque, nome e logo. Só o admin altera; a logo e o CSS são públicos (a tela de login usa). */
@Controller
public class MarcaController {

    @Autowired private MarcaService marcaService;

    /** CSS com as variáveis de destaque. Vazio quando não há cor escolhida. */
    @GetMapping("/marca/tema.css")
    public ResponseEntity<byte[]> tema() {
        var marca = marcaService.marca();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/css;charset=UTF-8"))
                .cacheControl(CacheControl.noCache())
                .eTag("\"" + marca.versao() + "\"")
                .body(marca.css().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/marca/logo")
    public ResponseEntity<byte[]> logo() {
        var logo = marcaService.logo();
        if (logo == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.tipo()))
                .cacheControl(CacheControl.noCache())
                .eTag("\"" + logo.etag() + "\"")
                .body(logo.dados());
    }

    @PostMapping("/marca/salvar")
    public String salvar(@RequestParam(required = false) String cor,
                         @RequestParam(required = false) String corPersonalizada,
                         @RequestParam(required = false) String nome,
                         @RequestParam(required = false) MultipartFile logo,
                         RedirectAttributes redirect) throws IOException {
        // Se o admin digitou uma cor própria, ela vale; senão vale a predefinição marcada.
        String escolhida = corPersonalizada != null && !corPersonalizada.isBlank() ? corPersonalizada : cor;
        byte[] bytes = logo != null && !logo.isEmpty() ? logo.getBytes() : null;
        String erro = marcaService.salvar(escolhida, nome, bytes);
        if (erro != null) redirect.addFlashAttribute("erro", erro);
        else redirect.addFlashAttribute("sucesso", "Identidade da academia atualizada.");
        return "redirect:/";
    }

    @PostMapping("/marca/restaurar")
    public String restaurar(RedirectAttributes redirect) {
        marcaService.restaurarPadrao();
        redirect.addFlashAttribute("sucesso", "Identidade restaurada: cor e logo padrão do GymSync.");
        return "redirect:/";
    }
}
