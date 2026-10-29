package com.example.sistema.academia.controller;

import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.Professor;
import com.example.sistema.academia.repository.AlunoRepository;
import com.example.sistema.academia.repository.ProfessorRepository;
import com.example.sistema.academia.service.LimiteTentativasService;
import com.example.sistema.academia.service.SenhaService;
import jakarta.annotation.PostConstruct;
import com.example.sistema.academia.config.SessaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired private AlunoRepository alunoRepository;
    @Autowired private ProfessorRepository professorRepository;
    @Autowired private PasswordEncoder encoder;
    @Autowired private SessaoService sessaoService;
    @Autowired private LimiteTentativasService limite;
    @Autowired private SenhaService senhaService;

    @Value("${gymsync.admin.usuario:admin}") private String adminUsuario;
    @Value("${gymsync.admin.senha:123}") private String adminSenha;
    @Value("${gymsync.login.max-falhas:5}") private int maxFalhasConta;
    @Value("${gymsync.login.max-falhas-ip:30}") private int maxFalhasIp;

    /** No modo de senhas fortes, o sistema não sobe com a senha de administrador fraca. */
    @PostConstruct
    void conferirSenhaAdmin() {
        if (senhaService.fortes() && !adminSenha.startsWith("$2") && senhaService.validarNova(adminSenha) != null) {
            throw new IllegalStateException("gymsync.senhas-fortes=true exige gymsync.admin.senha com pelo menos "
                    + senhaService.tamanhoMinimo() + " caracteres (ou um hash BCrypt).");
        }
    }

    private boolean senhaConfere(String salva, String digitada) {
        if (salva == null || salva.isBlank()) return senhaService.aceitaSenhaVaziaComo123() && "123".equals(digitada);
        if (salva.startsWith("$2")) return encoder.matches(digitada, salva);
        return salva.equals(digitada);
    }

    private boolean precisaMigrar(String salva) {
        return salva == null || salva.isBlank() || !salva.startsWith("$2");
    }

    private void registrarFalha(String conta, String ip) {
        limite.registrarFalha(conta, maxFalhasConta);
        limite.registrarFalha(ip, maxFalhasIp); // mais folgado: vários alunos podem dividir o mesmo Wi-Fi
    }

    @PostMapping("/login")
    public String realizarLogin(@RequestParam String tipo,
                                @RequestParam(required = false) String usuario,
                                @RequestParam(required = false) String identificador,
                                @RequestParam String senha,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                RedirectAttributes redirect) {
        String quem = "ADMIN".equals(tipo) ? usuario : identificador;
        String conta = "conta:" + tipo + ":" + (quem == null ? "" : quem.trim().toLowerCase());
        String ip = "ip:" + request.getRemoteAddr();
        long espera = Math.max(limite.minutosRestantes(conta), limite.minutosRestantes(ip));
        if (espera > 0) {
            redirect.addFlashAttribute("erroLogin", "Muitas tentativas incorretas. Tente de novo em " + espera + " minuto(s).");
            return "redirect:/";
        }

        if ("ADMIN".equals(tipo)) {
            if (adminUsuario.equalsIgnoreCase(usuario) && senhaConfere(adminSenha, senha)) {
                limite.registrarSucesso(conta);
                sessaoService.iniciar(request, response, "ADMIN", "Administrador", null);
                return "redirect:/";
            }
            registrarFalha(conta, ip);
            redirect.addFlashAttribute("erroLogin", "Usuário ou senha de administrador incorretos.");
            return "redirect:/";
        }

        String idLimpo = identificador != null ? identificador.trim() : "";

        if ("PROFESSOR".equals(tipo)) {
            Professor prof = professorRepository.findFirstByCpfOrEmailIgnoreCase(idLimpo, idLimpo).orElse(null);
            if (prof != null && senhaConfere(prof.getSenha(), senha)) {
                if (precisaMigrar(prof.getSenha())) {
                    prof.setSenha(encoder.encode(senha));
                    professorRepository.save(prof);
                }
                limite.registrarSucesso(conta);
                sessaoService.iniciar(request, response, "PROFESSOR", prof.getNome(), prof.getId());
                if (prof.isTrocarSenha()) request.getSession().setAttribute("trocarSenha", true);
                return "redirect:/";
            }
            registrarFalha(conta, ip);
            redirect.addFlashAttribute("erroLogin", "CPF/E-mail ou senha de professor incorretos.");
            return "redirect:/";
        }

        if ("ALUNO".equals(tipo)) {
            Aluno aluno = alunoRepository.findFirstByCpfOrEmailIgnoreCase(idLimpo, idLimpo).orElse(null);
            if (aluno != null && senhaConfere(aluno.getSenha(), senha)) {
                if (precisaMigrar(aluno.getSenha())) {
                    aluno.setSenha(encoder.encode(senha));
                    alunoRepository.save(aluno);
                }
                limite.registrarSucesso(conta);
                sessaoService.iniciar(request, response, "ALUNO", aluno.getNome(), aluno.getId());
                if (aluno.isTrocarSenha()) request.getSession().setAttribute("trocarSenha", true);
                return "redirect:/";
            }
            registrarFalha(conta, ip);
            redirect.addFlashAttribute("erroLogin", "CPF/E-mail ou senha de aluno incorretos.");
        }
        return "redirect:/";
    }

    @PostMapping("/recuperar-senha")
    public String recuperarSenha(@RequestParam String tipo,
                                 @RequestParam String cpf,
                                 @RequestParam String email,
                                 @RequestParam String novaSenha,
                                 RedirectAttributes redirect) {
        String erroSenha = senhaService.validarNova(novaSenha);
        if (erroSenha != null) {
            redirect.addFlashAttribute("erroLogin", erroSenha);
            return "redirect:/";
        }

        if ("ALUNO".equals(tipo)) {
            Aluno aluno = alunoRepository.findFirstByCpfAndEmailIgnoreCase(cpf.trim(), email.trim()).orElse(null);
            if (aluno != null) {
                aluno.setSenha(encoder.encode(novaSenha));
                aluno.setTrocarSenha(false);
                alunoRepository.save(aluno);
                redirect.addFlashAttribute("sucessoLogin", "Senha definida com sucesso! Faça seu login abaixo.");
                return "redirect:/";
            }
        } else if ("PROFESSOR".equals(tipo)) {
            Professor prof = professorRepository.findFirstByCpfAndEmailIgnoreCase(cpf.trim(), email.trim()).orElse(null);
            if (prof != null) {
                prof.setSenha(encoder.encode(novaSenha));
                prof.setTrocarSenha(false);
                professorRepository.save(prof);
                redirect.addFlashAttribute("sucessoLogin", "Senha definida com sucesso! Faça seu login abaixo.");
                return "redirect:/";
            }
        }
        redirect.addFlashAttribute("erroLogin", "Dados não conferem: verifique o Perfil, CPF e E-mail informados.");
        return "redirect:/";
    }

    @PostMapping("/alterar-senha")
    public String alterarMinhaSenha(@RequestParam String novaSenha, HttpSession session, RedirectAttributes redirect) {
        String erroSenha = senhaService.validarNova(novaSenha);
        if (erroSenha != null) {
            redirect.addFlashAttribute("erro", erroSenha);
            return "redirect:/";
        }

        String perfil = (String) session.getAttribute("perfil");
        Long usuarioId = (Long) session.getAttribute("usuarioId");

        if ("ALUNO".equals(perfil) && usuarioId != null) {
            Aluno a = alunoRepository.findById(usuarioId).orElse(null);
            if (a != null) {
                a.setSenha(encoder.encode(novaSenha));
                a.setTrocarSenha(false);
                alunoRepository.save(a);
                session.removeAttribute("trocarSenha");
                redirect.addFlashAttribute("sucesso", "Sua senha foi atualizada com sucesso!");
            }
        } else if ("PROFESSOR".equals(perfil) && usuarioId != null) {
            Professor p = professorRepository.findById(usuarioId).orElse(null);
            if (p != null) {
                p.setSenha(encoder.encode(novaSenha));
                p.setTrocarSenha(false);
                professorRepository.save(p);
                session.removeAttribute("trocarSenha");
                redirect.addFlashAttribute("sucesso", "Sua senha foi atualizada com sucesso!");
            }
        }
        return "redirect:/";
    }
}
