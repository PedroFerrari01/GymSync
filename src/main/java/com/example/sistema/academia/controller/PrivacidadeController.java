package com.example.sistema.academia.controller;

import com.example.sistema.academia.model.*;
import com.example.sistema.academia.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** LGPD: política de privacidade, aceite do aluno e "baixar meus dados" (acesso e portabilidade). */
@Controller
public class PrivacidadeController {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired private AlunoRepository alunoRepository;
    @Autowired private MatriculaRepository matriculaRepository;
    @Autowired private MensalidadeRepository mensalidadeRepository;
    @Autowired private EntradaRepository entradaRepository;
    @Autowired private AvaliacaoFisicaRepository avaliacaoFisicaRepository;
    @Autowired private TreinoRepository treinoRepository;
    @Autowired private ExercicioTreinoRepository exercicioTreinoRepository;
    @Autowired private RegistroExercicioRepository registroExercicioRepository;
    @Autowired private ReservaAulaRepository reservaAulaRepository;
    @Autowired private MensagemRepository mensagemRepository;

    @GetMapping("/privacidade")
    public String politica(Model model, HttpSession session) {
        model.addAttribute("logado", session.getAttribute("perfil") != null);
        return "privacidade";
    }

    @PostMapping("/aluno/aceitar-privacidade")
    public String aceitar(HttpSession session, RedirectAttributes redirect) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        Aluno aluno = usuarioId != null ? alunoRepository.findById(usuarioId).orElse(null) : null;
        if (aluno != null && aluno.getAceitePrivacidade() == null) {
            aluno.setAceitePrivacidade(LocalDateTime.now());
            alunoRepository.save(aluno);
            redirect.addFlashAttribute("sucesso", "Obrigado! Você aceitou a política de privacidade.");
        }
        return "redirect:/";
    }

    private static String data(LocalDate d) { return d == null ? null : d.format(DATA); }
    private static String dataHora(LocalDateTime d) { return d == null ? null : d.format(DATA_HORA); }

    /** Tudo o que o GymSync guarda sobre o aluno logado, em JSON. A senha (nem o hash) não vai. */
    @GetMapping("/aluno/meus-dados")
    public ResponseEntity<Map<String, Object>> meusDados(HttpSession session) {
        Long id = (Long) session.getAttribute("usuarioId");
        Aluno a = id != null ? alunoRepository.findById(id).orElse(null) : null;
        if (a == null) return ResponseEntity.notFound().build();

        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("geradoEm", dataHora(LocalDateTime.now()));
        dados.put("sistema", "GymSync");

        Map<String, Object> cadastro = new LinkedHashMap<>();
        cadastro.put("nome", a.getNome());
        cadastro.put("cpf", a.getCpf());
        cadastro.put("email", a.getEmail());
        cadastro.put("telefone", a.getContato());
        cadastro.put("nascimento", data(a.getDataNascimento()));
        cadastro.put("situacao", a.getSituacao());
        cadastro.put("aceitePrivacidade", dataHora(a.getAceitePrivacidade()));
        dados.put("cadastro", cadastro);

        dados.put("matriculas", matriculaRepository.findByAlunoId(id).stream().map(m -> {
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("plano", m.getPlano() != null ? m.getPlano().getNome() : null);
            l.put("inicio", data(m.getDataInicio()));
            l.put("fim", data(m.getDataFim()));
            l.put("situacao", m.getSituacao());
            return l;
        }).toList());

        dados.put("mensalidades", mensalidadeRepository.findByMatriculaAlunoId(id).stream().map(m -> {
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("valor", m.getValor());
            l.put("vencimento", data(m.getVencimento()));
            l.put("situacao", m.getSituacao());
            l.put("pagoEm", data(m.getDataPagamento()));
            return l;
        }).toList());

        dados.put("entradasNaCatraca", entradaRepository.findByAlunoIdOrderByDataHoraEntradaDesc(id).stream()
                .map(e -> dataHora(e.getDataHoraEntrada())).toList());

        dados.put("avaliacoesFisicas", avaliacaoFisicaRepository.findByAlunoIdOrderByDataAvaliacaoAscIdAsc(id).stream().map(av -> {
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("data", data(av.getDataAvaliacao()));
            l.put("pesoKg", av.getPeso());
            l.put("alturaM", av.getAltura());
            l.put("gorduraPercentual", av.getPercentualGordura());
            l.put("cinturaCm", av.getCintura());
            l.put("quadrilCm", av.getQuadril());
            l.put("peitoCm", av.getPeito());
            l.put("bracoCm", av.getBraco());
            l.put("coxaCm", av.getCoxa());
            l.put("observacao", av.getObservacao());
            return l;
        }).toList());

        dados.put("fichasDeTreino", treinoRepository.findByAlunoId(id).stream().map(t -> {
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("ficha", t.getDescricao());
            l.put("professor", t.getProfessor() != null ? t.getProfessor().getNome() : null);
            l.put("criadaEm", data(t.getDataCriacao()));
            l.put("exercicios", exercicioTreinoRepository.findByTreinoId(t.getId()).stream()
                    .map(ex -> ex.getExercicio() + " - " + ex.getSeries() + "x" + ex.getRepeticoes()).toList());
            return l;
        }).toList());

        dados.put("exerciciosRegistrados", registroExercicioRepository.findByAlunoId(id).stream().map(r -> {
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("data", data(r.getDataRegistro()));
            l.put("exercicio", r.getExercicio() != null ? r.getExercicio().getExercicio() : null);
            l.put("cargaKg", r.getCarga());
            l.put("observacao", r.getObservacao());
            return l;
        }).toList());

        dados.put("reservasDeAulas", reservaAulaRepository.findByAlunoId(id).stream().map(r -> {
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("aula", r.getAula() != null ? r.getAula().getNome() : null);
            l.put("data", data(r.getDataAula()));
            return l;
        }).toList());

        dados.put("mensagens", mensagemRepository.findByAlunoId(id).stream().map(m -> {
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("dataHora", dataHora(m.getDataHora()));
            l.put("de", "ALUNO".equals(m.getRemetente()) ? "você" : (m.getProfessor() != null ? m.getProfessor().getNome() : "professor"));
            l.put("texto", m.getTexto());
            return l;
        }).toList());

        String arquivo = "gymsync-meus-dados-" + LocalDate.now() + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(arquivo).build().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados);
    }
}
