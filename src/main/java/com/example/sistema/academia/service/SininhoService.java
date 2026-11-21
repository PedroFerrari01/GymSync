package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Acompanhamento;
import com.example.sistema.academia.dto.Aviso;
import com.example.sistema.academia.dto.Notificacao;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/**
 * Monta a lista do sininho a partir do que o sistema já calcula (avisos, acompanhamento, mensagens, reservas).
 * Não consulta o banco: o controlador entrega os dados prontos.
 */
@Service
public class SininhoService {

    public static final int LIMITE_SUMIDOS = 5;
    public static final int ADESAO_BAIXA = 30;

    public List<Notificacao> paraAdmin(List<Aviso> avisos, List<Acompanhamento> acompanhamento) {
        List<Notificacao> lista = new ArrayList<>();
        if (avisos != null) {
            for (Aviso av : avisos) {
                if (av.getMensalidadeId() != null) {
                    lista.add(Notificacao.naAba(av.getTipo(), av.getTexto(), "btn-aba-painel", "mensalidade-" + av.getMensalidadeId()));
                } else if (av.getEquipamentoId() != null) {
                    lista.add(Notificacao.naAba(av.getTipo(), av.getTexto(), "btn-aba-equipamentos", "equipamento-" + av.getEquipamentoId()));
                } else if (av.getAlunoId() != null) {
                    lista.add(Notificacao.naAba(av.getTipo(), av.getTexto(), "btn-aba-painel", "aluno-" + av.getAlunoId()));
                } else {
                    lista.add(Notificacao.naAba(av.getTipo(), av.getTexto(), "btn-aba-painel", null));
                }
            }
        }
        lista.addAll(sumidos(acompanhamento));
        return lista;
    }

    public List<Notificacao> paraProfessor(List<Acompanhamento> acompanhamento) {
        List<Notificacao> lista = new ArrayList<>(sumidos(acompanhamento));
        if (acompanhamento != null) {
            long baixa = acompanhamento.stream()
                    .filter(l -> !l.isSumido() && l.getTotalExercicios() > 0 && l.getAdesao() < ADESAO_BAIXA).count();
            if (baixa > 0) {
                lista.add(Notificacao.naAba("info", baixa + (baixa == 1 ? " aluno seguiu" : " alunos seguiram")
                        + " menos de " + ADESAO_BAIXA + "% da ficha nesta semana.", "btn-aba-treinos", null));
            }
        }
        return lista;
    }

    public List<Notificacao> paraAluno(List<Aviso> avisos, long mensagensNaoLidas, List<String> aulasReservadas) {
        List<Notificacao> lista = new ArrayList<>();
        if (avisos != null) {
            for (Aviso av : avisos) lista.add(Notificacao.naAba(av.getTipo(), av.getTexto(), "btn-aba-portal", null));
        }
        if (mensagensNaoLidas > 0) {
            lista.add(Notificacao.paraPagina("info", "Você tem " + mensagensNaoLidas
                    + (mensagensNaoLidas == 1 ? " mensagem nova" : " mensagens novas") + " do professor.", "/mensagens"));
        }
        if (aulasReservadas != null) {
            for (String aula : aulasReservadas) {
                lista.add(Notificacao.naAba("info", "Aula reservada: " + aula + ".", "btn-aba-aulas", null));
            }
        }
        return lista;
    }

    private List<Notificacao> sumidos(List<Acompanhamento> acompanhamento) {
        List<Notificacao> lista = new ArrayList<>();
        if (acompanhamento == null) return lista;
        List<Acompanhamento> sumidos = acompanhamento.stream().filter(Acompanhamento::isSumido).toList();
        for (Acompanhamento l : sumidos.stream().limit(LIMITE_SUMIDOS).toList()) {
            String quando = l.getDiasSemTreinar() == null ? "nunca passou pela catraca"
                    : "sem treinar há " + l.getDiasSemTreinar() + " dias";
            lista.add(Notificacao.naAba("warning", l.getNome() + " está " + quando + ".", "btn-aba-treinos", null));
        }
        if (sumidos.size() > LIMITE_SUMIDOS) {
            int resto = sumidos.size() - LIMITE_SUMIDOS;
            lista.add(Notificacao.naAba("warning", "E mais " + resto + (resto == 1 ? " aluno sumido." : " alunos sumidos."), "btn-aba-treinos", null));
        }
        return lista;
    }
}
