package com.example.sistema.academia.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Cor de destaque da academia: valida o #RRGGBB, ajusta a cor para ficar legível em cada tema
 * e monta o CSS que redefine as variáveis de destaque. Só cálculo, sem banco.
 */
public final class CorMarca {

    public static final String PADRAO = "#f97316";

    private static final Pattern HEX = Pattern.compile("^#?([0-9a-fA-F]{6})$");
    private static final int FUNDO_ESCURO = 0x18181f;   // cartão do tema escuro
    private static final int BRANCO = 0xffffff;
    private static final int TEXTO_ESCURO = 0x0d0d10;
    private static final double CONTRASTE_TEXTO = 4.5;
    private static final double LUMINANCIA_MAXIMA = 0.55;
    private static final double LUMINANCIA_MINIMA = 0.012;

    private CorMarca() {}

    /** Devolve "#rrggbb" em minúsculas ou null se não for uma cor hexadecimal de 6 dígitos. */
    public static String normalizar(String texto) {
        if (texto == null) return null;
        var m = HEX.matcher(texto.trim());
        return m.matches() ? "#" + m.group(1).toLowerCase(Locale.ROOT) : null;
    }

    public static boolean ehPadrao(String cor) {
        return cor == null || PADRAO.equals(normalizar(cor));
    }

    /** Mensagem de erro para a cor, ou null se ela serve como destaque. */
    public static String erro(String texto) {
        String cor = normalizar(texto);
        if (cor == null) return "Cor inválida. Use o formato #RRGGBB, por exemplo #2563eb.";
        double l = luminancia(rgb(cor));
        if (l > LUMINANCIA_MAXIMA) return "Essa cor é clara demais: o texto branco dos botões ficaria ilegível. Escolha uma cor mais escura.";
        if (l < LUMINANCIA_MINIMA) return "Essa cor é escura demais para destacar itens no tema escuro. Escolha uma cor mais viva ou um grafite.";
        return null;
    }

    public static int rgb(String cor) {
        return Integer.parseInt(normalizar(cor).substring(1), 16);
    }

    public static double luminancia(int rgb) {
        return 0.2126 * canal((rgb >> 16) & 0xff) + 0.7152 * canal((rgb >> 8) & 0xff) + 0.0722 * canal(rgb & 0xff);
    }

    private static double canal(int v) {
        double c = v / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    public static double contraste(int a, int b) {
        double la = luminancia(a), lb = luminancia(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    /** Mistura a cor com outra (alvo) na proporção dada (0 a 1). */
    static int misturar(int cor, int alvo, double parte) {
        int r = (int) Math.round(((cor >> 16) & 0xff) * (1 - parte) + ((alvo >> 16) & 0xff) * parte);
        int g = (int) Math.round(((cor >> 8) & 0xff) * (1 - parte) + ((alvo >> 8) & 0xff) * parte);
        int b = (int) Math.round((cor & 0xff) * (1 - parte) + (alvo & 0xff) * parte);
        return (r << 16) | (g << 8) | b;
    }

    /** Clareia até a cor ter contraste de texto sobre o fundo escuro (links, ícones e barras). */
    public static int paraTemaEscuro(int cor) {
        int atual = cor;
        for (int i = 1; i <= 20 && contraste(atual, FUNDO_ESCURO) < CONTRASTE_TEXTO; i++) {
            atual = misturar(cor, BRANCO, i * 0.05);
        }
        return atual;
    }

    /** Escurece até a cor ter contraste de texto sobre o fundo branco (e texto branco legível sobre ela). */
    public static int paraTemaClaro(int cor) {
        int atual = cor;
        for (int i = 1; i <= 20 && contraste(atual, BRANCO) < CONTRASTE_TEXTO; i++) {
            atual = misturar(cor, 0x000000, i * 0.05);
        }
        return atual;
    }

    /** Cor do texto sobre o destaque: a que tem mais contraste entre quase preto e branco. */
    public static int textoSobre(int fundo) {
        return contraste(fundo, TEXTO_ESCURO) >= contraste(fundo, BRANCO) ? TEXTO_ESCURO : BRANCO;
    }

    public static String hex(int rgb) {
        return String.format("#%06x", rgb & 0xffffff);
    }

    private static String rgba(int rgb, String alfa) {
        return "rgba(" + ((rgb >> 16) & 0xff) + ", " + ((rgb >> 8) & 0xff) + ", " + (rgb & 0xff) + ", " + alfa + ")";
    }

    /** CSS que redefine só as variáveis e os poucos pontos de cor fixa do laranja. Cores de erro, sucesso e aviso não mudam. */
    public static String css(String cor) {
        int base = rgb(cor);
        int escuro = paraTemaEscuro(base);
        int claro = paraTemaClaro(base);
        int escuroHover = misturar(escuro, 0x000000, 0.14);
        int claroHover = misturar(claro, 0x000000, 0.2);
        StringBuilder s = new StringBuilder("/* Identidade da academia: gerado pelo sistema, não edite */\n");
        s.append(":root[data-theme=\"dark\"] {\n")
                .append("    --accent: ").append(hex(escuro)).append(";\n")
                .append("    --accent-hover: ").append(hex(escuroHover)).append(";\n")
                .append("    --accent-soft: ").append(rgba(escuro, "0.14")).append(";\n")
                .append("    --accent-texto: ").append(hex(textoSobre(escuro))).append(";\n}\n");
        s.append(":root[data-theme=\"light\"] {\n")
                .append("    --accent: ").append(hex(claro)).append(";\n")
                .append("    --accent-hover: ").append(hex(claroHover)).append(";\n")
                .append("    --accent-soft: ").append(rgba(claro, "0.10")).append(";\n")
                .append("    --accent-texto: ").append(hex(textoSobre(claro))).append(";\n}\n");
        s.append(TEXTO_SOBRE_DESTAQUE).append(" { color: var(--accent-texto) !important; }\n");
        s.append(COR_FIXA);
        return s.toString();
    }

    /** Elementos que desenham texto ou ícone sobre fundo de destaque (hoje com cor fixa por tema). */
    private static final String TEXTO_SOBRE_DESTAQUE = String.join(",\n",
            ".brand-icon", ".nav-menu .nav-link.active", ".nav-menu .nav-link.active i", ".btn-brand", ".btn-brand:hover",
            ".role-btn.active", ".perfil-btn.active", ".msg-eu", ".cal-dia.treinou", ".jornada-animada .passo.ativo .passo-num",
            ".demo-feito.ativo", ".demo-periodo-btn.ativo", ".cena-marcador", ".video-play-icone", ".aba-mensalidade.ativa",
            ".carteirinha-avatar", ".mt-serie.feita", ".mt-btn-forte", ".grade-hoje", ".grade-hoje:hover", ".sininho-contagem");

    /** Pontos com laranja escrito direto no CSS (sem variável): passam a seguir a cor escolhida. */
    private static final String COR_FIXA = """
            .nav-menu .nav-link.active, .role-btn.active { box-shadow: 0 4px 14px color-mix(in srgb, var(--accent) 30%, transparent) !important; }
            .prog-barra { background: var(--accent) !important; }
            .btn-progresso:hover { color: var(--accent) !important; }
            .contato-pendente { border-left-color: var(--accent) !important; }
            .recibo-topo { border-bottom-color: var(--accent) !important; }
            .recibo-aviso { border-left-color: var(--accent) !important; }
            .mt-aviso-visitante { background: var(--accent-soft) !important; }
            """;
}
