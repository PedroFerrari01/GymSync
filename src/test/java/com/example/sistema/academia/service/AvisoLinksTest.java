package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Aviso;
import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.Matricula;
import com.example.sistema.academia.model.Mensalidade;
import com.example.sistema.academia.repository.EquipamentoRepository;
import com.example.sistema.academia.repository.MatriculaRepository;
import com.example.sistema.academia.repository.MensalidadeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/** Cada aviso do painel do admin leva ao aluno ou à mensalidade certos (ids do servidor). */
@ExtendWith(MockitoExtension.class)
class AvisoLinksTest {

    @Mock private MensalidadeRepository mensalidadeRepository;
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private EquipamentoRepository equipamentoRepository;
    @InjectMocks private AvisoService service;

    private final LocalDate hoje = LocalDate.now();

    private Aluno aluno(long id, String nome) {
        Aluno a = new Aluno();
        a.setId(id);
        a.setNome(nome);
        return a;
    }

    private Mensalidade mensalidade(long id, Aluno aluno, String situacao, LocalDate vencimento) {
        Matricula matricula = new Matricula();
        matricula.setAluno(aluno);
        Mensalidade m = new Mensalidade();
        m.setId(id);
        m.setMatricula(matricula);
        m.setSituacao(situacao);
        m.setVencimento(vencimento);
        m.setValor(new BigDecimal("99.90"));
        return m;
    }

    @Test
    void mensalidadeAtrasadaApontaParaAlunoEMensalidade() {
        Aluno bruno = aluno(7, "Bruno Lima");
        when(mensalidadeRepository.findBySituacao("ATRASADA"))
                .thenReturn(List.of(mensalidade(42, bruno, "ATRASADA", hoje.minusDays(5))));

        List<Aviso> avisos = service.paraAdmin();

        Aviso atrasada = avisos.get(0);
        assertEquals("danger", atrasada.getTipo());
        assertEquals(7L, atrasada.getAlunoId());
        assertEquals(42L, atrasada.getMensalidadeId());
        assertTrue(atrasada.getTexto().startsWith("Bruno Lima: mensalidade de R$ 99,90 está atrasada"));
    }

    @Test
    void vencendoEmAnaliseETerminandoTambemTemLink() {
        Aluno ana = aluno(3, "Ana Souza");
        Aluno caio = aluno(4, "Caio Melo");
        when(mensalidadeRepository.findBySituacao("ATRASADA")).thenReturn(List.of());
        when(mensalidadeRepository.findBySituacao("PENDENTE"))
                .thenReturn(List.of(mensalidade(10, ana, "PENDENTE", hoje.plusDays(2))));
        when(mensalidadeRepository.findBySituacao("EM_ANALISE"))
                .thenReturn(List.of(mensalidade(11, caio, "EM_ANALISE", hoje.minusDays(1))));
        Matricula termina = new Matricula();
        termina.setAluno(aluno(5, "Dani Reis"));
        termina.setDataFim(hoje.plusDays(10));
        when(matriculaRepository.findBySituacaoAndDataFimBetween("ATIVA", hoje, hoje.plusDays(15)))
                .thenReturn(List.of(termina));

        List<Aviso> avisos = service.paraAdmin();

        assertEquals(3, avisos.size());
        assertEquals(10L, avisos.get(0).getMensalidadeId());
        assertEquals(3L, avisos.get(0).getAlunoId());
        assertEquals(11L, avisos.get(1).getMensalidadeId());
        assertEquals(4L, avisos.get(1).getAlunoId());
        assertEquals(5L, avisos.get(2).getAlunoId());
        assertNull(avisos.get(2).getMensalidadeId());
    }

    @Test
    void avisosDoAlunoNuncaTrazemIdsParaLink() {
        Aluno ana = aluno(3, "Ana Souza");
        when(mensalidadeRepository.findByMatriculaAlunoId(3L))
                .thenReturn(List.of(mensalidade(10, ana, "ATRASADA", hoje.minusDays(2))));
        when(matriculaRepository.findByAlunoId(3L)).thenReturn(List.of());

        List<Aviso> avisos = service.paraAluno(3L);

        assertTrue(avisos.stream().allMatch(a -> a.getAlunoId() == null && a.getMensalidadeId() == null));
    }

    @Test
    void avisoSemAlvoContinuaSemLink() {
        Aviso generico = new Aviso("info", "Texto qualquer");
        assertNull(generico.getAlunoId());
        assertNull(generico.getMensalidadeId());
    }
}
