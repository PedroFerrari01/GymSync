package com.example.sistema.academia.controller;

import com.example.sistema.academia.model.*;
import com.example.sistema.academia.repository.*;
import com.example.sistema.academia.service.*;
import com.example.sistema.academia.util.Validador;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

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
    @Autowired private AvaliacaoFisicaRepository avaliacaoFisicaRepository;
    @Autowired private EquipamentoRepository equipamentoRepository;
    @Autowired private ManutencaoRepository manutencaoRepository;
    @Autowired private MensagemRepository mensagemRepository;
    @Autowired private RegistroExercicioRepository registroExercicioRepository;
    @Autowired private SituacaoService situacaoService;
    @Autowired private ExclusaoService exclusaoService;
    @Autowired private AvisoService avisoService;
    @Autowired private RelatorioService relatorioService;
    @Autowired private AulaService aulaService;
    @Autowired private FichaService fichaService;
    @Autowired private PasswordEncoder encoder;
    @Autowired private SenhaService senhaService;

    @Value("${gymsync.validar-cpf:true}") private boolean validarCpf;

    private Map<Long, RegistroExercicio> ultimosRegistros(List<RegistroExercicio> lista) {
        List<RegistroExercicio> ordenada = new ArrayList<>(lista);
        ordenada.sort(Comparator.comparing(RegistroExercicio::getDataRegistro).thenComparing(RegistroExercicio::getId).reversed());
        Map<Long, RegistroExercicio> mapa = new HashMap<>();
        for (RegistroExercicio r : ordenada) {
            mapa.putIfAbsent(r.getExercicio().getId(), r);
        }
        return mapa;
    }

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
        model.addAttribute("senhasFortes", senhaService.fortes());
        model.addAttribute("tamanhoMinimoSenha", senhaService.tamanhoMinimo());
        model.addAttribute("trocarSenha", Boolean.TRUE.equals(session.getAttribute("trocarSenha")));
        model.addAttribute("nomeUsuario", session.getAttribute("nomeUsuario"));
        Long usuarioId = (Long) session.getAttribute("usuarioId");

        if ("ALUNO".equals(perfil)) {
            Aluno aluno = usuarioId != null ? alunoRepository.findById(usuarioId).orElse(null) : null;
            if (aluno == null) {
                session.invalidate();
                return "redirect:/";
            }

            List<Entrada> entradas = entradaRepository.findByAlunoIdOrderByDataHoraEntradaDesc(usuarioId);
            LocalDate hoje = LocalDate.now();
            LocalDate inicioSemana = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            long visitasSemana = entradas.stream().filter(e -> !e.getDataHoraEntrada().toLocalDate().isBefore(inicioSemana)).count();
            long visitas30 = entradas.stream().filter(e -> !e.getDataHoraEntrada().toLocalDate().isBefore(hoje.minusDays(30))).count();

            List<AvaliacaoFisica> avaliacoes = avaliacaoFisicaRepository.findByAlunoIdOrderByDataAvaliacaoAscIdAsc(usuarioId);

            model.addAttribute("exercicios", exercicioTreinoRepository.findByTreinoAlunoId(usuarioId));
            model.addAttribute("mensalidades", mensalidadeRepository.findByMatriculaAlunoId(usuarioId));
            model.addAttribute("entradas", entradas);
            model.addAttribute("visitasSemana", visitasSemana);
            model.addAttribute("visitas30", visitas30);
            model.addAttribute("avisos", avisoService.paraAluno(usuarioId));
            model.addAttribute("ultimosRegistros", ultimosRegistros(registroExercicioRepository.findByAlunoId(usuarioId)));
            model.addAttribute("avaliacoes", avaliacoes);
            model.addAttribute("barrasPeso", relatorioService.barrasPeso(avaliacoes));
            model.addAttribute("resumoPeso", relatorioService.resumoPeso(avaliacoes));
            model.addAttribute("linkWhatsappFicha", fichaService.linkWhatsapp(aluno.getContato(), fichaService.textoAluno(aluno)));
            model.addAttribute("naoLidas", mensagemRepository.countByAlunoIdAndRemetenteAndLidaFalse(usuarioId, "PROFESSOR"));
            aulaService.preencher(model, usuarioId);
            return "index";
        }

        List<Entrada> todasEntradas = entradaRepository.findAllByOrderByDataHoraEntradaDesc();
        List<Treino> treinos = treinoRepository.findAll();
        Map<Long, String> linksWhatsapp = new HashMap<>();
        for (Treino t : treinos) {
            String texto = "GymSync - Ficha de treino de " + t.getAluno().getNome() + "\n\n" + fichaService.textoTreino(t);
            String link = fichaService.linkWhatsapp(t.getAluno().getContato(), texto);
            if (link != null) linksWhatsapp.put(t.getId(), link);
        }

        model.addAttribute("alunos", alunoRepository.findAll());
        model.addAttribute("professores", professorRepository.findAll());
        model.addAttribute("planos", planoRepository.findAll());
        model.addAttribute("treinos", treinos);
        model.addAttribute("exercicios", exercicioTreinoRepository.findAll());
        model.addAttribute("entradas", todasEntradas);
        model.addAttribute("entradasRecentes", todasEntradas.stream().limit(30).toList());
        model.addAttribute("avaliacoes", avaliacaoFisicaRepository.findAllByOrderByDataAvaliacaoDescIdDesc());
        model.addAttribute("ultimosRegistros", ultimosRegistros(registroExercicioRepository.findAll()));
        model.addAttribute("linksWhatsapp", linksWhatsapp);
        aulaService.preencher(model, null);

        if ("PROFESSOR".equals(perfil)) {
            model.addAttribute("professorId", usuarioId);
            model.addAttribute("meusTreinos", treinoRepository.findByProfessorId(usuarioId));
            model.addAttribute("naoLidas", mensagemRepository.countByProfessorIdAndRemetenteAndLidaFalse(usuarioId, "ALUNO"));
            return "index";
        }

        model.addAttribute("meusTreinos", treinos);
        model.addAttribute("matriculas", matriculaRepository.findAll());
        model.addAttribute("mensalidades", mensalidadeRepository.findAll());
        model.addAttribute("avisos", avisoService.paraAdmin());
        model.addAttribute("equipamentos", equipamentoRepository.findAll());
        model.addAttribute("manutencoes", manutencaoRepository.findAllByOrderByDataManutencaoDescIdDesc());
        relatorioService.montar(model);
        return "index";
    }

    private String validarPessoa(String nome, String cpf, String email, String contato) {
        if (nome == null || nome.trim().length() < 3) return "Informe o nome completo.";
        if (cpf != null && validarCpf && !Validador.cpfValido(cpf)) return "CPF inválido.";
        if (!Validador.emailValido(email)) return "E-mail inválido.";
        if (!Validador.telefoneValido(contato)) return "Telefone inválido.";
        return null;
    }

    private boolean nascimentoInvalido(LocalDate nascimento) {
        return nascimento == null || nascimento.isAfter(LocalDate.now()) || nascimento.getYear() < 1900;
    }

    @PostMapping("/alunos/salvar")
    public String salvarAluno(Aluno aluno, RedirectAttributes redirect) {
        String erro = validarPessoa(aluno.getNome(), aluno.getCpf(), aluno.getEmail(), aluno.getContato());
        if (erro == null && nascimentoInvalido(aluno.getDataNascimento())) {
            erro = "Data de nascimento inválida.";
        }
        if (erro == null && alunoRepository.existsByEmailIgnoreCase(aluno.getEmail().trim())) {
            erro = "Já existe um aluno cadastrado com este e-mail.";
        }
        if (erro != null) {
            redirect.addFlashAttribute("erro", erro);
            return "redirect:/";
        }

        boolean semSenha = aluno.getSenha() == null || aluno.getSenha().isBlank();
        String senha = semSenha ? senhaService.provisoria() : aluno.getSenha();
        if (!semSenha && senhaService.validarNova(senha) != null) {
            redirect.addFlashAttribute("erro", senhaService.validarNova(senha));
            return "redirect:/";
        }
        aluno.setTrocarSenha(senhaService.fortes());
        aluno.setNome(aluno.getNome().trim());
        aluno.setEmail(aluno.getEmail().trim());
        aluno.setSenha(encoder.encode(senha));
        try {
            alunoRepository.save(aluno);
            redirect.addFlashAttribute("sucesso", "Aluno cadastrado com sucesso!" + avisoSenha(semSenha, senha));
        } catch (Exception e) {
            redirect.addFlashAttribute("erro", "Erro ao cadastrar: CPF já cadastrado ou dados inválidos.");
        }
        return "redirect:/";
    }

    @PostMapping("/alunos/editar")
    public String editarAluno(@RequestParam Long id,
                              @RequestParam String nome,
                              @RequestParam String email,
                              @RequestParam String contato,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNascimento,
                              RedirectAttributes redirect) {
        Aluno aluno = alunoRepository.findById(id).orElse(null);
        if (aluno == null) {
            redirect.addFlashAttribute("erro", "Aluno não encontrado.");
            return "redirect:/";
        }
        String erro = validarPessoa(nome, null, email, contato);
        if (erro == null && nascimentoInvalido(dataNascimento)) erro = "Data de nascimento inválida.";
        if (erro == null && alunoRepository.existsByEmailIgnoreCaseAndIdNot(email.trim(), id)) {
            erro = "Já existe outro aluno com este e-mail.";
        }
        if (erro != null) {
            redirect.addFlashAttribute("erro", erro);
            return "redirect:/";
        }
        aluno.setNome(nome.trim());
        aluno.setEmail(email.trim());
        aluno.setContato(contato);
        aluno.setDataNascimento(dataNascimento);
        alunoRepository.save(aluno);
        redirect.addFlashAttribute("sucesso", "Dados do aluno atualizados!");
        return "redirect:/";
    }

    /** No modo de senhas fortes, mostra ao administrador a senha provisória gerada (só uma vez). */
    private String avisoSenha(boolean gerada, String senha) {
        if (!senhaService.fortes()) return "";
        return gerada ? " Senha provisória: " + senha + " (será trocada no primeiro acesso)." : " A senha será trocada no primeiro acesso.";
    }

    @PostMapping("/alunos/redefinir-senha/{id}")
    public String redefinirSenhaAluno(@PathVariable Long id, RedirectAttributes redirect) {
        Aluno aluno = alunoRepository.findById(id).orElse(null);
        if (aluno != null) {
            String senha = senhaService.provisoria();
            aluno.setSenha(encoder.encode(senha));
            aluno.setTrocarSenha(senhaService.fortes());
            alunoRepository.save(aluno);
            redirect.addFlashAttribute("sucesso", "A senha de " + aluno.getNome() + " voltou para " + senha + "."
                    + (senhaService.fortes() ? " Ela terá de ser trocada no próximo acesso." : ""));
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
    public String excluirAluno(@PathVariable Long id, RedirectAttributes redirect) {
        exclusaoService.excluirAluno(id);
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

        boolean semSenha = professor.getSenha() == null || professor.getSenha().isBlank();
        String senha = semSenha ? senhaService.provisoria() : professor.getSenha();
        if (!semSenha && senhaService.validarNova(senha) != null) {
            redirect.addFlashAttribute("erro", senhaService.validarNova(senha));
            return "redirect:/";
        }
        professor.setTrocarSenha(senhaService.fortes());
        professor.setNome(professor.getNome().trim());
        professor.setEmail(professor.getEmail().trim());
        professor.setSenha(encoder.encode(senha));
        try {
            professorRepository.save(professor);
            redirect.addFlashAttribute("sucesso", "Professor cadastrado com sucesso!" + avisoSenha(semSenha, senha));
        } catch (Exception e) {
            redirect.addFlashAttribute("erro", "Erro ao cadastrar professor: CPF já cadastrado.");
        }
        return "redirect:/";
    }

    @PostMapping("/professores/editar")
    public String editarProfessor(@RequestParam Long id,
                                  @RequestParam String nome,
                                  @RequestParam String email,
                                  @RequestParam String contato,
                                  @RequestParam String especialidade,
                                  RedirectAttributes redirect) {
        Professor professor = professorRepository.findById(id).orElse(null);
        if (professor == null) {
            redirect.addFlashAttribute("erro", "Professor não encontrado.");
            return "redirect:/";
        }
        String erro = validarPessoa(nome, null, email, contato);
        if (erro == null && especialidade.isBlank()) erro = "Informe a especialidade.";
        if (erro == null && professorRepository.existsByEmailIgnoreCaseAndIdNot(email.trim(), id)) {
            erro = "Já existe outro professor com este e-mail.";
        }
        if (erro != null) {
            redirect.addFlashAttribute("erro", erro);
            return "redirect:/";
        }
        professor.setNome(nome.trim());
        professor.setEmail(email.trim());
        professor.setContato(contato);
        professor.setEspecialidade(especialidade.trim());
        professorRepository.save(professor);
        redirect.addFlashAttribute("sucesso", "Dados do professor atualizados!");
        return "redirect:/";
    }

    @PostMapping("/professores/redefinir-senha/{id}")
    public String redefinirSenhaProfessor(@PathVariable Long id, RedirectAttributes redirect) {
        Professor professor = professorRepository.findById(id).orElse(null);
        if (professor != null) {
            String senha = senhaService.provisoria();
            professor.setSenha(encoder.encode(senha));
            professor.setTrocarSenha(senhaService.fortes());
            professorRepository.save(professor);
            redirect.addFlashAttribute("sucesso", "A senha de " + professor.getNome() + " voltou para " + senha + "."
                    + (senhaService.fortes() ? " Ela terá de ser trocada no próximo acesso." : ""));
        }
        return "redirect:/";
    }

    @PostMapping("/professores/excluir/{id}")
    public String excluirProfessor(@PathVariable Long id, RedirectAttributes redirect) {
        exclusaoService.excluirProfessor(id);
        redirect.addFlashAttribute("sucesso", "Professor, fichas e aulas dele foram excluídos.");
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

    @PostMapping("/planos/editar")
    public String editarPlano(@RequestParam Long id, Plano dados, RedirectAttributes redirect) {
        Plano plano = planoRepository.findById(id).orElse(null);
        if (plano == null || dados.getNome() == null || dados.getNome().isBlank()
                || dados.getValor() == null || "null".equals(String.valueOf(dados.getDuracaoMeses()))) {
            redirect.addFlashAttribute("erro", "Preencha todos os dados do plano.");
            return "redirect:/";
        }
        plano.setNome(dados.getNome().trim());
        plano.setDuracaoMeses(dados.getDuracaoMeses());
        plano.setValor(dados.getValor());
        planoRepository.save(plano);
        redirect.addFlashAttribute("sucesso", "Plano atualizado! As mensalidades que já foram geradas não mudam.");
        return "redirect:/";
    }

    @PostMapping("/planos/excluir/{id}")
    public String excluirPlano(@PathVariable Long id, RedirectAttributes redirect) {
        exclusaoService.excluirPlano(id);
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

    @PostMapping("/matriculas/cancelar/{id}")
    public String cancelarMatricula(@PathVariable Long id, RedirectAttributes redirect) {
        Matricula m = matriculaRepository.findById(id).orElse(null);
        if (m == null) {
            return "redirect:/";
        }
        m.setSituacao("CANCELADA");
        matriculaRepository.save(m);

        List<Mensalidade> abertas = new ArrayList<>();
        for (Mensalidade men : mensalidadeRepository.findByMatriculaId(id)) {
            String s = men.getSituacao();
            if ("PENDENTE".equals(s) || "ATRASADA".equals(s) || "EM_ANALISE".equals(s)) {
                men.setSituacao("CANCELADA");
                abertas.add(men);
            }
        }
        mensalidadeRepository.saveAll(abertas);
        redirect.addFlashAttribute("sucesso", "Matrícula cancelada. " + abertas.size() + " mensalidade(s) em aberto foram canceladas.");
        return "redirect:/";
    }

    @PostMapping("/mensalidades/pagar/{id}")
    public String pagarMensalidade(@PathVariable Long id, RedirectAttributes redirect) {
        Mensalidade m = mensalidadeRepository.findById(id).orElse(null);
        if (m != null && !"PAGA".equals(m.getSituacao()) && !"CANCELADA".equals(m.getSituacao())) {
            m.setSituacao("PAGA");
            m.setDataPagamento(LocalDate.now());
            mensalidadeRepository.save(m);
            redirect.addFlashAttribute("sucesso", "Pagamento registrado com sucesso!");
        }
        return "redirect:/";
    }

    @PostMapping("/mensalidades/recusar/{id}")
    public String recusarPagamento(@PathVariable Long id, RedirectAttributes redirect) {
        Mensalidade m = mensalidadeRepository.findById(id).orElse(null);
        if (m != null && "EM_ANALISE".equals(m.getSituacao())) {
            m.setSituacao("PENDENTE");
            mensalidadeRepository.save(m);
            redirect.addFlashAttribute("sucesso", "Pagamento recusado. A mensalidade voltou para pendente.");
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
