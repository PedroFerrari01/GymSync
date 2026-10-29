package com.example.sistema.academia.controller;

import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.Plano;
import com.example.sistema.academia.model.Professor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.bind.WebDataBinder;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Os cadastros só aceitam os campos do formulário. Campos extras (id, situacao) enviados por fora são ignorados,
 * e os campos do formulário continuam chegando (se faltar um aqui, o cadastro salvaria o dado vazio).
 */
class CamposPermitidosTest {

    private final AcademiaController controller = new AcademiaController();

    @Test
    void alunoRecebeOsCamposDoFormularioEIgnoraOsOutros() {
        Aluno aluno = new Aluno();
        WebDataBinder binder = new WebDataBinder(aluno, "aluno");
        binder.setConversionService(new org.springframework.format.support.DefaultFormattingConversionService());
        controller.camposAluno(binder);
        MutablePropertyValues valores = new MutablePropertyValues();
        valores.add("nome", "Ana");
        valores.add("cpf", "111.444.777-35");
        valores.add("email", "ana@exemplo.com");
        valores.add("dataNascimento", "2000-01-01");
        valores.add("contato", "(11) 91234-5678");
        valores.add("senha", "segredo1");
        valores.add("id", "99");
        valores.add("situacao", "INATIVO");
        binder.bind(valores);

        assertEquals("Ana", aluno.getNome());
        assertEquals("111.444.777-35", aluno.getCpf());
        assertEquals("ana@exemplo.com", aluno.getEmail());
        assertEquals(LocalDate.of(2000, 1, 1), aluno.getDataNascimento());
        assertEquals("(11) 91234-5678", aluno.getContato());
        assertEquals("segredo1", aluno.getSenha());
        assertNull(aluno.getId());
        assertEquals("ATIVO", aluno.getSituacao());
    }

    @Test
    void professorRecebeOsCamposDoFormularioEIgnoraOId() {
        Professor professor = new Professor();
        WebDataBinder binder = new WebDataBinder(professor, "professor");
        controller.camposProfessor(binder);
        MutablePropertyValues valores = new MutablePropertyValues();
        valores.add("nome", "Bruno");
        valores.add("cpf", "111.444.777-35");
        valores.add("email", "bruno@exemplo.com");
        valores.add("contato", "(11) 91234-5678");
        valores.add("especialidade", "Musculação");
        valores.add("senha", "segredo1");
        valores.add("id", "99");
        binder.bind(valores);

        assertEquals("Bruno", professor.getNome());
        assertEquals("Musculação", professor.getEspecialidade());
        assertEquals("segredo1", professor.getSenha());
        assertNull(professor.getId());
    }

    @Test
    void planoRecebeOsCamposDoFormularioEIgnoraOId() {
        Plano plano = new Plano();
        WebDataBinder binder = new WebDataBinder(plano, "plano");
        controller.camposPlano(binder);
        MutablePropertyValues valores = new MutablePropertyValues();
        valores.add("nome", "Mensal");
        valores.add("duracaoMeses", "1");
        valores.add("valor", "99.90");
        valores.add("id", "99");
        binder.bind(valores);

        assertEquals("Mensal", plano.getNome());
        assertEquals(1, plano.getDuracaoMeses());
        assertEquals(new BigDecimal("99.90"), plano.getValor());
        assertNull(plano.getId());
    }
}
