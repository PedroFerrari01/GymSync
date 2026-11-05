package com.example.sistema.academia.controller;

import com.example.sistema.academia.model.Equipamento;
import com.example.sistema.academia.model.EquipamentoImagem;
import com.example.sistema.academia.model.Manutencao;
import com.example.sistema.academia.repository.EquipamentoImagemRepository;
import com.example.sistema.academia.repository.EquipamentoRepository;
import com.example.sistema.academia.repository.ManutencaoRepository;
import com.example.sistema.academia.service.ExclusaoService;
import com.example.sistema.academia.util.ImagemValidador;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;
import java.time.LocalDate;

@Controller
public class EquipamentoController {

    @Autowired private EquipamentoRepository equipamentoRepository;
    @Autowired private EquipamentoImagemRepository imagemRepository;
    @Autowired private ManutencaoRepository manutencaoRepository;
    @Autowired private ExclusaoService exclusaoService;

    @PostMapping("/equipamentos/salvar")
    public String salvar(@RequestParam String nome,
                         @RequestParam(required = false) String tipo,
                         @RequestParam(required = false) String codigo,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAquisicao,
                         @RequestParam(required = false) MultipartFile imagem,
                         RedirectAttributes redirect) throws IOException {
        if (nome.isBlank()) {
            redirect.addFlashAttribute("erro", "Informe o nome do equipamento.");
            return "redirect:/";
        }
        boolean temImagem = imagem != null && !imagem.isEmpty();
        String erroImagem = temImagem ? validarImagem(imagem) : null;
        if (erroImagem != null) {
            redirect.addFlashAttribute("erro", erroImagem);
            return "redirect:/";
        }
        Equipamento e = new Equipamento();
        e.setNome(nome.trim());
        e.setTipo(tipo != null ? tipo.trim() : null);
        e.setCodigo(codigo != null ? codigo.trim() : null);
        e.setDataAquisicao(dataAquisicao);
        equipamentoRepository.save(e);
        if (temImagem) guardarImagem(e.getId(), imagem);
        redirect.addFlashAttribute("sucesso", "Equipamento cadastrado!");
        return "redirect:/";
    }

    /** Anexa ou troca a foto de um aparelho que já está cadastrado. */
    @PostMapping("/equipamentos/imagem/{id}")
    public String anexarImagem(@PathVariable Long id,
                               @RequestParam(required = false) MultipartFile imagem,
                               RedirectAttributes redirect) throws IOException {
        Equipamento e = equipamentoRepository.findById(id).orElse(null);
        if (e == null) {
            redirect.addFlashAttribute("erro", "Equipamento não encontrado.");
            return "redirect:/";
        }
        if (imagem == null || imagem.isEmpty()) {
            redirect.addFlashAttribute("erro", "Escolha uma imagem do aparelho.");
            return "redirect:/";
        }
        String erro = validarImagem(imagem);
        if (erro != null) {
            redirect.addFlashAttribute("erro", erro);
            return "redirect:/";
        }
        guardarImagem(id, imagem);
        redirect.addFlashAttribute("sucesso", "Imagem de " + e.getNome() + " salva.");
        return "redirect:/";
    }

    @GetMapping("/equipamentos/imagem/{id}")
    public ResponseEntity<byte[]> verImagem(@PathVariable Long id) {
        return imagemRepository.findById(id)
                .map(i -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(i.getTipo()))
                        .cacheControl(CacheControl.noCache())
                        .body(i.getDados()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private String validarImagem(MultipartFile imagem) throws IOException {
        if (imagem.getSize() > ImagemValidador.TAMANHO_MAXIMO) {
            return "A imagem deve ter no máximo 2 MB.";
        }
        if (ImagemValidador.tipo(imagem.getBytes()) == null) {
            return "Envie a imagem do aparelho em JPG ou PNG.";
        }
        return null;
    }

    private void guardarImagem(Long equipamentoId, MultipartFile imagem) throws IOException {
        byte[] dados = imagem.getBytes();
        imagemRepository.save(new EquipamentoImagem(equipamentoId, ImagemValidador.tipo(dados), dados));
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
