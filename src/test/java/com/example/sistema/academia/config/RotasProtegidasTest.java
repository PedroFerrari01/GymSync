package com.example.sistema.academia.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Garante que toda rota nova entre na lista do WebConfig.
 * Se alguém criar um @PostMapping e esquecer de proteger, este teste falha e mostra qual rota ficou aberta.
 */
class RotasProtegidasTest {

    /** Rotas que precisam funcionar sem login. Qualquer outra tem de passar pelo interceptador. */
    private static final Set<String> ROTAS_PUBLICAS = Set.of("/", "/login", "/logout", "/recuperar-senha");

    /** Expõe os interceptadores registrados, que no InterceptorRegistry são protegidos. */
    private static class RegistroDeTeste extends InterceptorRegistry {
        List<Object> registrados() {
            return getInterceptors();
        }
    }

    @Test
    void todaRotaQueNaoEPublicaPassaPeloInterceptador() throws Exception {
        WebConfig config = new WebConfig();
        ReflectionTestUtils.setField(config, "autenticacaoInterceptor", new AutenticacaoInterceptor());
        RegistroDeTeste registro = new RegistroDeTeste();
        config.addInterceptors(registro);
        MappedInterceptor protecao = (MappedInterceptor) registro.registrados().get(0);

        // Confere que o teste sabe reconhecer uma rota fora da lista
        assertFalse(protegida(protecao, "/rota-inventada/salvar"));

        List<String> abertas = new ArrayList<>();
        for (String rota : rotasDosControllers()) {
            if (ROTAS_PUBLICAS.contains(rota)) continue;
            if (!protegida(protecao, rota.replaceAll("\\{[^}]+}", "1"))) abertas.add(rota);
        }
        assertTrue(abertas.isEmpty(), "Rotas sem proteção (adicione no WebConfig e no AutenticacaoInterceptor): " + abertas);
    }

    private boolean protegida(MappedInterceptor protecao, String caminho) {
        MockHttpServletRequest requisicao = new MockHttpServletRequest("POST", caminho);
        ServletRequestPathUtils.parseAndCache(requisicao);
        return protecao.matches(requisicao);
    }

    private List<String> rotasDosControllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        List<String> rotas = new ArrayList<>();
        for (BeanDefinition bean : scanner.findCandidateComponents("com.example.sistema.academia.controller")) {
            for (Method metodo : Class.forName(bean.getBeanClassName()).getDeclaredMethods()) {
                RequestMapping mapeamento = AnnotatedElementUtils.findMergedAnnotation(metodo, RequestMapping.class);
                if (mapeamento != null) rotas.addAll(List.of(mapeamento.path()));
            }
        }
        assertTrue(rotas.size() > 10, "Nenhuma rota encontrada: o scanner de controllers não funcionou.");
        return rotas;
    }
}
