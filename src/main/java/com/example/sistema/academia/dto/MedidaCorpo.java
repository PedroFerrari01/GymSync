package com.example.sistema.academia.dto;

/** Uma medida do mapa do corpo: primeira e última vez que foi medida, diferença e classe (melhorou, piorou ou neutro). */
public class MedidaCorpo {
    private final String chave;
    private final String rotulo;
    private final String primeira;
    private final String ultima;
    private final Double primeiraNumero;
    private final Double ultimaNumero;
    private final String diferenca;
    private final String classe;

    public MedidaCorpo(String chave, String rotulo, String primeira, String ultima,
                       Double primeiraNumero, Double ultimaNumero, String diferenca, String classe) {
        this.chave = chave;
        this.rotulo = rotulo;
        this.primeira = primeira;
        this.ultima = ultima;
        this.primeiraNumero = primeiraNumero;
        this.ultimaNumero = ultimaNumero;
        this.diferenca = diferenca;
        this.classe = classe;
    }

    public String getChave() { return chave; }
    public String getRotulo() { return rotulo; }
    public String getPrimeira() { return primeira; }
    public String getUltima() { return ultima; }
    public Double getPrimeiraNumero() { return primeiraNumero; }
    public Double getUltimaNumero() { return ultimaNumero; }
    /** Nulo quando a medida só foi feita uma vez. */
    public String getDiferenca() { return diferenca; }
    public String getClasse() { return classe; }
}
