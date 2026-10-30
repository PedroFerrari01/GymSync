package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Comparacao;
import com.example.sistema.academia.model.AvaliacaoFisica;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvolucaoServiceTest {

    private final EvolucaoService service = new EvolucaoService();

    private AvaliacaoFisica avaliacao(LocalDate data, Double peso, Double gordura, Double cintura, Double braco) {
        AvaliacaoFisica a = new AvaliacaoFisica();
        a.setDataAvaliacao(data);
        a.setPeso(peso);
        a.setPercentualGordura(gordura);
        a.setCintura(cintura);
        a.setBraco(braco);
        return a;
    }

    @Test
    void comparaPrimeiraComUltimaMedida() {
        List<AvaliacaoFisica> lista = List.of(
                avaliacao(LocalDate.of(2026, 7, 1), 82.0, 24.0, 90.0, 33.0),
                avaliacao(LocalDate.of(2026, 8, 1), 80.0, null, 88.0, 34.0),
                avaliacao(LocalDate.of(2026, 9, 1), 79.5, 21.5, 92.0, 35.0));

        List<Comparacao> linhas = service.comparacao(lista);
        // Quadril, peito e coxa nunca foram medidos: ficam de fora
        assertEquals(List.of("Peso", "Gordura", "Cintura", "Braço"), linhas.stream().map(Comparacao::getRotulo).toList());

        Comparacao gordura = linhas.get(1);
        assertEquals("24,0%", gordura.getPrimeira());
        assertEquals("21,5%", gordura.getUltima());
        assertEquals("-2,5%", gordura.getDiferenca());
        assertEquals("melhorou", gordura.getClasse());

        assertEquals("+2,0 cm", linhas.get(2).getDiferenca());
        assertEquals("piorou", linhas.get(2).getClasse());
        // Peso e braço dependem do objetivo: sem cor
        assertEquals("neutro", linhas.get(0).getClasse());
        assertEquals("neutro", linhas.get(3).getClasse());
    }

    @Test
    void precisaDeDuasMedidas() {
        List<AvaliacaoFisica> uma = List.of(avaliacao(LocalDate.of(2026, 7, 1), 82.0, 24.0, null, null));
        assertTrue(service.comparacao(uma).isEmpty());
        assertEquals(1, service.barrasGordura(uma).size());
        assertEquals("24,0%", service.barrasGordura(uma).get(0).getTexto());
    }
}
