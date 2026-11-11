package com.example.sistema.academia.service;

import com.example.sistema.academia.dto.ProgressoAluno;
import com.example.sistema.academia.dto.ProgressoAluno.Barra;
import com.example.sistema.academia.dto.ProgressoAluno.Serie;
import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.AvaliacaoFisica;
import com.example.sistema.academia.model.RegistroExercicio;
import com.example.sistema.academia.repository.AlunoRepository;
import com.example.sistema.academia.repository.AvaliacaoFisicaRepository;
import com.example.sistema.academia.repository.RegistroExercicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Monta o gráfico de progresso de um aluno: peso e carga por exercício, dos registros mais recentes. */
@Service
public class ProgressoService {

    static final int MAX_PONTOS = 8;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM");

    @Autowired private AlunoRepository alunoRepository;
    @Autowired private AvaliacaoFisicaRepository avaliacaoFisicaRepository;
    @Autowired private RegistroExercicioRepository registroExercicioRepository;
    @Autowired private com.example.sistema.academia.repository.TreinoRepository treinoRepository;

    /** Admin vê todos; professor só alunos que tenham ficha dele; qualquer outro perfil não vê. */
    public boolean podeVer(String perfil, Long usuarioId, Long alunoId) {
        if ("ADMIN".equals(perfil)) return true;
        return "PROFESSOR".equals(perfil) && usuarioId != null && alunoId != null
                && treinoRepository.existsByProfessorIdAndAlunoId(usuarioId, alunoId);
    }

    public Optional<ProgressoAluno> montar(Long alunoId) {
        Optional<Aluno> aluno = alunoRepository.findById(alunoId);
        if (aluno.isEmpty()) return Optional.empty();

        List<Serie> peso = new ArrayList<>();
        List<AvaliacaoFisica> avaliacoes = avaliacaoFisicaRepository.findByAlunoIdOrderByDataAvaliacaoAscIdAsc(alunoId).stream()
                .filter(a -> a.getPeso() != null && a.getDataAvaliacao() != null).toList();
        List<Barra> barrasPeso = new ArrayList<>();
        double maxPeso = avaliacoes.stream().mapToDouble(AvaliacaoFisica::getPeso).max().orElse(0);
        for (AvaliacaoFisica a : ultimos(avaliacoes)) {
            barrasPeso.add(new Barra(a.getDataAvaliacao().format(DATA), formato(a.getPeso()) + " kg", altura(a.getPeso(), maxPeso)));
        }
        if (!barrasPeso.isEmpty()) peso.add(new Serie("Peso corporal", barrasPeso));

        Map<String, List<RegistroExercicio>> porExercicio = new LinkedHashMap<>();
        List<RegistroExercicio> registros = registroExercicioRepository.findByAlunoId(alunoId).stream()
                .filter(r -> r.getCarga() != null && r.getDataRegistro() != null && r.getExercicio() != null)
                .sorted((x, y) -> x.getDataRegistro().compareTo(y.getDataRegistro()))
                .toList();
        for (RegistroExercicio r : registros) {
            porExercicio.computeIfAbsent(r.getExercicio().getExercicio(), k -> new ArrayList<>()).add(r);
        }
        List<Serie> cargas = new ArrayList<>();
        for (Map.Entry<String, List<RegistroExercicio>> e : porExercicio.entrySet()) {
            List<RegistroExercicio> lista = ultimos(e.getValue());
            double max = lista.stream().mapToDouble(RegistroExercicio::getCarga).max().orElse(0);
            List<Barra> barras = new ArrayList<>();
            for (RegistroExercicio r : lista) {
                barras.add(new Barra(r.getDataRegistro().format(DATA), formato(r.getCarga()) + " kg", altura(r.getCarga(), max)));
            }
            cargas.add(new Serie(e.getKey(), barras));
        }

        return Optional.of(new ProgressoAluno(aluno.get().getNome(), peso, cargas));
    }

    private static <T> List<T> ultimos(List<T> lista) {
        return lista.size() <= MAX_PONTOS ? lista : lista.subList(lista.size() - MAX_PONTOS, lista.size());
    }

    private static int altura(double valor, double max) {
        if (max <= 0) return 4;
        return Math.max(4, (int) Math.round(valor / max * 100));
    }

    private static String formato(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f", v);
    }
}
