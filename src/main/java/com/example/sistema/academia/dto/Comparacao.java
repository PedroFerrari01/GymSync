package com.example.sistema.academia.dto;

/** Uma linha de "primeira x última avaliação". Classe: melhorou, piorou ou neutro (só para a cor na tela). */
public class Comparacao {
    private final String rotulo;
    private final String primeira;
    private final String ultima;
    private final String diferenca;
    private final String classe;

    public Comparacao(String rotulo, String primeira, String ultima, String diferenca, String classe) {
        this.rotulo = rotulo;
        this.primeira = primeira;
        this.ultima = ultima;
        this.diferenca = diferenca;
        this.classe = classe;
    }

    public String getRotulo() { return rotulo; }
    public String getPrimeira() { return primeira; }
    public String getUltima() { return ultima; }
    public String getDiferenca() { return diferenca; }
    public String getClasse() { return classe; }
}
