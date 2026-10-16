package com.example.sistema.academia.controller;

import com.example.sistema.academia.model.*;
import com.example.sistema.academia.repository.*;
import com.example.sistema.academia.service.SituacaoService;
import com.example.sistema.academia.util.Validador;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
public class AcademiaController {

    @Autowired private AlunoRepository alunoRepository;
    @Autowired private ProfessorRepository professorRepository;
    @Autowired private PlanoRepository planoRepository;
    @Autowired private MatriculaRepository matriculaRepository;
    @Autowired private MensalidadeRepository mensalidadeRepository;
    @Autowired private TreinoRepository treinoRepository;
    @Autowired private ExercicioTreinoRepository exercicioTreinoRepository;
    @Autowired private EntradaRepository entradaRepository;
    @Autowired private SituacaoService situacaoService;
    @Autowired private PasswordEncoder encoder;

    @Value("${gymsync.validar-cpf:true}") private boolean validarCpf;

    @GetMapping("/")
    public String paginaInicial(Model model, HttpSession session) {
        String perfil = (String) session.getAttribute("perfil");
        if (perfil == null) {
            model.addAttribute("logado", false);
            return "index";
        }

        situacaoService.atualizar();

        model.addAttribute("logado", true);
        model.addAttribute("perfil", perfil);
        model.addAttribute("nomeUsuario", session.getAttribute("nomeUsuario"));

        if ("ALUNO".equals(perfil)) {
            Long alunoId = (Long) session.getAttribute("usuarioId");
            model.addAttribute("exercicios", exercicioTreinoRepository.findByTreinoAlunoId(alunoId));
            model.addAttribute("mensalidades", mensalidadeRepository.findByMatriculaAlunoId(alunoId));
            model.addAttribute("entradas", entradaRepository.findByAlunoIdOrderByDataHoraEntradaDesc(alunoId));
            return "index";
        }

        model.addAttribute("alunos", alunoRepository.findAll());
        model.addAttribute("professores", professorRepository.findAll());
        model.addAttribute("planos", planoRepository.findAll());
        model.addAttribute("treinos", treinoRepository.findAll());
        model.addAttribute("exercicios", exercicioTreinoRepository.findAll());
        model.addAttribute("entradas", entradaRepository.findAllByOrderByDataHoraEntradaDesc());

        if ("PROFESSOR".equals(perfil)) {
            Long professorId = (Long) session.getAttribute("usuarioId");
            model.addAttribute("professorId", professorId);
            model.addAttribute("meusTreinos", treinoRepository.findByProfessorId(professorId));
            return "index";
        }

        model.addAttribute("matriculas", matriculaRepository.findAll());
        model.addAttribute("mensalidades", mensalidadeRepository.findAll());
        model.addAttribute("meusTreinos", treinoRepository.findAll());
        return "index";
    }

    private String validarPessoa(String nome, String cpf, String email, String contato) {
        if (nome == null || nome.trim().length() < 3) return "Informe o nome completo.";
        if (validarCpf && !Validador.cpfValido(cpf)) return "CPF inválido.";
        if (!Validador.emailValido(email)) return "E-mail inválido.";
        if (!Validador.telefoneValido(contato)) return "Telefone inválido.";
        return null;
    }

    @PostMapping("/alunos/salvar")
    public String salvarAluno(Aluno aluno, RedirectAttributes redirect) {
        String erro = validarPessoa(aluno.getNome(), aluno.getCpf(), aluno.getEmail(), aluno.getContato());
        LocalDate nascimento = aluno.getDataNascimento();
        if (erro == null && (nascimento == null || nascimento.isAfter(LocalDate.now()) || nascimento.getYear() < 1900)) {
            erro = "Data de nascimento inválida.";
        }
        if (erro == null && alunoRepository.existsByEmailIgnoreCase(aluno.getEmail().trim())) {
            erro = "Já existe um aluno cadastrado com este e-mail.";
        }
        if (erro != null) {
            redirect.addFlashAttribute("erro", erro);
            return "redirect:/";
        }

        String senha = (aluno.getSenha() == null || aluno.getSenha().isBlank()) ? "123" : aluno.getSenha();
        aluno.setNome(aluno.getNome().trim());
        aluno.setEmail(aluno.getEmail().trim());
        aluno.setSenha(encoder.encode(senha));
        try {
            alunoRepository.save(aluno);
            redirect.addFlashAttribute("sucesso", "Aluno cadastrado com sucesso!");
        } catch (Exception e) {
            redirect.addFlashAttribute("erro", "Erro ao cadastrar: CPF já cadastrado ou dados inválidos.");
        }
        return "redirect:/";
    }

    @PostMapping("/alunos/status/{id}")
    public String alterarStatusAluno(@PathVariable Long id) {
        Aluno a = alunoRepository.findById(id).orElse(null);
        if (a != null) {
            a.setSituacao("ATIVO".equals(a.getSituacao()) ? "INATIVO" : "ATIVO");
            alunoRepository.save(a);
        }
        return "redirect:/";
    }

    @PostMapping("/alunos/excluir/{id}")
    @Transactional
    public String excluirAluno(@PathVariable Long id, RedirectAttributes redirect) {
        entradaRepository.deleteAll(entradaRepository.findByAlunoId(id));
        exercicioTreinoRepository.deleteAll(exercicioTreinoRepository.findByTreinoAlunoId(id));
        treinoRepository.deleteAll(treinoRepository.findByAlunoId(id));
        mensalidadeRepository.deleteAll(mensalidadeRepository.findByMatriculaAlunoId(id));
        matriculaRepository.deleteAll(matriculaRepository.findByAlunoId(id));
        alunoRepository.deleteById(id);
        redirect.addFlashAttribute("sucesso", "Aluno e todo o histórico dele foram excluídos.");
        return "redirect:/";
    }

    @PostMapping("/professores/salvar")
    public String salvarProfessor(Professor professor, RedirectAttributes redirect) {
        String erro = validarPessoa(professor.getNome(), professor.getCpf(), professor.getEmail(), professor.getContato());
        if (erro == null && professorRepository.existsByEmailIgnoreCase(professor.getEmail().trim())) {
            erro = "Já existe um professor cadastrado com este e-mail.";
        }
        if (erro != null) {
            redirect.addFlashAttribute("erro", erro);
            return "redirect:/";
        }

        String senha = (professor.getSenha() == null || professor.getSenha().isBlank()) ? "123" : professor.getSenha();
        professor.setNome(professor.getNome().trim());
        professor.setEmail(professor.getEmail().trim());
        professor.setSenha(encoder.encode(senha));
        try {
            professorRepository.save(professor);
            redirect.addFlashAttribute("sucesso", "Professor cadastrado com sucesso!");
        } catch (Exception e) {
            redirect.addFlashAttribute("erro", "Erro ao cadastrar professor: CPF já cadastrado.");
        }
        return "redirect:/";
    }

    @PostMapping("/professores/excluir/{id}")
    @Transactional
    public String excluirProfessor(@PathVariable Long id, RedirectAttributes redirect) {
        exercicioTreinoRepository.deleteAll(exercicioTreinoRepository.findByTreinoProfessorId(id));
        treinoRepository.deleteAll(treinoRepository.findByProfessorId(id));
        professorRepository.deleteById(id);
        redirect.addFlashAttribute("sucesso", "Professor e as fichas dele foram excluídos.");
        return "redirect:/";
    }

    @PostMapping("/planos/salvar")
    public String salvarPlano(Plano plano, RedirectAttributes redirect) {
        if (plano.getNome() == null || plano.getNome().isBlank()) {
            redirect.addFlashAttribute("erro", "Informe o nome do plano.");
            return "redirect:/";
        }
        planoRepository.save(plano);
        redirect.addFlashAttribute("sucesso", "Plano cadastrado com sucesso!");
        return "redirect:/";
    }

    @PostMapping("/planos/excluir/{id}")
    @Transactional
    public String excluirPlano(@PathVariable Long id, RedirectAttributes redirect) {
        mensalidadeRepository.deleteAll(mensalidadeRepository.findByMatriculaPlanoId(id));
        matriculaRepository.deleteAll(matriculaRepository.findByPlanoId(id));
        planoRepository.deleteById(id);
        redirect.addFlashAttribute("sucesso", "Plano excluído junto com as matrículas e mensalidades dele.");
        return "redirect:/";
    }

    @PostMapping("/matriculas/salvar")
    public String salvarMatricula(@RequestParam Long alunoId, @RequestParam Long planoId, RedirectAttributes redirect) {
        situacaoService.atualizar();

        Aluno a = alunoRepository.findById(alunoId).orElse(null);
        Plano p = planoRepository.findById(planoId).orElse(null);
        if (a == null || p == null) {
            redirect.addFlashAttribute("erro", "Aluno ou plano não encontrado.");
            return "redirect:/";
        }
        if ("INATIVO".equals(a.getSituacao())) {
            redirect.addFlashAttribute("erro", "O aluno " + a.getNome() + " está INATIVO. Ative o cadastro antes de matricular.");
            return "redirect:/";
        }
        if (matriculaRepository.existsByAlunoIdAndSituacao(alunoId, "ATIVA")) {
            redirect.addFlashAttribute("erro", "O aluno " + a.getNome() + " já possui uma matrícula ativa.");
            return "redirect:/";
        }

        long meses = p.getDuracaoMeses();
        if (meses < 1) meses = 1;

        Matricula m = new Matricula();
        m.setAluno(a);
        m.setPlano(p);
        m.setDataInicio(LocalDate.now());
        m.setDataFim(LocalDate.now().plusMonths(meses));
        m.setSituacao("ATIVA");
        matriculaRepository.save(m);

        BigDecimal total = p.getValor();
        BigDecimal parcela = total.divide(BigDecimal.valueOf(meses), 2, RoundingMode.HALF_UP);
        BigDecimal acumulado = BigDecimal.ZERO;

        for (int i = 0; i < meses; i++) {
            BigDecimal valorParcela = (i == meses - 1) ? total.subtract(acumulado) : parcela;
            acumulado = acumulado.add(valorParcela);

            Mensalidade mens = new Mensalidade();
            mens.setMatricula(m);
            mens.setValor(valorParcela);
            mens.setVencimento(LocalDate.now().plusDays(10).plusMonths(i));
            mens.setSituacao("PENDENTE");
            mensalidadeRepository.save(mens);
        }
        redirect.addFlashAttribute("sucesso", "Matrícula realizada e " + meses + " mensalidade(s) gerada(s)!");
        return "redirect:/";
    }

    @PostMapping("/mensalidades/pagar/{id}")
    public String pagarMensalidade(@PathVariable Long id, RedirectAttributes redirect) {
        Mensalidade m = mensalidadeRepository.findById(id).orElse(null);
        if (m != null) {
            m.setSituacao("PAGA");
            m.setDataPagamento(LocalDate.now());
            mensalidadeRepository.save(m);
            redirect.addFlashAttribute("sucesso", "Pagamento registrado com sucesso!");
        }
        return "redirect:/";
    }

    @PostMapping("/mensalidades/excluir/{id}")
    public String excluirMensalidade(@PathVariable Long id) {
        mensalidadeRepository.deleteById(id);
        return "redirect:/";
    }

    @PostMapping("/entradas/registrar")
    public String registrarEntrada(@RequestParam Long alunoId, RedirectAttributes redirect) {
        situacaoService.atualizar();

        Aluno a = alunoRepository.findById(alunoId).orElse(null);
        if (a == null) {
            return "redirect:/";
        }
        if ("INATIVO".equals(a.getSituacao())) {
            redirect.addFlashAttribute("erro", "Catraca Bloqueada: o aluno " + a.getNome() + " está com cadastro INATIVO!");
            return "redirect:/";
        }
        if (!matriculaRepository.existsByAlunoIdAndSituacao(alunoId, "ATIVA")) {
            redirect.addFlashAttribute("erro", "Catraca Bloqueada: o aluno " + a.getNome() + " não possui matrícula ativa!");
            return "redirect:/";
        }
        if (mensalidadeRepository.existsByMatriculaAlunoIdAndSituacao(alunoId, "ATRASADA")) {
            redirect.addFlashAttribute("erro", "Catraca Bloqueada: o aluno " + a.getNome() + " está com mensalidade em atraso!");
            return "redirect:/";
        }

        Entrada e = new Entrada();
        e.setAluno(a);
        e.setDataHoraEntrada(LocalDateTime.now());
        entradaRepository.save(e);
        redirect.addFlashAttribute("sucesso", "Catraca Liberada: acesso registrado para " + a.getNome() + "!");
        return "redirect:/";
    }

    @PostMapping("/entradas/excluir/{id}")
    public String excluirEntrada(@PathVariable Long id) {
        entradaRepository.deleteById(id);
        return "redirect:/";
    }
}