package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.AulaColetiva;
import com.example.sistema.academia.model.PresencaAula;
import com.example.sistema.academia.model.ReservaAula;
import com.example.sistema.academia.repository.AlunoRepository;
import com.example.sistema.academia.repository.AulaColetivaRepository;
import com.example.sistema.academia.repository.PresencaAulaRepository;
import com.example.sistema.academia.repository.ReservaAulaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Chamada de exemplo do visitante: reservas e presenças passadas da Ana (quase total) e do Bruno (2 faltas).
 * Só roda com o visitante ligado, com os alunos de exemplo existindo e com a tabela de presenças vazia.
 */
@Component
public class PresencaExemplo {

    private static final Logger log = LoggerFactory.getLogger(PresencaExemplo.class);

    @Value("${gymsync.visitante:false}") private boolean habilitado;

    private final AlunoRepository alunoRepository;
    private final AulaColetivaRepository aulaRepository;
    private final ReservaAulaRepository reservaRepository;
    private final PresencaAulaRepository presencaRepository;

    public PresencaExemplo(AlunoRepository alunoRepository, AulaColetivaRepository aulaRepository,
                           ReservaAulaRepository reservaRepository, PresencaAulaRepository presencaRepository) {
        this.alunoRepository = alunoRepository;
        this.aulaRepository = aulaRepository;
        this.reservaRepository = reservaRepository;
        this.presencaRepository = presencaRepository;
    }

    /** Chamado pelo VisitanteService.aoSubir, depois de criar os alunos de exemplo. */
    @Transactional
    public void criar() {
        if (!habilitado || presencaRepository.count() > 0) return;
        Optional<Aluno> ana = alunoRepository.findFirstByCpfOrEmailIgnoreCase(VisitanteService.EMAIL_ALUNO, VisitanteService.EMAIL_ALUNO);
        Optional<Aluno> bruno = alunoRepository.findFirstByCpfOrEmailIgnoreCase("bruno.lima@exemplo.gymsync", "bruno.lima@exemplo.gymsync");
        if (ana.isEmpty() || bruno.isEmpty()) return;
        LocalDate hoje = LocalDate.now();
        List<AulaColetiva> aulas = aulaRepository.findAll();
        log.info("Criando a chamada de exemplo das aulas coletivas.");
        for (AulaColetiva aula : aulas) {
            if (aula.getDiaSemana() == null) continue;
            String nome = aula.getNome();
            // semanas passadas: k = 1 é a aula mais recente que já aconteceu
            for (int k = 1; k <= 5; k++) {
                LocalDate data = ultimaOcorrencia(aula, hoje).minusWeeks(k - 1L);
                if ("Spinning".equals(nome) || "Funcional".equals(nome)) {
                    marcar(aula, ana.get(), data, true);                       // Ana: presença total nestas aulas
                    if ("Spinning".equals(nome) && k <= 4) {
                        marcar(aula, bruno.get(), data, k == 2 || k == 4);     // Bruno: falta nas semanas 1 e 3
                    }
                } else if ("Yoga".equals(nome) && k <= 3) {
                    marcar(aula, ana.get(), data, k != 3);                     // Ana: 1 falta na Yoga
                    marcar(aula, bruno.get(), data, true);                     // Bruno: presente na Yoga
                }
            }
        }
    }

    /** Data mais recente (hoje ou antes) em que a aula aconteceu; hoje só conta se já passou do horário. */
    static LocalDate ultimaOcorrencia(AulaColetiva aula, LocalDate hoje) {
        LocalDate data = hoje;
        while (data.getDayOfWeek() != aula.getDiaSemana()) data = data.minusDays(1);
        if (data.equals(hoje) && aula.getHorario() != null && LocalDateTime.now().toLocalTime().isBefore(aula.getHorario())) {
            data = data.minusWeeks(1);
        }
        return data;
    }

    private void marcar(AulaColetiva aula, Aluno aluno, LocalDate data, boolean presente) {
        if (reservaRepository.findFirstByAulaIdAndAlunoIdAndDataAula(aula.getId(), aluno.getId(), data).isEmpty()) {
            ReservaAula r = new ReservaAula();
            r.setAula(aula);
            r.setAluno(aluno);
            r.setDataAula(data);
            reservaRepository.save(r);
        }
        PresencaAula p = new PresencaAula();
        p.setAula(aula);
        p.setAluno(aluno);
        p.setDataAula(data);
        p.setPresente(presente);
        p.setRegistradoPor(aula.getProfessor().getNome());
        p.setRegistradoEm(data.atTime(aula.getHorario() != null ? aula.getHorario().plusHours(1) : java.time.LocalTime.NOON));
        presencaRepository.save(p);
    }
}
