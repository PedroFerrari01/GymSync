package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Acompanhamento;
import com.example.sistema.academia.dto.Aviso;
import com.example.sistema.academia.dto.Notificacao;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SininhoServiceTest {

    private final SininhoService service = new SininhoService();

    private Acompanhamento linha(long id, Integer dias, int total, int adesao, boolean sumido) {
        return new Acompanhamento(id, "Aluno " + id, null, dias, 0, 0, total, adesao, sumido);
    }

    @Test
    void adminLevaAvisoDeMensalidadeParaALinhaDela() {
        List<Notificacao> n = service.paraAdmin(List.of(new Aviso("info", "Bruno: aguarda confirmação.", 7L, 9L)), List.of());
        assertEquals(1, n.size());
        assertEquals("mensalidade-9", n.get(0).alvo());
        assertEquals("btn-aba-painel", n.get(0).aba());
    }

    @Test
    void adminLevaEquipamentoParaAbaDeEquipamentos() {
        List<Notificacao> n = service.paraAdmin(List.of(Aviso.doEquipamento("info", "Esteira em manutenção.", 3L)), null);
        assertEquals("btn-aba-equipamentos", n.get(0).aba());
        assertEquals("equipamento-3", n.get(0).alvo());
    }

    @Test
    void sumidosPassamDoLimiteViramUmaLinhaSo() {
        List<Acompanhamento> linhas = new ArrayList<>();
        for (int i = 1; i <= 7; i++) linhas.add(linha(i, 12, 4, 0, true));
        List<Notificacao> n = service.paraProfessor(linhas);
        assertEquals(SininhoService.LIMITE_SUMIDOS + 1, n.size());
        assertTrue(n.get(n.size() - 1).texto().startsWith("E mais 2"));
    }

    @Test
    void professorAvisaAdesaoBaixaSoDeQuemNaoSumiu() {
        List<Notificacao> n = service.paraProfessor(List.of(
                linha(1, 2, 5, 10, false), linha(2, 1, 5, 80, false), linha(3, 20, 5, 0, true)));
        assertEquals(2, n.size());
        assertTrue(n.get(1).texto().startsWith("1 aluno seguiu"));
    }

    @Test
    void alunoRecebeMensagemEReserva() {
        List<Notificacao> n = service.paraAluno(List.of(), 2, List.of("Spinning, seg 18:00"));
        assertEquals(2, n.size());
        assertEquals("/mensagens", n.get(0).href());
        assertEquals("btn-aba-aulas", n.get(1).aba());
    }

    @Test
    void chaveEhEstavelEListaVaziaNaoQuebra() {
        assertEquals(Notificacao.naAba("info", "x", "a", null).chave(), Notificacao.naAba("info", "x", "b", null).chave());
        assertTrue(service.paraAluno(null, 0, null).isEmpty());
    }
}
