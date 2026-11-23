package com.example.sistema.academia.dto;

/** Meta do aluno já com o progresso calculado (nada disso é gravado). */
public record MetaProgresso(Long id, String tipo, String titulo, String icone, String atualTexto, String alvoTexto,
                            int percentual, boolean concluida, boolean semDados, String prazoTexto, boolean atrasada,
                            String alvoCampo, String prazoCampo) {
}
