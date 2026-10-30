package com.example.sistema.academia.controller;

import com.example.sistema.academia.repository.*;
import com.example.sistema.academia.service.ExportacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Exportar relatórios em planilha (CSV). Só o administrador (rota sem regra própria em RegrasAcesso). */
@Controller
public class RelatorioController {

    @Autowired private AlunoRepository alunoRepository;
    @Autowired private MatriculaRepository matriculaRepository;
    @Autowired private MensalidadeRepository mensalidadeRepository;
    @Autowired private EntradaRepository entradaRepository;
    @Autowired private ExportacaoService exportacaoService;

    @GetMapping("/relatorios/exportar/{tipo}")
    public ResponseEntity<byte[]> exportar(@PathVariable String tipo) {
        LocalDate hoje = LocalDate.now();
        String conteudo = switch (tipo) {
            case "alunos" -> exportacaoService.alunos(alunoRepository.findAll(), matriculaRepository.findAll());
            case "mensalidades" -> exportacaoService.mensalidades(mensalidadeRepository.findAll());
            case "entradas" -> exportacaoService.entradas(
                    entradaRepository.findByDataHoraEntradaAfter(hoje.minusDays(90).atStartOfDay()).stream()
                            .sorted((a, b) -> b.getDataHoraEntrada().compareTo(a.getDataHoraEntrada())).toList());
            case "resumo-mensal" -> exportacaoService.resumoMensal(hoje,
                    entradaRepository.findByDataHoraEntradaAfter(hoje.withDayOfMonth(1).minusMonths(11).atStartOfDay().minusSeconds(1)),
                    matriculaRepository.findAll(), mensalidadeRepository.findAll());
            default -> null;
        };
        if (conteudo == null) return ResponseEntity.notFound().build();

        String arquivo = "gymsync-" + tipo + "-" + hoje.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(arquivo).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(conteudo.getBytes(StandardCharsets.UTF_8));
    }
}
