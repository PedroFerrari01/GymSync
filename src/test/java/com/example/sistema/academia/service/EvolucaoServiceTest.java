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
        // Peso não tem cor; braço que aumentou melhorou
        assertEquals("neutro", linhas.get(0).getClasse());
        assertEquals("melhorou", linhas.get(3).getClasse());
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
        assertEquals("neutro", mapa.getMedidas().get(0).getClasse());
        assertEquals("melhorou", mapa.getMedidas().get(3).getClasse());

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

    private MedidaCorpo medida(MapaCorpo mapa, String chave) {
        return mapa.getMedidas().stream().filter(m -> m.getChave().equals(chave)).findFirst().orElseThrow();
    }

    private AvaliacaoFisica completa(LocalDate data, double v) {
        AvaliacaoFisica a = avaliacao(data, 80 + v, 20 + v, 85 + v, 33 + v);
        a.setQuadril(98 + v);
        a.setAbdomen(90 + v);
        a.setPeito(100 + v);
        a.setCoxa(56 + v);
        a.setOmbro(112 + v);
        a.setAntebraco(28 + v);
        a.setPanturrilha(37 + v);
        a.setAltura(1.75);
        return a;
    }

    @Test
    void aumentarEhBomParaOmbroAntebracoEPanturrilha() {
        List<String> novas = List.of("ombro", "antebraco", "panturrilha");
        MapaCorpo subiu = service.mapaCorpo(1L, "Aluno", List.of(
                completa(LocalDate.of(2026, 8, 18), 0), completa(LocalDate.of(2026, 11, 11), 2)));
        for (String chave : novas) assertEquals("melhorou", medida(subiu, chave).getClasse(), chave);
        MapaCorpo desceu = service.mapaCorpo(1L, "Aluno", List.of(
                completa(LocalDate.of(2026, 8, 18), 2), completa(LocalDate.of(2026, 11, 11), 0)));
        for (String chave : novas) assertEquals("piorou", medida(desceu, chave).getClasse(), chave);
        // Panturrilha 37 → 39 (+5,4%): mesma escala de intensidade das outras medidas
        assertEquals(0.68, medida(subiu, "panturrilha").getIntensidade());
        // Também entram na tabela "primeira x última", com a mesma cor
        assertTrue(service.comparacao(List.of(completa(LocalDate.of(2026, 8, 18), 0), completa(LocalDate.of(2026, 11, 11), 2)))
                .stream().anyMatch(c -> c.getRotulo().equals("Antebraço") && c.getClasse().equals("melhorou")));
    }

    @Test
    void bioimpedanciaMassaMuscularSobeEhBomMassaGordaDesceEhBom() {
        AvaliacaoFisica antes = avaliacao(LocalDate.of(2026, 8, 18), 68.0, null, null, null);
        antes.setMassaMuscularKg(24.8);
        antes.setMassaGordaKg(18.7);
        AvaliacaoFisica depois = avaliacao(LocalDate.of(2026, 11, 11), 65.1, null, null, null);
        depois.setMassaMuscularKg(25.9);
        depois.setMassaGordaKg(15.8);
        MapaCorpo mapa = service.mapaCorpo(1L, "Ana", List.of(antes, depois));
        assertEquals("melhorou", medida(mapa, "massaMuscular").getClasse());
        assertEquals("+1,1 kg", medida(mapa, "massaMuscular").getDiferenca());
        assertEquals("melhorou", medida(mapa, "massaGorda").getClasse());
        assertEquals("-2,9 kg", medida(mapa, "massaGorda").getDiferenca());
        assertTrue(service.comparacao(List.of(antes, depois)).stream()
                .anyMatch(c -> c.getRotulo().equals("Massa muscular") && c.getClasse().equals("melhorou")));
        // Ao contrário, as duas pioram
        assertEquals("piorou", medida(service.mapaCorpo(1L, "Ana", List.of(depois, antes)), "massaMuscular").getClasse());
        assertEquals("piorou", medida(service.mapaCorpo(1L, "Ana", List.of(depois, antes)), "massaGorda").getClasse());
    }

    @Test
    void semBioimpedanciaNaoApareceNada() {
        MapaCorpo mapa = service.mapaCorpo(1L, "Aluno", List.of(
                completa(LocalDate.of(2026, 8, 18), 0), completa(LocalDate.of(2026, 11, 11), 2)));
        assertTrue(mapa.getMedidas().stream().noneMatch(m -> m.getChave().startsWith("massa")));
    }

    @Test
    void alturaDoCadastroValeAntesDaAvaliacaoESilhuetaVemDoAluno() {
        Aluno aluno = new Aluno();
        aluno.setId(3L);
        aluno.setNome("Ana");
        AvaliacaoFisica a = completa(LocalDate.of(2026, 11, 11), 0); // altura 1,75 m na avaliação
        a.setAluno(aluno);
        // Sem altura no cadastro: vale a da avaliação; silhueta padrão "não informada"
        MapaCorpo semCadastro = service.mapaCorpo(3L, "Ana", List.of(a));
        assertEquals(175.0, semCadastro.getAlturaCm());
        assertEquals(Aluno.SILHUETA_NAO_INFORMADA, semCadastro.getSilhueta());
        // Com altura e silhueta no cadastro: valem as do cadastro
        aluno.setAlturaCm(165);
        aluno.setSilhueta(Aluno.SILHUETA_FEMININA);
        MapaCorpo comCadastro = service.mapaCorpo(3L, "Ana", List.of(a));
        assertEquals(165.0, comCadastro.getAlturaCm());
        assertEquals("FEMININA", comCadastro.getSilhueta());
        // Sem altura em lugar nenhum: nula (o mapa usa a altura padrão e avisa)
        aluno.setAlturaCm(null);
        a.setAltura(null);
        assertNull(service.mapaCorpo(3L, "Ana", List.of(a)).getAlturaCm());
    }

    @Test
    void medidaNovaNulaNaoEntraNoMapaNemNaComparacao() {
        // Avaliações antigas não têm ombro, antebraço nem panturrilha
        AvaliacaoFisica antiga = completa(LocalDate.of(2026, 8, 18), 0);
        antiga.setOmbro(null);
        antiga.setAntebraco(null);
        antiga.setPanturrilha(null);
        AvaliacaoFisica nova = completa(LocalDate.of(2026, 11, 11), 2);
        nova.setAntebraco(null);
        nova.setPanturrilha(null);
        MapaCorpo mapa = service.mapaCorpo(1L, "Aluno", List.of(antiga, nova));
        // Ombro medido só uma vez: aparece sem diferença e sem cor; antebraço e panturrilha ficam de fora
        assertEquals("neutro", medida(mapa, "ombro").getClasse());
        assertNull(medida(mapa, "ombro").getDiferenca());
        assertTrue(mapa.getMedidas().stream().noneMatch(m -> m.getChave().equals("antebraco") || m.getChave().equals("panturrilha")));
        assertTrue(service.comparacao(List.of(antiga, nova)).stream()
                .noneMatch(c -> List.of("Ombro", "Antebraço", "Panturrilha").contains(c.getRotulo())));
    }

    @Test
    void aumentarEhBomParaPeitoBracoECoxa() {
        // Todas as medidas sobem 2: peito, braço e coxa melhoram; cintura, quadril e gordura pioram
        MapaCorpo mapa = service.mapaCorpo(1L, "Aluno", List.of(
                completa(LocalDate.of(2026, 8, 18), 0), completa(LocalDate.of(2026, 11, 11), 2)));
        for (String chave : List.of("peito", "braco", "coxa")) assertEquals("melhorou", medida(mapa, chave).getClasse(), chave);
        for (String chave : List.of("cintura", "quadril", "gordura")) assertEquals("piorou", medida(mapa, chave).getClasse(), chave);
        for (String chave : List.of("peso", "imc")) assertEquals("neutro", medida(mapa, chave).getClasse(), chave);
    }

    @Test
    void diminuirEhBomParaCinturaQuadrilEGordura() {
        // Todas as medidas descem 2: o contrário do teste acima; peso e IMC continuam sem cor
        MapaCorpo mapa = service.mapaCorpo(1L, "Aluno", List.of(
                completa(LocalDate.of(2026, 8, 18), 2), completa(LocalDate.of(2026, 11, 11), 0)));
        for (String chave : List.of("cintura", "quadril", "gordura")) assertEquals("melhorou", medida(mapa, chave).getClasse(), chave);
        for (String chave : List.of("peito", "braco", "coxa")) assertEquals("piorou", medida(mapa, chave).getClasse(), chave);
        for (String chave : List.of("peso", "imc")) assertEquals("neutro", medida(mapa, chave).getClasse(), chave);
        // Cada região tem um status só
        assertEquals(mapa.getMedidas().size(), mapa.getMedidas().stream().map(MedidaCorpo::getChave).distinct().count());
    }

    @Test
    void semMudancaFicaNeutro() {
        assertEquals("neutro", EvolucaoService.classe(EvolucaoService.Direcao.MAIOR_EH_MELHOR, 0));
        assertEquals("neutro", EvolucaoService.classe(EvolucaoService.Direcao.MENOR_EH_MELHOR, 0));
        assertEquals("neutro", EvolucaoService.classe(EvolucaoService.Direcao.NEUTRA, 5));
    }

    @Test
    void intensidadeUsaTetoFixoDeOitoPorCento() {
        // 8% ou mais: cor cheia; 4%: metade (a escala não depende do maior valor do aluno)
        assertEquals(1.0, service.intensidade("melhorou", 100, 92));
        assertEquals(1.0, service.intensidade("piorou", 100, 120));
        assertEquals(0.5, service.intensidade("melhorou", 50, 52));
        assertEquals(0.5, service.intensidade("piorou", 100, 96));
        // Estável (sem diferença) e neutro (peso, IMC): sem cor, intensidade 0
        assertEquals(0.0, service.intensidade(EvolucaoService.classe(EvolucaoService.Direcao.MENOR_EH_MELHOR, 0), 90, 90));
        assertEquals(0.0, service.intensidade("neutro", 80, 90));
        // O teto vem da configuração
        assertEquals(1.0, new EvolucaoService(4).intensidade("melhorou", 100, 96));
    }

    @Test
    void mapaLevaAIntensidadeDeCadaMedida() {
        // Cintura 76 → 71 (−6,6%) pinta mais forte que coxa 55 → 56 (+1,8%); peso fica em 0
        AvaliacaoFisica antes = avaliacao(LocalDate.of(2026, 8, 18), 68.0, null, 76.0, null);
        antes.setCoxa(55.0);
        AvaliacaoFisica depois = avaliacao(LocalDate.of(2026, 11, 11), 65.1, null, 71.0, null);
        depois.setCoxa(56.0);
        MapaCorpo mapa = service.mapaCorpo(1L, "Ana", List.of(antes, depois));
        assertEquals(0.82, medida(mapa, "cintura").getIntensidade());
        assertEquals(0.23, medida(mapa, "coxa").getIntensidade());
        assertEquals(0.0, medida(mapa, "peso").getIntensidade());
        assertTrue(medida(mapa, "cintura").getIntensidade() > medida(mapa, "coxa").getIntensidade());
    }

    @Test
    void precisaDeDuasMedidas() {
        List<AvaliacaoFisica> uma = List.of(avaliacao(LocalDate.of(2026, 7, 1), 82.0, 24.0, null, null));
        assertTrue(service.comparacao(uma).isEmpty());
        assertEquals(1, service.barrasGordura(uma).size());
        assertEquals("24,0%", service.barrasGordura(uma).get(0).getTexto());
    }

    @Test
    void abdomenDiminuirEhBomComAMesmaRegraDaCintura() {
        MapaCorpo desceu = service.mapaCorpo(1L, "Aluno", List.of(
                completa(LocalDate.of(2026, 8, 18), 4), completa(LocalDate.of(2026, 11, 11), 0)));
        assertEquals("melhorou", medida(desceu, "abdomen").getClasse());
        assertEquals("-4,0 cm", medida(desceu, "abdomen").getDiferenca());
        assertEquals("94,0 cm", medida(desceu, "abdomen").getPrimeira());
        MapaCorpo subiu = service.mapaCorpo(1L, "Aluno", List.of(
                completa(LocalDate.of(2026, 8, 18), 0), completa(LocalDate.of(2026, 11, 11), 3)));
        assertEquals("piorou", medida(subiu, "abdomen").getClasse());
        // 90 -> 90 sem mudança: sem cor (o mapa mostra "estável")
        MapaCorpo igual = service.mapaCorpo(1L, "Aluno", List.of(
                completa(LocalDate.of(2026, 8, 18), 0), completa(LocalDate.of(2026, 11, 11), 0)));
        assertEquals("neutro", medida(igual, "abdomen").getClasse());
        // Avaliações antigas, sem abdômen, não quebram: a medida só some
        assertTrue(service.mapaCorpo(1L, "Aluno", List.of(avaliacao(LocalDate.of(2026, 8, 18), 80.0, 20.0, 85.0, 33.0)))
                .getMedidas().stream().noneMatch(m -> m.getChave().equals("abdomen")));
        assertTrue(service.comparacao(List.of(completa(LocalDate.of(2026, 8, 18), 4), completa(LocalDate.of(2026, 11, 11), 0)))
                .stream().anyMatch(c -> c.getRotulo().equals("Abdômen") && c.getClasse().equals("melhorou")));
    }
}
