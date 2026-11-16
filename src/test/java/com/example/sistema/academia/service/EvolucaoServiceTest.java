package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Comparacao;
import com.example.sistema.academia.dto.MapaCorpo;
import com.example.sistema.academia.dto.MedidaCorpo;
import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.AvaliacaoFisica;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void mapaDoCorpoUsaAMesmaRegraDeCores() {
        List<AvaliacaoFisica> lista = List.of(
                avaliacao(LocalDate.of(2026, 7, 1), 82.0, 24.0, 90.0, 33.0),
                avaliacao(LocalDate.of(2026, 8, 1), 80.0, null, 88.0, 34.0),
                avaliacao(LocalDate.of(2026, 9, 1), 79.5, 21.5, 92.0, 35.0));
        lista.get(2).setAltura(1.8);
        lista.get(2).setCoxa(58.0);

        MapaCorpo mapa = service.mapaCorpo(7L, "Aluno", lista);
        assertEquals(3, mapa.getAvaliacoes());
        assertEquals("01/07/2026", mapa.getDataPrimeira());
        assertEquals("01/09/2026", mapa.getDataUltima());
        assertEquals(180.0, mapa.getAlturaCm());
        // Quadril e peito nunca foram medidos; o IMC só existe na avaliação que tem altura
        assertEquals(List.of("peso", "gordura", "cintura", "braco", "coxa", "imc"),
                mapa.getMedidas().stream().map(MedidaCorpo::getChave).toList());

        MedidaCorpo cintura = mapa.getMedidas().get(2);
        assertEquals("92,0 cm", cintura.getUltima());
        assertEquals(90.0, cintura.getPrimeiraNumero());
        assertEquals("+2,0 cm", cintura.getDiferenca());
        assertEquals("piorou", cintura.getClasse());
        assertEquals("melhorou", mapa.getMedidas().get(1).getClasse());
        assertEquals("neutro", mapa.getMedidas().get(3).getClasse());

        // Medida feita uma vez só: aparece o valor, sem diferença e sem cor
        MedidaCorpo coxa = mapa.getMedidas().get(4);
        assertEquals("58,0 cm", coxa.getUltima());
        assertNull(coxa.getDiferenca());
        assertEquals("neutro", coxa.getClasse());
    }

    @Test
    void mapasSeparamPorAlunoEOrdenamPorData() {
        Aluno bia = new Aluno();
        bia.setId(2L);
        bia.setNome("Bia");
        Aluno ari = new Aluno();
        ari.setId(1L);
        ari.setNome("Ari");
        // Chegam da mais nova para a mais antiga, como na tela do administrador
        AvaliacaoFisica biaNova = avaliacao(LocalDate.of(2026, 9, 1), 60.0, null, 70.0, null);
        AvaliacaoFisica ariUnica = avaliacao(LocalDate.of(2026, 8, 1), 90.0, null, null, null);
        AvaliacaoFisica biaAntiga = avaliacao(LocalDate.of(2026, 7, 1), 62.0, null, 74.0, null);
        biaNova.setAluno(bia);
        biaAntiga.setAluno(bia);
        ariUnica.setAluno(ari);

        List<MapaCorpo> mapas = service.mapasCorpo(List.of(biaNova, ariUnica, biaAntiga));
        assertEquals(List.of("Ari", "Bia"), mapas.stream().map(MapaCorpo::getNome).toList());
        assertNull(mapas.get(0).getMedidas().get(0).getDiferenca());
        MedidaCorpo cinturaBia = mapas.get(1).getMedidas().get(1);
        assertEquals("-4,0 cm", cinturaBia.getDiferenca());
        assertEquals("melhorou", cinturaBia.getClasse());
        assertTrue(service.mapasCorpo(List.of()).isEmpty());
    }

    @Test
    void precisaDeDuasMedidas() {
        List<AvaliacaoFisica> uma = List.of(avaliacao(LocalDate.of(2026, 7, 1), 82.0, 24.0, null, null));
        assertTrue(service.comparacao(uma).isEmpty());
        assertEquals(1, service.barrasGordura(uma).size());
        assertEquals("24,0%", service.barrasGordura(uma).get(0).getTexto());
    }
}
