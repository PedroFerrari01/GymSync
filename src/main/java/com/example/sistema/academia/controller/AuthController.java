package com.example.sistema.academia.controller;

import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.Professor;
import com.example.sistema.academia.repository.AlunoRepository;
import com.example.sistema.academia.repository.ProfessorRepository;
import com.example.sistema.academia.service.LimiteTentativasService;
import com.example.sistema.academia.service.RecuperacaoSenhaService;
import com.example.sistema.academia.service.SenhaService;
import com.example.sistema.academia.service.VisitanteService;
import jakarta.annotation.PostConstruct;
import com.example.sistema.academia.config.SessaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired private AlunoRepository alunoRepository;
    @Autowired private ProfessorRepository professorRepository;
    @Autowired private PasswordEncoder encoder;
    @Autowired private SessaoService sessaoService;
    @Autowired private LimiteTentativasService limite;
    @Autowired private SenhaService senhaService;
    @Autowired private RecuperacaoSenhaService recuperacao;
    @Autowired private VisitanteService visitanteService;

    @Value("${gymsync.admin.usuario:admin}") private String adminUsuario;
    @Value("${gymsync.admin.senha:123}") private String adminSenha;
    @Value("${gymsync.login.max-falhas:5}") private int maxFalhasConta;
    @Value("${gymsync.login.max-falhas-ip:30}") private int maxFalhasIp;
    @Value("${gymsync.recuperacao-por-email:false}") private boolean recuperacaoPorEmail;

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

    /** Para o log: mostra só o começo do e-mail ou os 2 últimos dígitos do CPF (dado pessoal não vai inteiro para o log). */
    static String mascarar(String quem) {
        if (quem == null || quem.isBlank()) return "(vazio)";
        String q = quem.trim();
        int arroba = q.indexOf('@');
        if (arroba > 0) return q.substring(0, Math.min(2, arroba)) + "***" + q.substring(arroba);
        String digitos = q.replaceAll("\\D", "");
        if (digitos.length() >= 2) return "CPF ***" + digitos.substring(digitos.length() - 2);
        return "***";
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
            log.warn("Login bloqueado por excesso de tentativas: perfil={} conta={} ip={}", tipo, mascarar(quem), request.getRemoteAddr());
            redirect.addFlashAttribute("erroLogin", "Muitas tentativas incorretas. Tente de novo em " + espera + " minuto(s).");
            return "redirect:/";
        }

        if ("ADMIN".equals(tipo)) {
            if (adminUsuario.equalsIgnoreCase(usuario) && senhaConfere(adminSenha, senha)) {
                limite.registrarSucesso(conta);
                log.info("Login: perfil={} conta={} ip={}", tipo, mascarar(quem), request.getRemoteAddr());
                sessaoService.iniciar(request, response, "ADMIN", "Administrador", null);
                return "redirect:/";
            }
            registrarFalha(conta, ip);
            log.warn("Login recusado: perfil={} conta={} ip={}", tipo, mascarar(quem), request.getRemoteAddr());
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
                log.info("Login: perfil={} conta={} ip={}", tipo, mascarar(quem), request.getRemoteAddr());
                sessaoService.iniciar(request, response, "PROFESSOR", prof.getNome(), prof.getId());
                if (prof.isTrocarSenha()) request.getSession().setAttribute("trocarSenha", true);
                return "redirect:/";
            }
            registrarFalha(conta, ip);
            log.warn("Login recusado: perfil={} conta={} ip={}", tipo, mascarar(quem), request.getRemoteAddr());
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
                log.info("Login: perfil={} conta={} ip={}", tipo, mascarar(quem), request.getRemoteAddr());
                sessaoService.iniciar(request, response, "ALUNO", aluno.getNome(), aluno.getId());
                if (aluno.isTrocarSenha()) request.getSession().setAttribute("trocarSenha", true);
                return "redirect:/";
            }
            registrarFalha(conta, ip);
            log.warn("Login recusado: perfil={} conta={} ip={}", tipo, mascarar(quem), request.getRemoteAddr());
            redirect.addFlashAttribute("erroLogin", "CPF/E-mail ou senha de aluno incorretos.");
        }
        return "redirect:/";
    }

    /** Entra só para olhar, com o perfil escolhido no login e os dados de exemplo. Nada pode ser salvo. */
    @PostMapping("/visitante")
    public String entrarComoVisitante(@RequestParam(defaultValue = "ADMIN") String tipo,
                                      HttpServletRequest request,
                                      HttpServletResponse response,
                                      RedirectAttributes redirect) {
        if (!visitanteService.habilitado()) return "redirect:/";
        if ("ALUNO".equals(tipo)) {
            Aluno aluno = visitanteService.alunoExemplo().orElse(null);
            if (aluno == null) return semDadosDeExemplo(redirect);
            sessaoService.iniciar(request, response, "ALUNO", aluno.getNome(), aluno.getId());
        } else if ("PROFESSOR".equals(tipo)) {
            Professor prof = visitanteService.professorExemplo().orElse(null);
            if (prof == null) return semDadosDeExemplo(redirect);
            sessaoService.iniciar(request, response, "PROFESSOR", prof.getNome(), prof.getId());
        } else {
            sessaoService.iniciar(request, response, "ADMIN", "Administrador", null);
        }
        request.getSession().setAttribute("visitante", true);
        log.info("Entrada de visitante: perfil={} ip={}", tipo, request.getRemoteAddr());
        return "redirect:/";
    }

    private String semDadosDeExemplo(RedirectAttributes redirect) {
        redirect.addFlashAttribute("erroLogin", "Os dados de exemplo não estão disponíveis. Entre como administrador visitante.");
        return "redirect:/";
    }

    @PostMapping("/recuperar-senha")
    public String recuperarSenha(@RequestParam String tipo,
                                 @RequestParam(required = false) String identificador,
                                 @RequestParam(required = false) String cpf,
                                 @RequestParam(required = false) String email,
                                 @RequestParam(required = false) String novaSenha,
                                 HttpServletRequest request,
                                 RedirectAttributes redirect) {
        if (!recuperacaoPorEmail) return recuperarSenhaDireto(tipo, cpf, email, novaSenha, redirect);

        // Limita pedidos para ninguém usar o formulário para lotar a caixa de e-mail de alguém
        String ip = "recuperar-ip:" + request.getRemoteAddr();
        String conta = "recuperar-conta:" + tipo + ":" + (identificador == null ? "" : identificador.trim().toLowerCase());
        long espera = Math.max(limite.minutosRestantes(ip), limite.minutosRestantes(conta));
        if (espera > 0) {
            redirect.addFlashAttribute("erroLogin", "Muitos pedidos de nova senha. Tente de novo em " + espera + " minuto(s).");
            return "redirect:/";
        }
        limite.registrarFalha(ip, 10);
        limite.registrarFalha(conta, 3);

        if (!recuperacao.disponivel()) {
            redirect.addFlashAttribute("erroLogin", "A recuperação por e-mail está indisponível no momento. Procure a recepção da academia.");
            return "redirect:/";
        }
        recuperacao.solicitar(tipo, identificador);
        redirect.addFlashAttribute("sucessoLogin", "Se os dados estiverem certos, enviamos um link para o e-mail cadastrado. Ele vale "
                + RecuperacaoSenhaService.VALIDADE_MINUTOS + " minutos.");
        return "redirect:/";
    }

    @GetMapping("/redefinir-senha")
    public String telaRedefinirSenha(@RequestParam(required = false) String token, Model model) {
        if (!recuperacaoPorEmail) return "redirect:/";
        model.addAttribute("token", token);
        model.addAttribute("tokenValido", recuperacao.valido(token) != null);
        model.addAttribute("tamanhoMinimoSenha", senhaService.tamanhoMinimo());
        return "redefinir-senha";
    }

    @PostMapping("/redefinir-senha")
    public String redefinirSenha(@RequestParam String token, @RequestParam String novaSenha, RedirectAttributes redirect) {
        if (!recuperacaoPorEmail) return "redirect:/";
        String erro = recuperacao.redefinir(token, novaSenha);
        if (erro != null) {
            redirect.addFlashAttribute("erro", erro);
            redirect.addAttribute("token", token);
            return "redirect:/redefinir-senha";
        }
        redirect.addFlashAttribute("sucessoLogin", "Senha criada com sucesso! Faça seu login abaixo.");
        return "redirect:/";
    }

    /** Modo de desenvolvimento: troca a senha direto conferindo CPF e e-mail (sem link por e-mail). */
    private String recuperarSenhaDireto(String tipo, String cpf, String email, String novaSenha, RedirectAttributes redirect) {
        if (cpf == null || email == null) {
            redirect.addFlashAttribute("erroLogin", "Informe o CPF e o e-mail cadastrados.");
            return "redirect:/";
        }
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
