package com.example.sistema.academia.controller;

import com.example.sistema.academia.dto.Contato;
import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.Mensagem;
import com.example.sistema.academia.model.Professor;
import com.example.sistema.academia.repository.AlunoRepository;
import com.example.sistema.academia.repository.MensagemRepository;
import com.example.sistema.academia.repository.ProfessorRepository;
import com.example.sistema.academia.service.FichaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
public class MensagemController {

    @Autowired private AlunoRepository alunoRepository;
    @Autowired private ProfessorRepository professorRepository;
    @Autowired private MensagemRepository mensagemRepository;
    @Autowired private FichaService fichaService;
    @Autowired private com.example.sistema.academia.service.AlunosDoProfessorService alunosDoProfessor;

    @GetMapping("/mensagens")
    public String pagina(@RequestParam(required = false) Long com, Model model, HttpSession session) {
        String perfil = (String) session.getAttribute("perfil");
        Long id = (Long) session.getAttribute("usuarioId");
        boolean aluno = "ALUNO".equals(perfil);

        List<Contato> contatos = new ArrayList<>();
        if (aluno) {
            for (Professor p : professorRepository.findAll()) {
                long novas = mensagemRepository.countByAlunoIdAndProfessorIdAndRemetenteAndLidaFalse(id, p.getId(), "PROFESSOR");
                String zap = fichaService.linkWhatsapp(p.getContato(), "Olá, professor(a) " + p.getNome() + "! Aqui é " + session.getAttribute("nomeUsuario") + ", aluno(a) da academia.");
                contatos.add(new Contato(p.getId(), p.getNome(), p.getEspecialidade(), novas, zap));
            }
        } else {
            java.util.Set<Long> meus = alunosDoProfessor.ids(id);
            for (Aluno a : alunoRepository.findAllById(meus)) {
                long novas = mensagemRepository.countByAlunoIdAndProfessorIdAndRemetenteAndLidaFalse(a.getId(), id, "ALUNO");
                String zap = fichaService.linkWhatsapp(a.getContato(), "Olá, " + a.getNome() + "! Aqui é o professor " + session.getAttribute("nomeUsuario") + ".");
                contatos.add(new Contato(a.getId(), a.getNome(), a.getEmail(), novas, zap));
            }
        }

        Contato selecionado = null;
        List<Mensagem> mensagens = new ArrayList<>();
        if (com != null) {
            selecionado = contatos.stream().filter(c -> c.getId().equals(com)).findFirst().orElse(null);
        }
        if (selecionado != null) {
            Long alunoId = aluno ? id : com;
            Long professorId = aluno ? com : id;
            mensagens = mensagemRepository.findByAlunoIdAndProfessorIdOrderByDataHoraAscIdAsc(alunoId, professorId);
            String remetenteOposto = aluno ? "PROFESSOR" : "ALUNO";
            boolean visitante = Boolean.TRUE.equals(session.getAttribute("visitante"));
            List<Mensagem> paraMarcar = new ArrayList<>();
            // visitante só olha: não marca nada como lido
            if (!visitante) {
                for (Mensagem m : mensagens) {
                    if (!m.isLida() && remetenteOposto.equals(m.getRemetente())) {
                        m.setLida(true);
                        paraMarcar.add(m);
                    }
                }
                mensagemRepository.saveAll(paraMarcar);
            }
            selecionado = new Contato(selecionado.getId(), selecionado.getNome(), selecionado.getDetalhe(), 0, selecionado.getWhatsapp());
            final Long idSelecionado = selecionado.getId();
            contatos = contatos.stream()
                    .map(c -> c.getId().equals(idSelecionado) ? new Contato(c.getId(), c.getNome(), c.getDetalhe(), 0, c.getWhatsapp()) : c)
                    .toList();
        }

        for (Contato c : contatos) {
            List<Mensagem> conversa = aluno
                    ? mensagemRepository.findByAlunoIdAndProfessorIdOrderByDataHoraAscIdAsc(id, c.getId())
                    : mensagemRepository.findByAlunoIdAndProfessorIdOrderByDataHoraAscIdAsc(c.getId(), id);
            if (conversa.isEmpty()) continue;
            Mensagem ultima = conversa.get(conversa.size() - 1);
            String meu = aluno ? "ALUNO" : "PROFESSOR";
            String quem = meu.equals(ultima.getRemetente()) ? "Você: " : "";
            String texto = ultima.getTexto() == null ? "" : ultima.getTexto();
            if (texto.length() > 40) texto = texto.substring(0, 40) + "…";
            c.setUltima(quem + texto + " · " + quando(ultima.getDataHora()));
        }
        model.addAttribute("perfil", perfil);
        model.addAttribute("nomeUsuario", session.getAttribute("nomeUsuario"));
        model.addAttribute("contatos", contatos);
        model.addAttribute("selecionado", selecionado);
        model.addAttribute("mensagens", mensagens);
        model.addAttribute("souAluno", aluno);
        return "mensagens";
    }

    @PostMapping("/mensagens/enviar")
    public String enviar(@RequestParam Long destinoId, @RequestParam String texto, HttpSession session, RedirectAttributes redirect) {
        String perfil = (String) session.getAttribute("perfil");
        Long id = (Long) session.getAttribute("usuarioId");
        boolean aluno = "ALUNO".equals(perfil);

        String limpo = texto.trim();
        if (limpo.isEmpty()) {
            redirect.addFlashAttribute("erro", "Escreva uma mensagem antes de enviar.");
            return "redirect:/mensagens?com=" + destinoId;
        }
        if (limpo.length() > 1000) limpo = limpo.substring(0, 1000);

        Aluno a = alunoRepository.findById(aluno ? id : destinoId).orElse(null);
        Professor p = professorRepository.findById(aluno ? destinoId : id).orElse(null);
        // Professor só escreve para os próprios alunos (ficha dele ou conversa já aberta pelo aluno)
        if (a == null || p == null || (!aluno && !alunosDoProfessor.ids(id).contains(a.getId()))) {
            redirect.addFlashAttribute("erro", "Destinatário não encontrado.");
            return "redirect:/mensagens";
        }

        Mensagem m = new Mensagem();
        m.setAluno(a);
        m.setProfessor(p);
        m.setRemetente(aluno ? "ALUNO" : "PROFESSOR");
        m.setTexto(limpo);
        m.setDataHora(LocalDateTime.now());
        m.setLida(false);
        mensagemRepository.save(m);
        return "redirect:/mensagens?com=" + destinoId;
    }
    private static String quando(java.time.LocalDateTime dataHora) {
        if (dataHora == null) return "";
        long min = java.time.Duration.between(dataHora, java.time.LocalDateTime.now()).toMinutes();
        if (min < 60) return "há " + Math.max(min, 1) + " min";
        if (min < 24 * 60) return "há " + (min / 60) + " h";
        return "há " + (min / (24 * 60)) + " dia(s)";
    }
}