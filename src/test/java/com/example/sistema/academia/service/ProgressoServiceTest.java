package com.example.sistema.academia.service;

import com.example.sistema.academia.repository.TreinoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressoServiceTest {

    private final TreinoRepository treinos = Mockito.mock(TreinoRepository.class);
    private final ProgressoService service = new ProgressoService();

    ProgressoServiceTest() {
        ReflectionTestUtils.setField(service, "treinoRepository", treinos);
        Mockito.when(treinos.existsByProfessorIdAndAlunoId(2L, 10L)).thenReturn(true);
    }

    @Test
    void adminVeTodos() {
        assertTrue(service.podeVer("ADMIN", null, 99L));
    }

    @Test
    void professorVeSoAlunoComFichaDele() {
        assertTrue(service.podeVer("PROFESSOR", 2L, 10L));
        assertFalse(service.podeVer("PROFESSOR", 2L, 11L));
        assertFalse(service.podeVer("PROFESSOR", 3L, 10L));
    }

    @Test
    void alunoOuSemSessaoNaoVe() {
        assertFalse(service.podeVer("ALUNO", 10L, 10L));
        assertFalse(service.podeVer(null, null, 10L));
        assertFalse(service.podeVer("PROFESSOR", null, 10L));
    }
}
