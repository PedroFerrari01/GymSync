package com.example.sistema.academia.controller;

import com.example.sistema.academia.model.*;
import com.example.sistema.academia.repository.*;
import com.example.sistema.academia.service.EmailService;
import com.example.sistema.academia.service.ExclusaoService;
import com.example.sistema.academia.service.FichaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
public class TreinoController {

    @Autowired private AlunoRepository alunoRepository;
    @Autowired private ProfessorRepository professorRepository;
    @Autowired private TreinoRepository treinoRepository;
    @Autowired private ExercicioTreinoRepository exercicioTreinoRepository;
    @Autowired private ExclusaoService exclusaoService;
    @Autowired private FichaService fichaService;
    @Autowired private EmailService emailService;

    private boolean podeEditar(HttpSession session, Professor dono) {
        String perfil = (String) session.getAttribute("perfil");
        if ("ADMIN".equals(perfil)) return true;
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        return "PROFESSOR".equals(perfil) && usuarioId != null && dono != null && usuarioId.equals(dono.getId());
    }

    @PostMapping("/treinos/salvar")
    public String salvarTreino(@RequestParam Long alunoId,
                               @RequestParam(required = false) Long professorId,
                               @RequestParam String descricao,
                               HttpSession session,
                               RedirectAttributes redirect) {
        if ("PROFESSOR".equals(session.getAttribute("perfil"))) {
            professorId = (Long) session.getAttribute("usuarioId");
        }
        Aluno a = alunoRepository.findById(alunoId).orElse(null);
        Professor prof = professorId != null ? professorRepository.findById(professorId).orElse(null) : null;
        if (a == null || prof == null || descricao.isBlank()) {
            redirect.addFlashAttribute("erro", "Selecione o aluno, o professor e informe o nome do treino.");
            return "redirect:/";
        }
        Treino t = new Treino();
        t.setAluno(a);
        t.setProfessor(prof);
        t.setDescricao(descricao.trim());
        t.setDataCriacao(LocalDate.now());
        treinoRepository.save(t);
        redirect.addFlashAttribute("sucesso", "Ficha de treino criada!");
        return "redirect:/";
    }

    @PostMapping("/treinos/editar")
    public String editarTreino(@RequestParam Long id, @RequestParam String descricao, HttpSession session, RedirectAttributes redirect) {
        Treino t = treinoRepository.findById(id).orElse(null);
        if (t == null || !podeEditar(session, t.getProfessor())) {
            redirect.addFlashAttribute("erro", "Você não tem permissão para alterar esta ficha.");
            return "redirect:/";
        }
        if (descricao.isBlank()) {
            redirect.addFlashAttribute("erro", "O nome do treino não pode ficar vazio.");
            return "redirect:/";
        }
        t.setDescricao(descricao.trim());
        treinoRepository.save(t);
        redirect.addFlashAttribute("sucesso", "Ficha de treino atualizada!");
        return "redirect:/";
    }

    @PostMapping("/treinos/excluir/{id}")
    public String excluirTreino(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        Treino t = treinoRepository.findById(id).orElse(null);
        if (t == null || !podeEditar(session, t.getProfessor())) {
            redirect.addFlashAttribute("erro", "Você não tem permissão para excluir esta ficha.");
            return "redirect:/";
        }
        exclusaoService.excluirTreino(id);
        redirect.addFlashAttribute("sucesso", "Ficha de treino excluída.");
        return "redirect:/";
    }

    @PostMapping("/treinos/enviar-email/{id}")
    public String enviarFichaPorEmail(@PathVariable Long id, RedirectAttributes redirect) {
        Treino t = treinoRepository.findById(id).orElse(null);
        if (t == null) {
            return "redirect:/";
        }
        String destino = t.getAluno().getEmail();
        if (!emailService.configurado()) {
            redirect.addFlashAttribute("erro", "O envio de e-mail ainda não foi configurado no application.properties.");
        } else if (destino == null || destino.isBlank()) {
            redirect.addFlashAttribute("erro", "O aluno não tem e-mail cadastrado.");
        } else {
            try {
                String texto = "Olá, " + t.getAluno().getNome() + "!\n\nSegue a sua ficha de treino:\n\n"
                        + fichaService.textoTreino(t) + "\nBom treino!\nGymSync";
                emailService.enviar(destino, "GymSync - sua ficha de treino", texto);
                redirect.addFlashAttribute("sucesso", "Ficha enviada para " + destino + ".");
            } catch (Exception e) {
                redirect.addFlashAttribute("erro", "Não foi possível enviar o e-mail. Confira a configuração do Gmail.");
            }
        }
        return "redirect:/";
    }

    @PostMapping("/exercicios/salvar")
    public String salvarExercicio(@RequestParam Long treinoId,
                                  @RequestParam String exercicio,
                                  @RequestParam Integer series,
                                  @RequestParam Integer repeticoes,
                                  HttpSession session,
                                  RedirectAttributes redirect) {
        Treino t = treinoRepository.findById(treinoId).orElse(null);
        if (t == null || !podeEditar(session, t.getProfessor())) {
            redirect.addFlashAttribute("erro", "Você não tem permissão para alterar esta ficha.");
            return "redirect:/";
        }
        ExercicioTreino ex = new ExercicioTreino();
        ex.setTreino(t);
        ex.setExercicio(exercicio.trim());
        ex.setSeries(series);
        ex.setRepeticoes(repeticoes);
        exercicioTreinoRepository.save(ex);
        redirect.addFlashAttribute("sucesso", "Exercício adicionado à ficha!");
        return "redirect:/";
    }

    @PostMapping("/exercicios/editar")
    public String editarExercicio(@RequestParam Long id,
                                  @RequestParam String exercicio,
                                  @RequestParam Integer series,
                                  @RequestParam Integer repeticoes,
                                  HttpSession session,
                                  RedirectAttributes redirect) {
        ExercicioTreino ex = exercicioTreinoRepository.findById(id).orElse(null);
        if (ex == null || !podeEditar(session, ex.getTreino().getProfessor())) {
            redirect.addFlashAttribute("erro", "Você não tem permissão para alterar este exercício.");
            return "redirect:/";
        }
        if (exercicio.isBlank() || series < 1 || repeticoes < 1) {
            redirect.addFlashAttribute("erro", "Preencha o exercício com séries e repetições válidas.");
            return "redirect:/";
        }
        ex.setExercicio(exercicio.trim());
        ex.setSeries(series);
        ex.setRepeticoes(repeticoes);
        exercicioTreinoRepository.save(ex);
        redirect.addFlashAttribute("sucesso", "Exercício atualizado!");
        return "redirect:/";
    }

    @PostMapping("/exercicios/excluir/{id}")
    public String excluirExercicio(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        ExercicioTreino ex = exercicioTreinoRepository.findById(id).orElse(null);
        if (ex == null || !podeEditar(session, ex.getTreino().getProfessor())) {
            redirect.addFlashAttribute("erro", "Você não tem permissão para excluir este exercício.");
            return "redirect:/";
        }
        exclusaoService.excluirExercicio(id);
        return "redirect:/";
    }
}
