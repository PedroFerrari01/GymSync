package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.AulaColetiva;
import com.example.sistema.academia.model.AvaliacaoFisica;
import com.example.sistema.academia.model.Matricula;
import com.example.sistema.academia.model.Mensagem;
import com.example.sistema.academia.model.Treino;
import com.example.sistema.academia.repository.AulaColetivaRepository;
import com.example.sistema.academia.repository.AvaliacaoFisicaRepository;
import com.example.sistema.academia.repository.MatriculaRepository;
import com.example.sistema.academia.repository.MensagemRepository;
import com.example.sistema.academia.repository.ReservaAulaRepository;
import com.example.sistema.academia.repository.TreinoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pendências da equipe. Com professorId nulo, é a visão do admin (todos os professores);
 * com professorId, só o que é desse professor.
 */
@Service
public class VisaoEquipeService {

    public static final int DIAS_AVALIACAO = 90;
    public static final int DIAS_FICHA = 60;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM");

    @Autowired private MatriculaRepository matriculaRepository;
    @Autowired private TreinoRepository treinoRepository;
    @Autowired private AvaliacaoFisicaRepository avaliacaoFisicaRepository;
    @Autowired private AulaColetivaRepository aulaColetivaRepository;
    @Autowired private ReservaAulaRepository reservaAulaRepository;
    @Autowired private MensagemRepository mensagemRepository;

    /** Listas já em texto pronto para a tela. Listas vazias = nada pendente. */
    public record Painel(List<String> alunosSemFicha, List<String> fichasSemProfessor,
                         List<String> avaliacoesAtrasadas, long avaliacoesNoMes,
                         List<String> fichasVencidas, List<String> mensagensEspera,
                         List<String> aulasSemana) {}

    public Painel montar(Long professorId, LocalDate hoje) {
        boolean admin = professorId == null;
        List<Treino> todosTreinos = treinoRepository.findAll();
        List<Treino> treinos = admin ? todosTreinos
                : todosTreinos.stream().filter(t -> professorId.equals(t.getProfessor() == null ? null : t.getProfessor().getId())).toList();

        Set<Long> alunosComFicha = todosTreinos.stream().filter(t -> t.getAluno() != null)
                .map(t -> t.getAluno().getId()).collect(Collectors.toSet());
        List<Matricula> ativas = matriculaRepository.findAll().stream()
                .filter(m -> "ATIVA".equals(m.getSituacao()) && m.getAluno() != null).toList();

        // Alunos ativos sem nenhuma ficha (só admin)
        List<String> semFicha = new ArrayList<>();
        if (admin) {
            for (Matricula m : ativas) {
                if (!alunosComFicha.contains(m.getAluno().getId())) {
                    semFicha.add(m.getAluno().getNome() + " · matriculado em " + m.getDataInicio().format(DATA));
                }
            }
        }

        // Fichas sem professor atribuído (só admin)
        List<String> semProfessor = new ArrayList<>();
        if (admin) {
            for (Treino t : todosTreinos) {
                if (t.getProfessor() == null && t.getAluno() != null) {
                    semProfessor.add(t.getAluno().getNome() + " · ficha de " + t.getDataCriacao().format(DATA));
                }
            }
        }

        // Fichas antigas, do escopo (professor ou todos)
        List<String> vencidas = new ArrayList<>();
        LocalDate limiteFicha = hoje.minusDays(DIAS_FICHA);
        for (Treino t : treinos) {
            if (t.getAluno() != null && t.getDataCriacao() != null && t.getDataCriacao().isBefore(limiteFicha)) {
                vencidas.add(t.getAluno().getNome() + " · ficha de " + t.getDataCriacao().format(DATA)
                        + " (há " + ChronoUnit.DAYS.between(t.getDataCriacao(), hoje) + " dias)");
            }
        }

        // Alunos do escopo sem avaliação física há mais de 90 dias
        Map<Long, Aluno> alunosEscopo = new LinkedHashMap<>();
        for (Treino t : treinos) if (t.getAluno() != null) alunosEscopo.putIfAbsent(t.getAluno().getId(), t.getAluno());
        List<String> atrasadas = new ArrayList<>();
        LocalDate limiteAvaliacao = hoje.minusDays(DIAS_AVALIACAO);
        for (Aluno a : alunosEscopo.values()) {
            LocalDate ultima = avaliacaoFisicaRepository.findByAlunoId(a.getId()).stream()
                    .map(AvaliacaoFisica::getDataAvaliacao).filter(Objects::nonNull)
                    .max(LocalDate::compareTo).orElse(null);
            if (ultima == null) atrasadas.add(a.getNome() + " · nunca avaliado");
            else if (ultima.isBefore(limiteAvaliacao)) atrasadas.add(a.getNome() + " · última em " + ultima.format(DATA));
        }

        // Avaliações feitas no mês corrente, do escopo
        long noMes = avaliacaoFisicaRepository.findAll().stream()
                .filter(av -> av.getDataAvaliacao() != null
                        && av.getDataAvaliacao().getMonthValue() == hoje.getMonthValue()
                        && av.getDataAvaliacao().getYear() == hoje.getYear())
                .filter(av -> admin || professorId.equals(av.getProfessorId()))
                .count();

        // Mensagens de alunos ainda sem leitura, do escopo
        List<String> espera = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now();
        for (Mensagem m : mensagemRepository.findAll()) {
            if (m.isLida() || !"ALUNO".equals(m.getRemetente()) || m.getProfessor() == null || m.getAluno() == null) continue;
            if (!admin && !professorId.equals(m.getProfessor().getId())) continue;
            long horas = m.getDataHora() == null ? 0 : ChronoUnit.HOURS.between(m.getDataHora(), agora);
            String quando = horas < 24 ? "há " + Math.max(horas, 0) + " h" : "há " + (horas / 24) + " dia(s)";
            espera.add(m.getAluno().getNome() + " → " + m.getProfessor().getNome() + " · " + quando);
        }

        // Aulas coletivas: próxima ocorrência e vagas, do escopo
        List<String> aulas = new ArrayList<>();
        for (AulaColetiva aula : aulaColetivaRepository.findAll()) {
            if (aula.getDiaSemana() == null || aula.getHorario() == null) continue;
            if (!admin && (aula.getProfessor() == null || !professorId.equals(aula.getProfessor().getId()))) continue;
            LocalDate proxima = hoje.with(TemporalAdjusters.nextOrSame(aula.getDiaSemana()));
            long reservas = reservaAulaRepository.countByAulaIdAndDataAula(aula.getId(), proxima);
            String prof = aula.getProfessor() == null ? "sem professor" : aula.getProfessor().getNome();
            aulas.add(aula.getDiaSemanaPt() + " " + aula.getHorario() + " · " + aula.getNome() + " · " + prof
                    + " · " + reservas + "/" + (aula.getVagas() == null ? "-" : aula.getVagas()) + " vagas");
        }

        return new Painel(semFicha, semProfessor, atrasadas, noMes, vencidas, espera, aulas);
    }
}
