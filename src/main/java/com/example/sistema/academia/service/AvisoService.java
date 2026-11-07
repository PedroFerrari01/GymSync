package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Aviso;
import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.Equipamento;
import com.example.sistema.academia.model.Matricula;
import com.example.sistema.academia.model.Mensalidade;
import com.example.sistema.academia.repository.*;
import com.example.sistema.academia.view.FormatadorView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AvisoService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final FormatadorView FORMATO = new FormatadorView();

    @Autowired private MensalidadeRepository mensalidadeRepository;
    @Autowired private MatriculaRepository matriculaRepository;
    @Autowired private EquipamentoRepository equipamentoRepository;

    public List<Aviso> paraAdmin() {
        List<Aviso> avisos = new ArrayList<>();
        LocalDate hoje = LocalDate.now();

        // Cada linha leva ao aluno (#aluno-ID) ou à mensalidade (#mensalidade-ID) da própria tela de admin.
        List<Mensalidade> atrasadas = mensalidadeRepository.findBySituacao("ATRASADA");
        atrasadas.stream()
                .sorted(Comparator.comparing(Mensalidade::getVencimento, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(m -> avisos.add(avisoDaMensalidade("danger", m,
                        "está atrasada (venceu em " + m.getVencimento().format(DATA) + ")")));

        mensalidadeRepository.findBySituacao("PENDENTE").stream()
                .filter(m -> m.getVencimento() != null && !m.getVencimento().isBefore(hoje) && !m.getVencimento().isAfter(hoje.plusDays(3)))
                .sorted(Comparator.comparing(Mensalidade::getVencimento))
                .forEach(m -> avisos.add(avisoDaMensalidade("warning", m,
                        "vence em " + m.getVencimento().format(DATA))));

        mensalidadeRepository.findBySituacao("EM_ANALISE").forEach(m -> avisos.add(avisoDaMensalidade("info", m,
                "informou o pagamento e aguarda confirmação")));

        List<Matricula> terminando = matriculaRepository.findBySituacaoAndDataFimBetween("ATIVA", hoje, hoje.plusDays(15));
        for (Matricula m : terminando) {
            Aluno aluno = m.getAluno();
            avisos.add(new Aviso("warning", aluno.getNome() + ": matrícula termina em " + m.getDataFim().format(DATA) + ".",
                    aluno.getId(), null));
        }

        for (Equipamento eq : equipamentoRepository.findAll()) {
            if ("EM_MANUTENCAO".equals(eq.getSituacao())) {
                avisos.add(Aviso.doEquipamento("info", "Equipamento em manutenção: " + eq.getNome() + ".", eq.getId()));
            } else if (eq.getProximaManutencao() != null && !eq.getProximaManutencao().isAfter(hoje.plusDays(7))) {
                String quando = eq.getProximaManutencao().isBefore(hoje) ? "estava prevista para " : "está prevista para ";
                avisos.add(Aviso.doEquipamento("warning", "A manutenção de " + eq.getNome() + " " + quando + eq.getProximaManutencao().format(DATA) + ".", eq.getId()));
            }
        }
        return avisos;
    }

    public List<Aviso> paraAluno(Long alunoId) {
        List<Aviso> avisos = new ArrayList<>();
        LocalDate hoje = LocalDate.now();
        List<Mensalidade> mensalidades = mensalidadeRepository.findByMatriculaAlunoId(alunoId);

        long atrasadas = mensalidades.stream().filter(m -> "ATRASADA".equals(m.getSituacao())).count();
        if (atrasadas > 0) {
            avisos.add(new Aviso("danger", "Você tem " + atrasadas + " mensalidade(s) em atraso. Regularize para liberar a catraca."));
        }

        avisoVencimento(mensalidades, hoje).ifPresent(avisos::add);

        long emAnalise = mensalidades.stream().filter(m -> "EM_ANALISE".equals(m.getSituacao())).count();
        if (emAnalise > 0) {
            avisos.add(new Aviso("info", "Você informou " + emAnalise + " pagamento(s). A recepção vai confirmar em breve."));
        }

        List<Matricula> matriculas = matriculaRepository.findByAlunoId(alunoId);
        Optional<Matricula> ativa = matriculas.stream().filter(m -> "ATIVA".equals(m.getSituacao())).findFirst();
        if (ativa.isEmpty()) {
            avisos.add(new Aviso("danger", "Você não tem matrícula ativa. Fale com a recepção para renovar."));
        } else if (ativa.get().getDataFim() != null && !ativa.get().getDataFim().isAfter(hoje.plusDays(15))) {
            avisos.add(new Aviso("warning", "Seu plano termina em " + ativa.get().getDataFim().format(DATA) + "."));
        }
        return avisos;
    }

    /** Linha do painel do admin ligada à mensalidade e ao aluno dela. */
    private Aviso avisoDaMensalidade(String tipo, Mensalidade m, String situacao) {
        Aluno aluno = m.getMatricula().getAluno();
        String texto = aluno.getNome() + ": mensalidade de R$ " + FORMATO.dinheiro(m.getValor()) + " " + situacao + ".";
        return new Aviso(tipo, texto, aluno.getId(), m.getId());
    }

    /** Mensalidade pendente que vence nos próximos 3 dias (a mais próxima), com o botão de pagar no aviso. */
    public Optional<Aviso> avisoVencimento(List<Mensalidade> mensalidades, LocalDate hoje) {
        return mensalidades.stream()
                .filter(m -> "PENDENTE".equals(m.getSituacao()) && m.getVencimento() != null
                        && !m.getVencimento().isBefore(hoje) && !m.getVencimento().isAfter(hoje.plusDays(3)))
                .min(Comparator.comparing(Mensalidade::getVencimento))
                .map(m -> {
                    long dias = ChronoUnit.DAYS.between(hoje, m.getVencimento());
                    String quando = dias == 0 ? "vence hoje" : dias == 1 ? "vence amanhã" : "vence em " + dias + " dias";
                    String valor = "R$ " + FORMATO.dinheiro(m.getValor());
                    String data = m.getVencimento().format(DATA);
                    return new Aviso("warning", "Sua mensalidade de " + valor + " " + quando + " (" + data + "). Pague pelo Pix para não atrasar.",
                            m.getId(), valor, data);
                });
    }
}
