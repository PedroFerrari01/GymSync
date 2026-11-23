package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Matricula;
import com.example.sistema.academia.model.Mensalidade;
import com.example.sistema.academia.repository.MatriculaRepository;
import com.example.sistema.academia.repository.MensalidadeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Trancamento de matrícula (só o admin). Enquanto a matrícula está TRANCADA:
 * as mensalidades PENDENTES ficam congeladas (o SituacaoService não as marca como atrasadas) e a catraca bloqueia.
 * Ao destrancar, o fim da matrícula e os vencimentos pendentes andam tantos dias quantos ficaram trancados.
 * As regras ficam em métodos estáticos (sem banco) para serem testadas; os métodos com repositório só carregam e salvam.
 */
@Service
public class TrancamentoService {

    public static final String TRANCADA = "TRANCADA";
    public static final int MAX_MOTIVO = 200;
    public static final int MAX_DIAS_PASSADO = 30;
    public static final int MAX_DIAS_RETORNO = 365;
    public static final int AVISO_RETORNO_DIAS = 7;

    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    @Autowired private MatriculaRepository matriculaRepository;
    @Autowired private MensalidadeRepository mensalidadeRepository;

    /** Resultado de uma operação: erro (texto para o usuário) ou mensagem de sucesso. */
    public record Resultado(String erro, String sucesso) {
        public boolean ok() { return erro == null; }
    }

    // ---------------------------------------------------------------- regras puras

    /** Valida e aplica o trancamento na matrícula. Não salva nada. */
    public static Resultado trancar(Matricula m, List<Mensalidade> mensalidades, LocalDate inicio,
                                    LocalDate retorno, String motivo, LocalDate hoje) {
        if (m == null) return erro("Matrícula não encontrada.");
        if (TRANCADA.equals(m.getSituacao())) return erro("Esta matrícula já está trancada. Destranque antes de trancar de novo.");
        if (!"ATIVA".equals(m.getSituacao())) return erro("Só dá para trancar uma matrícula ATIVA (esta está " + m.getSituacao() + ").");
        if (inicio == null) inicio = hoje;
        if (inicio.isAfter(hoje)) return erro("O início do trancamento não pode ser no futuro.");
        if (inicio.isBefore(hoje.minusDays(MAX_DIAS_PASSADO))) return erro("O início do trancamento não pode ser há mais de " + MAX_DIAS_PASSADO + " dias.");
        if (m.getDataInicio() != null && inicio.isBefore(m.getDataInicio())) return erro("O início do trancamento não pode ser antes do início da matrícula.");
        if (retorno != null) {
            if (!retorno.isAfter(inicio)) return erro("O retorno previsto precisa ser depois do início do trancamento.");
            if (retorno.isAfter(inicio.plusDays(MAX_DIAS_RETORNO))) return erro("O retorno previsto não pode passar de " + MAX_DIAS_RETORNO + " dias do início.");
        }
        String mot = limpar(motivo);
        if (mot.length() > MAX_MOTIVO) return erro("O motivo pode ter no máximo " + MAX_MOTIVO + " caracteres.");

        for (Mensalidade men : mensalidades) {
            boolean atrasada = "ATRASADA".equals(men.getSituacao())
                    || ("PENDENTE".equals(men.getSituacao()) && men.getVencimento() != null && men.getVencimento().isBefore(hoje));
            if (atrasada) {
                return erro("Não é possível trancar: há mensalidade em atraso. Regularize o pagamento (ou cancele a matrícula) antes.");
            }
        }

        m.setSituacao(TRANCADA);
        m.setTrancadaDesde(inicio);
        m.setTrancadaAte(retorno);
        m.setMotivoTrancamento(mot.isEmpty() ? null : mot);
        String ate = retorno == null ? "sem data de retorno" : "retorno previsto em " + retorno.format(DIA_MES);
        return new Resultado(null, "Matrícula trancada (" + ate + "). As mensalidades pendentes ficam congeladas.");
    }

    /**
     * Destranca: volta para ATIVA e desloca o fim da matrícula e os vencimentos PENDENTES pelos dias trancados.
     * Mensalidades pagas, em análise ou canceladas não mudam. Não salva nada; as alteradas ficam em {@code alteradas}.
     */
    public static Resultado destrancar(Matricula m, List<Mensalidade> mensalidades, LocalDate hoje, List<Mensalidade> alteradas) {
        if (m == null) return erro("Matrícula não encontrada.");
        if (!TRANCADA.equals(m.getSituacao())) return erro("Esta matrícula não está trancada.");

        long dias = m.getTrancadaDesde() == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(m.getTrancadaDesde(), hoje));
        if (dias > 0) {
            if (m.getDataFim() != null) m.setDataFim(m.getDataFim().plusDays(dias));
            for (Mensalidade men : mensalidades) {
                if ("PENDENTE".equals(men.getSituacao()) && men.getVencimento() != null) {
                    men.setVencimento(men.getVencimento().plusDays(dias));
                    if (alteradas != null) alteradas.add(men);
                }
            }
        }
        m.setSituacao("ATIVA");
        m.setTrancadaAte(hoje); // vira o registro: trancada de desde até hoje
        return new Resultado(null, "Matrícula destrancada. " + (dias == 0
                ? "Ficou trancada por menos de um dia: nada foi deslocado."
                : "Ficou " + dias + (dias == 1 ? " dia" : " dias") + " trancada: o fim da matrícula e os vencimentos pendentes andaram o mesmo tempo."));
    }

    /** Texto do bloqueio da catraca/carteirinha ("está com a matrícula trancada até 05/12"); null se não está trancada. */
    public static String motivoCatraca(Matricula trancada) {
        if (trancada == null || !TRANCADA.equals(trancada.getSituacao())) return null;
        return trancada.getTrancadaAte() == null ? "está com a matrícula trancada"
                : "está com a matrícula trancada até " + trancada.getTrancadaAte().format(DIA_MES);
    }

    /** Aviso discreto do aluno. */
    public static String avisoAluno(Matricula trancada) {
        if (trancada == null || !TRANCADA.equals(trancada.getSituacao())) return null;
        return trancada.getTrancadaAte() == null
                ? "Sua matrícula está trancada. Fale com a recepção para voltar a treinar."
                : "Sua matrícula está trancada até " + trancada.getTrancadaAte().format(DIA_MES) + ".";
    }

    /** Retorno previsto até {@code AVISO_RETORNO_DIAS} dias (ou já passado, ainda trancada): vale avisar o admin. */
    public static boolean retornoProximo(Matricula m, LocalDate hoje) {
        return m != null && TRANCADA.equals(m.getSituacao()) && m.getTrancadaAte() != null
                && !m.getTrancadaAte().isAfter(hoje.plusDays(AVISO_RETORNO_DIAS));
    }

    public static String textoRetorno(Matricula m, LocalDate hoje) {
        long d = ChronoUnit.DAYS.between(hoje, m.getTrancadaAte());
        String quando = d < 0 ? "o retorno estava previsto para " + m.getTrancadaAte().format(DIA_MES)
                : d == 0 ? "volta hoje" : d == 1 ? "volta amanhã" : "volta em " + d + " dias (" + m.getTrancadaAte().format(DIA_MES) + ")";
        return m.getAluno().getNome() + ": matrícula trancada, " + quando + ". Destranque quando o aluno retornar.";
    }

    // ---------------------------------------------------------------- com banco

    public Resultado trancarPorId(Long id, LocalDate inicio, LocalDate retorno, String motivo, LocalDate hoje) {
        Matricula m = matriculaRepository.findById(id).orElse(null);
        if (m == null) return erro("Matrícula não encontrada.");
        Resultado r = trancar(m, mensalidadeRepository.findByMatriculaId(id), inicio, retorno, motivo, hoje);
        if (r.ok()) matriculaRepository.save(m);
        return r;
    }

    public Resultado destrancarPorId(Long id, LocalDate hoje) {
        Matricula m = matriculaRepository.findById(id).orElse(null);
        if (m == null) return erro("Matrícula não encontrada.");
        List<Mensalidade> alteradas = new java.util.ArrayList<>();
        Resultado r = destrancar(m, mensalidadeRepository.findByMatriculaId(id), hoje, alteradas);
        if (r.ok()) {
            matriculaRepository.save(m);
            mensalidadeRepository.saveAll(alteradas);
        }
        return r;
    }

    public Optional<Matricula> trancadaDoAluno(Long alunoId) {
        return matriculaRepository.findFirstByAlunoIdAndSituacao(alunoId, TRANCADA);
    }

    /** Motivo de bloqueio da catraca por trancamento; null se o aluno não tem matrícula trancada. */
    public String motivoCatracaDoAluno(Long alunoId) {
        return motivoCatraca(trancadaDoAluno(alunoId).orElse(null));
    }

    public List<Matricula> trancadas() {
        return matriculaRepository.findBySituacao(TRANCADA);
    }

    // ---------------------------------------------------------------- util

    private static Resultado erro(String texto) { return new Resultado(texto, null); }

    private static String limpar(String s) {
        return s == null ? "" : s.replaceAll("[\\p{Cntrl}]", " ").trim();
    }
}
