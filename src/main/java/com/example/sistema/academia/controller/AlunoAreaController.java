package com.example.sistema.academia.controller;

import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.ExercicioTreino;
import com.example.sistema.academia.model.Mensalidade;
import com.example.sistema.academia.model.RegistroExercicio;
import com.example.sistema.academia.repository.AlunoRepository;
import com.example.sistema.academia.repository.ExercicioTreinoRepository;
import com.example.sistema.academia.repository.MensalidadeRepository;
import com.example.sistema.academia.repository.RegistroExercicioRepository;
import com.example.sistema.academia.service.EmailService;
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
public class AlunoAreaController {

    @Autowired private AlunoRepository alunoRepository;
    @Autowired private MensalidadeRepository mensalidadeRepository;
    @Autowired private ExercicioTreinoRepository exercicioTreinoRepository;
    @Autowired private RegistroExercicioRepository registroExercicioRepository;
    @Autowired private FichaService fichaService;
    @Autowired private EmailService emailService;

    @PostMapping("/aluno/mensalidades/informar/{id}")
    public String informarPagamento(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        Long alunoId = (Long) session.getAttribute("usuarioId");
        Mensalidade m = mensalidadeRepository.findById(id).orElse(null);
        if (m == null || alunoId == null || !m.getMatricula().getAluno().getId().equals(alunoId)) {
            redirect.addFlashAttribute("erro", "Mensalidade não encontrada.");
        } else if (!"PENDENTE".equals(m.getSituacao()) && !"ATRASADA".equals(m.getSituacao())) {
            redirect.addFlashAttribute("erro", "Essa mensalidade não está disponível para pagamento.");
        } else {
            m.setSituacao("EM_ANALISE");
            mensalidadeRepository.save(m);
            redirect.addFlashAttribute("sucesso", "Pagamento informado! A recepção vai confirmar e a mensalidade fica EM ANÁLISE até lá.");
        }
        return "redirect:/";
    }

    @PostMapping("/aluno/exercicios/registrar")
    public String registrarExecucao(@RequestParam Long exercicioId,
                                    @RequestParam(required = false) Double carga,
                                    @RequestParam(required = false) String observacao,
                                    HttpSession session,
                                    RedirectAttributes redirect) {
        Long alunoId = (Long) session.getAttribute("usuarioId");
        ExercicioTreino ex = exercicioTreinoRepository.findById(exercicioId).orElse(null);
        Aluno aluno = alunoId != null ? alunoRepository.findById(alunoId).orElse(null) : null;
        if (ex == null || aluno == null || !ex.getTreino().getAluno().getId().equals(alunoId)) {
            redirect.addFlashAttribute("erro", "Exercício não encontrado.");
            return "redirect:/";
        }
        if (carga != null && (carga < 0 || carga > 1000)) {
            redirect.addFlashAttribute("erro", "Informe uma carga entre 0 e 1000 kg.");
            return "redirect:/";
        }
        RegistroExercicio r = new RegistroExercicio();
        r.setExercicio(ex);
        r.setAluno(aluno);
        r.setDataRegistro(LocalDate.now());
        r.setCarga(carga);
        if (observacao != null && !observacao.isBlank()) {
            String limpa = observacao.trim();
            r.setObservacao(limpa.length() > 200 ? limpa.substring(0, 200) : limpa);
        }
        registroExercicioRepository.save(r);
        redirect.addFlashAttribute("sucesso", "Exercício registrado! Bom treino.");
        return "redirect:/";
    }

    @PostMapping("/aluno/ficha/email")
    public String enviarMinhaFicha(HttpSession session, RedirectAttributes redirect) {
        Long alunoId = (Long) session.getAttribute("usuarioId");
        Aluno aluno = alunoId != null ? alunoRepository.findById(alunoId).orElse(null) : null;
        if (aluno == null) {
            return "redirect:/";
        }
        if (!emailService.configurado()) {
            redirect.addFlashAttribute("erro", "O envio de e-mail ainda não foi configurado. Use o botão do WhatsApp.");
        } else if (aluno.getEmail() == null || aluno.getEmail().isBlank()) {
            redirect.addFlashAttribute("erro", "Você não tem e-mail cadastrado.");
        } else {
            try {
                emailService.enviar(aluno.getEmail(), "GymSync - sua ficha de treino", fichaService.textoAluno(aluno) + "\nBom treino!\nGymSync");
                redirect.addFlashAttribute("sucesso", "Ficha enviada para " + aluno.getEmail() + ".");
            } catch (Exception e) {
                redirect.addFlashAttribute("erro", "Não foi possível enviar o e-mail agora.");
            }
        }
        return "redirect:/";
    }
}
