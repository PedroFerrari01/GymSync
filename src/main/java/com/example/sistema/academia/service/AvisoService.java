package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Aviso;
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
import java.util.stream.Collectors;

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

        List<Mensalidade> atrasadas = mensalidadeRepository.findBySituacao("ATRASADA");
        if (!atrasadas.isEmpty()) {
            List<String> nomes = atrasadas.stream().map(m -> m.getMatricula().getAluno().getNome()).distinct().collect(Collectors.toList());
            String lista = nomes.stream().limit(5).collect(Collectors.joining(", "));
            if (nomes.size() > 5) lista += " e mais " + (nomes.size() - 5);
            avisos.add(new Aviso("danger", nomes.size() + " aluno(s) com mensalidade atrasada: " + lista + "."));
        }

        long vencendo = mensalidadeRepository.findBySituacao("PENDENTE").stream()
                .filter(m -> m.getVencimento() != null && !m.getVencimento().isBefore(hoje) && !m.getVencimento().isAfter(hoje.plusDays(3)))
                .count();
        if (vencendo > 0) {
            avisos.add(new Aviso("warning", vencendo + " mensalidade(s) vencem nos próximos 3 dias."));
        }

        long emAnalise = mensalidadeRepository.findBySituacao("EM_ANALISE").size();
        if (emAnalise > 0) {
            avisos.add(new Aviso("info", emAnalise + " pagamento(s) informado(s) pelos alunos aguardando confirmação."));
        }

        List<Matricula> terminando = matriculaRepository.findBySituacaoAndDataFimBetween("ATIVA", hoje, hoje.plusDays(15));
        if (!terminando.isEmpty()) {
            String lista = terminando.stream().map(m -> m.getAluno().getNome()).limit(5).collect(Collectors.joining(", "));
            avisos.add(new Aviso("warning", terminando.size() + " matrícula(s) terminam em até 15 dias: " + lista + "."));
        }

        for (Equipamento eq : equipamentoRepository.findAll()) {
            if ("EM_MANUTENCAO".equals(eq.getSituacao())) {
                avisos.add(new Aviso("info", "Equipamento em manutenção: " + eq.getNome() + "."));
            } else if (eq.getProximaManutencao() != null && !eq.getProximaManutencao().isAfter(hoje.plusDays(7))) {
                String quando = eq.getProximaManutencao().isBefore(hoje) ? "estava prevista para " : "está prevista para ";
                avisos.add(new Aviso("warning", "A manutenção de " + eq.getNome() + " " + quando + eq.getProximaManutencao().format(DATA) + "."));
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
