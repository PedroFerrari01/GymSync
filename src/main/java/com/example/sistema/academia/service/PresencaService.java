package com.example.sistema.academia.service;

import com.example.sistema.academia.model.AulaColetiva;
import com.example.sistema.academia.model.PresencaAula;
import com.example.sistema.academia.model.ReservaAula;
import com.example.sistema.academia.repository.AulaColetivaRepository;
import com.example.sistema.academia.repository.PresencaAulaRepository;
import com.example.sistema.academia.repository.ReservaAulaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lista de chamada das aulas coletivas: quem o professor ou o admin pode marcar e os resumos de faltas. */
@Service
public class PresencaService {

    /** Quantos dias para trás a chamada fica aberta. */
    public static final int DIAS_CHAMADA = 14;

    private final AulaColetivaRepository aulaRepository;
    private final ReservaAulaRepository reservaRepository;
    private final PresencaAulaRepository presencaRepository;

    public PresencaService(AulaColetivaRepository aulaRepository, ReservaAulaRepository reservaRepository,
                           PresencaAulaRepository presencaRepository) {
        this.aulaRepository = aulaRepository;
        this.reservaRepository = reservaRepository;
        this.presencaRepository = presencaRepository;
    }

    /** Um aluno na chamada: {@code presente} é null enquanto ninguém marcou. */
    public record Chamada(Long reservaId, String aluno, Boolean presente) {}

    /** Uma aula numa data, com a lista de chamada. */
    public record Sessao(Long aulaId, String chave, String aula, String professor, LocalDate data, String dia,
                         List<Chamada> alunos, int marcados) {}

    /** Aulas dos últimos dias (até hoje) que tiveram reserva; {@code professorId} null = todas (admin). */
    public List<Sessao> sessoes(Long professorId, LocalDate hoje) {
        List<Sessao> sessoes = new ArrayList<>();
        for (AulaColetiva aula : aulaRepository.findAll()) {
            if (aula.getDiaSemana() == null) continue;
            if (professorId != null && !professorId.equals(aula.getProfessor().getId())) continue;
            for (int i = 0; i <= DIAS_CHAMADA; i++) {
                LocalDate data = hoje.minusDays(i);
                if (data.getDayOfWeek() != aula.getDiaSemana()) continue;
                List<ReservaAula> reservas = reservaRepository.findByAulaIdAndDataAula(aula.getId(), data);
                if (reservas.isEmpty()) continue;
                Map<Long, Boolean> marcas = new HashMap<>();
                for (PresencaAula p : presencaRepository.findByAulaIdAndDataAula(aula.getId(), data)) {
                    marcas.put(p.getAluno().getId(), p.isPresente());
                }
                List<Chamada> alunos = new ArrayList<>();
                for (ReservaAula r : reservas) {
                    alunos.add(new Chamada(r.getId(), r.getAluno().getNome(), marcas.get(r.getAluno().getId())));
                }
                alunos.sort(Comparator.comparing(Chamada::aluno, String.CASE_INSENSITIVE_ORDER));
                int marcados = (int) alunos.stream().filter(c -> c.presente() != null).count();
                sessoes.add(new Sessao(aula.getId(), aula.getId() + "_" + data, aula.getNome(), aula.getProfessor().getNome(),
                        data, aula.getDiaSemanaPt(), alunos, marcados));
            }
        }
        sessoes.sort(Comparator.comparing(Sessao::data).reversed().thenComparing(Sessao::aula));
        return sessoes;
    }

    /** Grava (ou troca) a marcação. Devolve o motivo da recusa, ou null se deu certo. */
    public String marcar(Long reservaId, boolean presente, String perfil, Long usuarioId, String quem, LocalDate hoje) {
        ReservaAula reserva = reservaRepository.findById(reservaId).orElse(null);
        if (reserva == null) return "Reserva não encontrada.";
        AulaColetiva aula = reserva.getAula();
        if (!"ADMIN".equals(perfil)
                && !("PROFESSOR".equals(perfil) && usuarioId != null && usuarioId.equals(aula.getProfessor().getId()))) {
            return "Você só pode marcar presença nas suas aulas.";
        }
        LocalDate data = reserva.getDataAula();
        if (data == null || data.isAfter(hoje)) return "Esta aula ainda não aconteceu.";
        PresencaAula p = presencaRepository
                .findFirstByAulaIdAndAlunoIdAndDataAula(aula.getId(), reserva.getAluno().getId(), data)
                .orElseGet(PresencaAula::new);
        p.setAula(aula);
        p.setAluno(reserva.getAluno());
        p.setDataAula(data);
        p.setPresente(presente);
        p.setRegistradoPor(quem);
        p.setRegistradoEm(LocalDateTime.now());
        presencaRepository.save(p);
        return null;
    }

    /** Faltas e % dos últimos 30 dias; professor vê só as aulas dele (professorId), admin vê todas (null). */
    public List<PresencaCalculo.Linha> relatorio(Long professorId, LocalDate hoje) {
        List<PresencaCalculo.Marca> marcas = new ArrayList<>();
        for (PresencaAula p : presencaRepository.findByDataAulaGreaterThanEqual(hoje.minusDays(PresencaCalculo.JANELA_DIAS))) {
            if (professorId != null && !professorId.equals(p.getAula().getProfessor().getId())) continue;
            marcas.add(new PresencaCalculo.Marca(p.getAluno().getId(), p.getAluno().getNome(), p.getDataAula(), p.isPresente()));
        }
        return PresencaCalculo.ranking(marcas, hoje, PresencaCalculo.JANELA_DIAS);
    }

    /** Resumo do próprio aluno nos últimos 30 dias. */
    public PresencaCalculo.Linha doAluno(Long alunoId, LocalDate hoje) {
        List<PresencaCalculo.Marca> marcas = new ArrayList<>();
        for (PresencaAula p : presencaRepository.findByAlunoId(alunoId)) {
            marcas.add(new PresencaCalculo.Marca(alunoId, p.getAluno().getNome(), p.getDataAula(), p.isPresente()));
        }
        return PresencaCalculo.doAluno(alunoId, marcas, hoje, PresencaCalculo.JANELA_DIAS);
    }

    public void excluirDaAula(Long aulaId) {
        presencaRepository.deleteAll(presencaRepository.findByAulaId(aulaId));
    }

    public void excluirDoAluno(Long alunoId) {
        presencaRepository.deleteAll(presencaRepository.findByAlunoId(alunoId));
    }
}
