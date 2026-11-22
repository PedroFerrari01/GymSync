package com.example.sistema.academia.controller;

import com.example.sistema.academia.dto.Acompanhamento;
import com.example.sistema.academia.dto.Aviso;
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
import org.springframework.web.bind.WebDataBinder;
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
    @Autowired private EquipamentoImagemRepository equipamentoImagemRepository;
    @Autowired private ManutencaoRepository manutencaoRepository;
    @Autowired private MensagemRepository mensagemRepository;
    @Autowired private RegistroExercicioRepository registroExercicioRepository;
    @Autowired private SituacaoService situacaoService;
    @Autowired private ExclusaoService exclusaoService;
    @Autowired private AvisoService avisoService;
    @Autowired private RelatorioService relatorioService;
    @Autowired private com.example.sistema.academia.service.MovimentoService movimentoService;
    @Autowired private AulaService aulaService;
    @Autowired private FichaService fichaService;
    @Autowired private FrequenciaService frequenciaService;
    @Autowired private EvolucaoService evolucaoService;
    @Autowired private AcompanhamentoService acompanhamentoService;
    @Autowired private SininhoService sininhoService;
    @Autowired private com.example.sistema.academia.service.VisaoEquipeService visaoEquipeService;
    @Autowired private com.example.sistema.academia.service.AlunosDoProfessorService alunosDoProfessor;
    @Autowired private PasswordEncoder encoder;
    @Autowired private SenhaService senhaService;
    @Autowired private VisitanteService visitanteService;
    @Autowired private CarteirinhaService carteirinhaService;

    @Value("${gymsync.validar-cpf:true}") private boolean validarCpf;
    @Value("${gymsync.recuperacao-por-email:true}") private boolean recuperacaoPorEmail;
    @Value("${gymsync.video-youtube:}") private String videoYoutube;

    // Os cadastros recebem a entidade inteira: só estes campos podem vir do formulário (nada de id, situacao etc.)
    @InitBinder("aluno")
    void camposAluno(WebDataBinder binder) {
        binder.setAllowedFields("nome", "cpf", "email", "dataNascimento", "contato", "senha", "alturaCm", "silhueta");
    }

    @InitBinder("professor")
    void camposProfessor(WebDataBinder binder) {
        binder.setAllowedFields("nome", "cpf", "email", "contato", "especialidade", "senha");
    }

    @InitBinder("plano")
    void camposPlano(WebDataBinder binder) {
        binder.setAllowedFields("nome", "duracaoMeses", "valor");
    }

    private Map<Long, RegistroExercicio> ultimosRegistros(List<RegistroExercicio> lista) {
        List<RegistroExercicio> ordenada = new ArrayList<>(lista);
        ordenada.sort(Comparator.comparing(RegistroExercicio::getDataRegistro).thenComparing(RegistroExercicio::getId).reversed());
        Map<Long, RegistroExercicio> mapa = new HashMap<>();
        for (RegistroExercicio r : ordenada) {
            mapa.putIfAbsent(r.getExercicio().getId(), r);
        }
        return mapa;
    }

    private List<Acompanhamento> acompanhamento(List<Treino> treinos, List<ExercicioTreino> exercicios, List<Entrada> entradas,
                                                List<RegistroExercicio> registros, String nomeUsuario, boolean professor) {
        List<Acompanhamento> linhas = acompanhamentoService.montar(treinos, exercicios, entradas, registros, LocalDate.now());
        String quem = professor && nomeUsuario != null ? "o(a) professor(a) " + nomeUsuario : "a recepção";
        for (Acompanhamento l : linhas) {
            if (!l.isSumido()) continue;
            String primeiroNome = l.getNome().trim().split("\\s+")[0];
            String texto = "Oi, " + primeiroNome + "! Aqui é " + quem + " da academia. Sentimos sua falta nos treinos."
                    + " Está tudo bem? Vamos voltar esta semana?";
            l.setLinkWhatsapp(fichaService.linkWhatsapp(l.getContato(), texto));
        }
        return linhas;
    }

    /** "Spinning, seg 18:00 (23/11)" para cada aula que o aluno reservou (dados que o AulaService já pôs no model). */
    @SuppressWarnings("unchecked")
    private List<String> aulasReservadasTexto(Model model) {
        List<AulaColetiva> aulas = (List<AulaColetiva>) model.asMap().get("aulas");
        Set<Long> reservadas = (Set<Long>) model.asMap().get("aulasReservadas");
        Map<Long, LocalDate> datas = (Map<Long, LocalDate>) model.asMap().get("proximasDatas");
        List<String> textos = new ArrayList<>();
        if (aulas == null || reservadas == null || datas == null) return textos;
        for (AulaColetiva a : aulas) {
            if (!reservadas.contains(a.getId()) || a.getHorario() == null) continue;
            textos.add(a.getNome() + ", " + a.getDiaSemanaPt() + " " + a.getHorario()
                    + " (" + datas.get(a.getId()).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")) + ")");
        }
        return textos;
    }

    @GetMapping("/")
    public String paginaInicial(@RequestParam(required = false) String mes,
                                @RequestParam(required = false) String semana, Model model, HttpSession session) {
        String perfil = (String) session.getAttribute("perfil");
        model.addAttribute("tamanhoMinimoSenha", senhaService.tamanhoMinimo());
        if (perfil == null) {
            model.addAttribute("logado", false);
            model.addAttribute("recuperacaoPorEmail", recuperacaoPorEmail);
            model.addAttribute("visitanteHabilitado", visitanteService.habilitado());
            model.addAttribute("videoYoutube", videoYoutube.matches("[A-Za-z0-9_-]{11}") ? videoYoutube : "");
            return "index";
        }

        situacaoService.atualizar();

        model.addAttribute("logado", true);
        model.addAttribute("perfil", perfil);
        model.addAttribute("senhasFortes", senhaService.fortes());
        model.addAttribute("trocarSenha", Boolean.TRUE.equals(session.getAttribute("trocarSenha")));
        model.addAttribute("visitante", Boolean.TRUE.equals(session.getAttribute("visitante")));
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
            String codigoCarteirinha = carteirinhaService.token(usuarioId);
            String motivoCarteirinha = CarteirinhaService.motivoBloqueio(aluno,
                    matriculaRepository.existsByAlunoIdAndSituacao(usuarioId, "ATIVA"),
                    mensalidadeRepository.existsByMatriculaAlunoIdAndSituacao(usuarioId, "ATRASADA"));
            model.addAttribute("carteirinhaCodigo", codigoCarteirinha);
            model.addAttribute("carteirinhaQr", carteirinhaService.qrSvg(codigoCarteirinha));
            model.addAttribute("carteirinhaMotivo", motivoCarteirinha);
            model.addAttribute("carteirinhaRestante", CarteirinhaService.VALIDADE_SEGUNDOS);
            model.addAttribute("carteirinhaAluno", aluno);
            model.addAttribute("entradas", entradas);
            model.addAttribute("visitasSemana", visitasSemana);
            model.addAttribute("visitas30", visitas30);
            model.addAttribute("calendario", frequenciaService.calendario(entradas, frequenciaService.mesPedido(mes, hoje), hoje));
            model.addAttribute("pedirAceite", aluno.getAceitePrivacidade() == null
                    && !Boolean.TRUE.equals(session.getAttribute("visitante"))
                    && !Boolean.TRUE.equals(session.getAttribute("trocarSenha")));
            model.addAttribute("avisos", avisoService.paraAluno(usuarioId));
            model.addAttribute("ultimosRegistros", ultimosRegistros(registroExercicioRepository.findByAlunoId(usuarioId)));
            model.addAttribute("avaliacoes", avaliacoes);
            model.addAttribute("barrasPeso", relatorioService.barrasPeso(avaliacoes));
            model.addAttribute("resumoPeso", relatorioService.resumoPeso(avaliacoes));
            model.addAttribute("barrasGordura", evolucaoService.barrasGordura(avaliacoes));
            model.addAttribute("comparacao", evolucaoService.comparacao(avaliacoes));
            model.addAttribute("mapasCorpo", avaliacoes.isEmpty() ? List.of()
                    : List.of(evolucaoService.mapaCorpo(aluno.getId(), aluno.getNome(), avaliacoes)));
            model.addAttribute("linkWhatsappFicha", fichaService.linkWhatsapp(aluno.getContato(), fichaService.textoAluno(aluno)));
            model.addAttribute("naoLidas", mensagemRepository.countByAlunoIdAndRemetenteAndLidaFalse(usuarioId, "PROFESSOR"));
            aulaService.preencher(model, usuarioId);
            aulaService.grade(model, usuarioId, semana);
            model.addAttribute("melhorHorario", movimentoService.planoAluno(java.time.LocalDateTime.now()));
            model.addAttribute("sininho", sininhoService.paraAluno(avisoService.paraAluno(usuarioId),
                    mensagemRepository.countByAlunoIdAndRemetenteAndLidaFalse(usuarioId, "PROFESSOR"), aulasReservadasTexto(model)));
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
        List<ExercicioTreino> todosExercicios = exercicioTreinoRepository.findAll();
        List<RegistroExercicio> todosRegistros = registroExercicioRepository.findAll();
        boolean professor = "PROFESSOR".equals(perfil);
        List<Treino> meusTreinos = professor ? treinoRepository.findByProfessorId(usuarioId) : treinos;

        model.addAttribute("treinos", treinos);
        model.addAttribute("exercicios", todosExercicios);
        model.addAttribute("entradas", todasEntradas);
        model.addAttribute("entradasRecentes", todasEntradas.stream().limit(30).toList());
        List<AvaliacaoFisica> todasAvaliacoes = avaliacaoFisicaRepository.findAllByOrderByDataAvaliacaoDescIdDesc();
        model.addAttribute("avaliacoes", todasAvaliacoes);
        model.addAttribute("mapasCorpo", evolucaoService.mapasCorpo(todasAvaliacoes));
        model.addAttribute("ultimosRegistros", ultimosRegistros(todosRegistros));
        model.addAttribute("linksWhatsapp", linksWhatsapp);
        model.addAttribute("acompanhamento", acompanhamento(meusTreinos, todosExercicios, todasEntradas, todosRegistros,
                (String) session.getAttribute("nomeUsuario"), professor));
        model.addAttribute("diasSumido", AcompanhamentoService.DIAS_SUMIDO);
        model.addAttribute("painel", visaoEquipeService.montar(professor ? usuarioId : null, LocalDate.now()));
        aulaService.preencher(model, null);
        aulaService.grade(model, null, semana);
        model.addAttribute("mapaCalor", movimentoService.mapa(java.time.LocalDateTime.now()));

        if (professor) {
            // LGPD: o professor só vê fichas, avaliações, entradas e WhatsApp dos próprios alunos.
            // A lista "alunos" continua inteira só para os campos de escolha (nome), para ele montar a 1ª ficha.
            java.util.Set<Long> meus = alunosDoProfessor.ids(usuarioId);
            java.util.Set<Long> idsMeusTreinos = new java.util.HashSet<>();
            for (Treino t : meusTreinos) idsMeusTreinos.add(t.getId());
            Map<Long, String> meusLinks = new HashMap<>();
            linksWhatsapp.forEach((treinoId, link) -> { if (idsMeusTreinos.contains(treinoId)) meusLinks.put(treinoId, link); });
            List<Entrada> entradasMeus = todasEntradas.stream()
                    .filter(e -> e.getAluno() != null && meus.contains(e.getAluno().getId())).toList();
            model.addAttribute("treinos", meusTreinos);
            model.addAttribute("exercicios", todosExercicios.stream()
                    .filter(ex -> ex.getTreino() != null && idsMeusTreinos.contains(ex.getTreino().getId())).toList());
            model.addAttribute("entradas", entradasMeus);
            model.addAttribute("entradasRecentes", entradasMeus.stream().limit(30).toList());
            model.addAttribute("avaliacoes", avaliacaoFisicaRepository.findAllByOrderByDataAvaliacaoDescIdDesc().stream()
                    .filter(av -> av.getAluno() != null && meus.contains(av.getAluno().getId())).toList());
            model.addAttribute("ultimosRegistros", ultimosRegistros(todosRegistros.stream()
                    .filter(r -> r.getAluno() != null && meus.contains(r.getAluno().getId())).toList()));
            model.addAttribute("linksWhatsapp", meusLinks);
            model.addAttribute("professorId", usuarioId);
            model.addAttribute("meusTreinos", meusTreinos);
            model.addAttribute("naoLidas", mensagemRepository.countByProfessorIdAndRemetenteAndLidaFalse(usuarioId, "ALUNO"));
            model.addAttribute("sininho", sininhoService.paraProfessor((List<Acompanhamento>) model.asMap().get("acompanhamento")));
            return "index";
        }

        model.addAttribute("meusTreinos", treinos);
        if (!professor) {
            List<com.example.sistema.academia.dto.ResumoProfessor> equipe = new java.util.ArrayList<>();
            for (com.example.sistema.academia.model.Professor p : professorRepository.findAll()) {
                List<Treino> dele = treinoRepository.findByProfessorId(p.getId());
                List<Acompanhamento> linhas = acompanhamentoService.montar(dele, todosExercicios, todasEntradas, todosRegistros, LocalDate.now());
                int sumidos = (int) linhas.stream().filter(Acompanhamento::isSumido).count();
                int media = linhas.isEmpty() ? 0 : (int) Math.round(linhas.stream().mapToInt(Acompanhamento::getAdesao).average().orElse(0));
                equipe.add(new com.example.sistema.academia.dto.ResumoProfessor(p.getNome(), p.getEspecialidade(), dele.size(), linhas.size(), sumidos, media));
            }
            model.addAttribute("equipe", equipe);
        }
        model.addAttribute("matriculas", matriculaRepository.findAll());
        model.addAttribute("mensalidades", mensalidadeRepository.findAll());
        List<Aviso> avisosAdmin = avisoService.paraAdmin();
        model.addAttribute("avisos", avisosAdmin);
        model.addAttribute("sininho", sininhoService.paraAdmin(avisosAdmin, (List<Acompanhamento>) model.asMap().get("acompanhamento")));
        model.addAttribute("equipamentos", equipamentoRepository.findAll());
        model.addAttribute("equipamentosComFoto", equipamentoImagemRepository.idsComImagem());
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

    /** Altura do aluno é opcional; quando vem, fica entre 100 e 250 cm. */
    static boolean alturaInvalida(Integer alturaCm) {
        return alturaCm != null && (alturaCm < 100 || alturaCm > 250);
    }

    @PostMapping("/alunos/salvar")
    public String salvarAluno(Aluno aluno, RedirectAttributes redirect) {
        String erro = validarPessoa(aluno.getNome(), aluno.getCpf(), aluno.getEmail(), aluno.getContato());
        if (erro == null && nascimentoInvalido(aluno.getDataNascimento())) {
            erro = "Data de nascimento inválida.";
        }
        if (erro == null && alturaInvalida(aluno.getAlturaCm())) erro = "Altura inválida (use centímetros, entre 100 e 250).";
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
                              @RequestParam(required = false) Integer alturaCm,
                              @RequestParam(required = false) String silhueta,
                              RedirectAttributes redirect) {
        Aluno aluno = alunoRepository.findById(id).orElse(null);
        if (aluno == null) {
            redirect.addFlashAttribute("erro", "Aluno não encontrado.");
            return "redirect:/";
        }
        String erro = validarPessoa(nome, null, email, contato);
        if (erro == null && nascimentoInvalido(dataNascimento)) erro = "Data de nascimento inválida.";
        if (erro == null && alturaInvalida(alturaCm)) erro = "Altura inválida (use centímetros, entre 100 e 250).";
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
        aluno.setAlturaCm(alturaCm);
        aluno.setSilhueta(silhueta);
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
        return liberarCatraca(alunoId, redirect);
    }

    /** Leitura da carteirinha digital (simulada): o código do QR leva ao mesmo fluxo da catraca. */
    @PostMapping("/entradas/carteirinha")
    public String registrarPelaCarteirinha(@RequestParam String codigo,
                                           @RequestParam(required = false) String origem,
                                           RedirectAttributes redirect) {
        // Vindo do leitor pela webcam, volta para ele (próximo aluno); senão, para o painel
        String volta = "leitor".equals(origem) ? "redirect:/catraca/leitor" : "redirect:/";
        CarteirinhaService.Leitura leitura = carteirinhaService.ler(codigo);
        if (!leitura.valido()) {
            redirect.addFlashAttribute("erro", "Catraca Bloqueada: " + leitura.motivoRecusa());
            return volta;
        }
        liberarCatraca(leitura.alunoId(), redirect);
        return volta;
    }

    /** Leitor do QR da carteirinha pela webcam (simulado). O vídeo fica no navegador; só o código lido é enviado. */
    @GetMapping("/catraca/leitor")
    public String leitorCatraca() {
        return "catraca-leitor";
    }

    private String liberarCatraca(Long alunoId, RedirectAttributes redirect) {
        situacaoService.atualizar();

        Aluno a = alunoRepository.findById(alunoId).orElse(null);
        if (a == null) {
            redirect.addFlashAttribute("erro", "Catraca Bloqueada: aluno não encontrado.");
            return "redirect:/";
        }
        String motivo = CarteirinhaService.motivoBloqueio(a,
                matriculaRepository.existsByAlunoIdAndSituacao(alunoId, "ATIVA"),
                mensalidadeRepository.existsByMatriculaAlunoIdAndSituacao(alunoId, "ATRASADA"));
        if (motivo != null) {
            redirect.addFlashAttribute("erro", "Catraca Bloqueada: o aluno " + a.getNome() + " " + motivo + "!");
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
