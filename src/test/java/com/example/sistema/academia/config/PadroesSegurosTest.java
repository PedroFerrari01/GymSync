package com.example.sistema.academia.config;

import com.example.sistema.academia.controller.AcademiaController;
import com.example.sistema.academia.controller.AuthController;
import com.example.sistema.academia.service.RecuperacaoSenhaService;
import com.example.sistema.academia.service.SenhaService;
import com.example.sistema.academia.service.VisitanteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Garante que, sem configuração nenhuma, o sistema nasce fechado (os padrões do código são os seguros). */
class PadroesSegurosTest {

    private static String valor(Class<?> classe, String campo) throws Exception {
        Field f = classe.getDeclaredField(campo);
        return f.getAnnotation(Value.class).value();
    }

    @Test
    void senhaDoAdminNaoTemPadrao() throws Exception {
        // sem ":valor" no fim: sem a propriedade o sistema não sobe (nada de admin/123 escondido no código)
        assertEquals("${gymsync.admin.senha}", valor(AuthController.class, "adminSenha"));
    }

    @Test
    void recuperacaoDeSenhaNasceComLinkPorEmail() throws Exception {
        assertEquals("${gymsync.recuperacao-por-email:true}", valor(AuthController.class, "recuperacaoPorEmail"));
        assertEquals("${gymsync.recuperacao-por-email:true}", valor(AcademiaController.class, "recuperacaoPorEmail"));
    }

    @Test
    void senhasFortesNascemLigadas() throws Exception {
        Constructor<?> c = SenhaService.class.getConstructors()[0];
        Value v = (Value) c.getParameters()[0].getAnnotation(Value.class);
        assertNotNull(v);
        assertEquals("${gymsync.senhas-fortes:true}", v.value());
        assertTrue(new SenhaService(true).fortes());
    }

    @Test
    void visitanteNasceDesligado() throws Exception {
        assertEquals("${gymsync.visitante:false}", valor(VisitanteService.class, "habilitado"));
        assertFalse(new VisitanteService().habilitado());
    }
}
