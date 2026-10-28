package com.example.sistema.academia.controller;

import com.example.sistema.academia.model.Equipamento;
import com.example.sistema.academia.model.Manutencao;
import com.example.sistema.academia.repository.EquipamentoRepository;
import com.example.sistema.academia.repository.ManutencaoRepository;
import com.example.sistema.academia.service.ExclusaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
public class EquipamentoController {

    @Autowired private EquipamentoRepository equipamentoRepository;
    @Autowired private ManutencaoRepository manutencaoRepository;
    @Autowired private ExclusaoService exclusaoService;

    @PostMapping("/equipamentos/salvar")
    public String salvar(@RequestParam String nome,
                         @RequestParam(required = false) String tipo,
                         @RequestParam(required = false) String codigo,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAquisicao,
                         RedirectAttributes redirect) {
        if (nome.isBlank()) {
            redirect.addFlashAttribute("erro", "Informe o nome do equipamento.");
            return "redirect:/";
        }
        Equipamento e = new Equipamento();
        e.setNome(nome.trim());
        e.setTipo(tipo != null ? tipo.trim() : null);
        e.setCodigo(codigo != null ? codigo.trim() : null);
        e.setDataAquisicao(dataAquisicao);
        equipamentoRepository.save(e);
        redirect.addFlashAttribute("sucesso", "Equipamento cadastrado!");
        return "redirect:/";
    }

    @PostMapping("/equipamentos/status/{id}")
    public String alternarStatus(@PathVariable Long id) {
        Equipamento e = equipamentoRepository.findById(id).orElse(null);
        if (e != null) {
            e.setSituacao("OPERANDO".equals(e.getSituacao()) ? "EM_MANUTENCAO" : "OPERANDO");
            equipamentoRepository.save(e);
        }
        return "redirect:/";
    }

    @PostMapping("/equipamentos/excluir/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        exclusaoService.excluirEquipamento(id);
        redirect.addFlashAttribute("sucesso", "Equipamento e histórico de manutenções excluídos.");
        return "redirect:/";
    }

    @PostMapping("/equipamentos/manutencao")
    public String registrarManutencao(@RequestParam Long equipamentoId,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataManutencao,
                                      @RequestParam String tipo,
                                      @RequestParam String descricao,
                                      @RequestParam(required = false) Double custo,
                                      @RequestParam(required = false) String responsavel,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate proximaManutencao,
                                      @RequestParam(required = false) String continuaEmManutencao,
                                      RedirectAttributes redirect) {
        Equipamento e = equipamentoRepository.findById(equipamentoId).orElse(null);
        if (e == null || descricao.isBlank() || dataManutencao.isAfter(LocalDate.now()) || (custo != null && custo < 0)) {
            redirect.addFlashAttribute("erro", "Confira os dados da manutenção.");
            return "redirect:/";
        }
        Manutencao m = new Manutencao();
        m.setEquipamento(e);
        m.setDataManutencao(dataManutencao);
        m.setTipo(tipo);
        m.setDescricao(descricao.trim().length() > 300 ? descricao.trim().substring(0, 300) : descricao.trim());
        m.setCusto(custo);
        m.setResponsavel(responsavel != null ? responsavel.trim() : null);
        manutencaoRepository.save(m);

        e.setProximaManutencao(proximaManutencao);
        e.setSituacao(continuaEmManutencao != null ? "EM_MANUTENCAO" : "OPERANDO");
        equipamentoRepository.save(e);
        redirect.addFlashAttribute("sucesso", "Manutenção registrada para " + e.getNome() + ".");
        return "redirect:/";
    }

    @PostMapping("/equipamentos/manutencao/excluir/{id}")
    public String excluirManutencao(@PathVariable Long id) {
        manutencaoRepository.deleteById(id);
        return "redirect:/";
    }
}
