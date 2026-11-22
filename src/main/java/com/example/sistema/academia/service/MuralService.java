package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.Notificacao;
import com.example.sistema.academia.model.MuralAviso;
import com.example.sistema.academia.repository.MuralAvisoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Mural de avisos da academia: quais estão no ar e a validação do que o admin publica. */
@Service
public class MuralService {

    public static final int MAX_TITULO = 80;
    public static final int MAX_TEXTO = 300;

    @Autowired private MuralAvisoRepository repository;

    /** Avisos no ar hoje, do mais novo para o mais antigo. */
    public List<MuralAviso> ativos(LocalDate hoje) {
        return filtrarAtivos(repository.findByEncerradoFalseOrderByIdDesc(), hoje);
    }

    /** No ar = não encerrado e sem data de término vencida (o dia do término ainda vale). */
    public static List<MuralAviso> filtrarAtivos(List<MuralAviso> todos, LocalDate hoje) {
        List<MuralAviso> lista = new ArrayList<>();
        if (todos == null) return lista;
        for (MuralAviso a : todos) {
            if (a.isEncerrado()) continue;
            if (a.getDataTermino() != null && a.getDataTermino().isBefore(hoje)) continue;
            lista.add(a);
        }
        return lista;
    }

    /** Mensagem de erro para o formulário, ou null se estiver tudo certo. */
    public static String validar(String titulo, String texto, LocalDate termino, LocalDate hoje) {
        if (titulo == null || titulo.trim().length() < 3) return "Informe o título do aviso (mínimo 3 letras).";
        if (titulo.trim().length() > MAX_TITULO) return "O título pode ter até " + MAX_TITULO + " caracteres.";
        if (texto == null || texto.trim().isEmpty()) return "Escreva o texto do aviso.";
        if (texto.trim().length() > MAX_TEXTO) return "O texto pode ter até " + MAX_TEXTO + " caracteres.";
        if (termino != null && termino.isBefore(hoje)) return "A data de término não pode ser anterior a hoje.";
        return null;
    }

    public MuralAviso publicar(String titulo, String texto, LocalDate termino, LocalDate hoje) {
        MuralAviso a = new MuralAviso();
        a.setTitulo(titulo.trim());
        a.setTexto(texto.trim());
        a.setDataPublicacao(hoje);
        a.setDataTermino(termino);
        return repository.save(a);
    }

    /** Encerra (tira do mural) sem apagar. Devolve false se o aviso não existe. */
    public boolean encerrar(Long id) {
        MuralAviso a = repository.findById(id).orElse(null);
        if (a == null) return false;
        a.setEncerrado(true);
        repository.save(a);
        return true;
    }

    /** Linhas do sininho: uma por aviso; o clique leva ao cartão do mural na aba inicial do perfil. */
    public static List<Notificacao> notificacoes(List<MuralAviso> avisos, String perfil) {
        String aba = "ALUNO".equals(perfil) ? "btn-aba-portal" : "PROFESSOR".equals(perfil) ? "btn-aba-treinos" : "btn-aba-painel";
        List<Notificacao> lista = new ArrayList<>();
        if (avisos == null) return lista;
        for (MuralAviso a : avisos) {
            lista.add(Notificacao.naAba("info", "Mural: " + a.getTitulo() + ". " + a.getTexto(), aba, "mural-academia"));
        }
        return lista;
    }

    /** Mural na frente das demais notificações. */
    public static List<Notificacao> naFrente(List<Notificacao> demais, List<MuralAviso> avisos, String perfil) {
        List<Notificacao> lista = notificacoes(avisos, perfil);
        if (demais != null) lista.addAll(demais);
        return lista;
    }
}
