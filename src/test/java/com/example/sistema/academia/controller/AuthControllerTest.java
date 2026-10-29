package com.example.sistema.academia.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** O log de login não pode guardar o CPF ou o e-mail inteiro. */
class AuthControllerTest {

    @Test
    void mascaraCpfDeixandoSoOsDoisUltimosDigitos() {
        assertEquals("CPF ***52", AuthController.mascarar("513.790.480-52"));
    }

    @Test
    void mascaraEmailDeixandoSoOComeco() {
        assertEquals("jo***@teste.com", AuthController.mascarar("joao@teste.com"));
    }

    @Test
    void valorVazioOuSemDigitos() {
        assertEquals("(vazio)", AuthController.mascarar(null));
        assertEquals("(vazio)", AuthController.mascarar("  "));
        assertEquals("***", AuthController.mascarar("admin"));
    }
}
