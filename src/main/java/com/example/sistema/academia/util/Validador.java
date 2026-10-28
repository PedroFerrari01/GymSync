package com.example.sistema.academia.util;

public class Validador {

    public static boolean cpfValido(String cpf) {
        if (cpf == null) return false;
        String n = cpf.replaceAll("\\D", "");
        if (n.length() != 11 || n.chars().distinct().count() == 1) return false;
        for (int t = 9; t < 11; t++) {
            int soma = 0;
            for (int i = 0; i < t; i++) {
                soma += (n.charAt(i) - '0') * (t + 1 - i);
            }
            int dv = (soma * 10) % 11;
            if (dv == 10) dv = 0;
            if (dv != n.charAt(t) - '0') return false;
        }
        return true;
    }

    public static boolean emailValido(String email) {
        return email != null && email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    public static boolean telefoneValido(String telefone) {
        if (telefone == null) return false;
        int digitos = telefone.replaceAll("\\D", "").length();
        return digitos == 10 || digitos == 11;
    }
}
