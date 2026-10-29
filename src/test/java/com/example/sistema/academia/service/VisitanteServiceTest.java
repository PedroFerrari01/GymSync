package com.example.sistema.academia.service;

import com.example.sistema.academia.util.Validador;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisitanteServiceTest {

    @Test
    void cpfsDeExemploSaoValidosEDiferentes() {
        Set<String> vistos = new HashSet<>();
        for (int n = 1; n <= 8; n++) {
            String cpf = VisitanteService.cpfDeExemplo(n);
            assertEquals(14, cpf.length(), cpf);
            assertTrue(Validador.cpfValido(cpf), "CPF inválido: " + cpf);
            assertTrue(vistos.add(cpf));
        }
    }
}
