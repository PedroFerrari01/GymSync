package com.example.sistema.academia.service;

import com.example.sistema.academia.model.Aluno;
import com.example.sistema.academia.model.ExercicioTreino;
import com.example.sistema.academia.model.Treino;
import com.example.sistema.academia.repository.ExercicioTreinoRepository;
import com.example.sistema.academia.repository.TreinoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class FichaService {

    @Autowired private TreinoRepository treinoRepository;
    @Autowired private ExercicioTreinoRepository exercicioTreinoRepository;

    public String textoTreino(Treino treino) {
        StringBuilder sb = new StringBuilder();
        sb.append(treino.getDescricao()).append(" (Prof. ").append(treino.getProfessor().getNome()).append(")\n");
        for (ExercicioTreino ex : exercicioTreinoRepository.findByTreinoId(treino.getId())) {
            sb.append("- ").append(ex.getExercicio()).append(": ").append(ex.getSeries()).append("x").append(ex.getRepeticoes()).append("\n");
        }
        return sb.toString();
    }

    public String textoAluno(Aluno aluno) {
        StringBuilder sb = new StringBuilder();
        sb.append("GymSync - Ficha de treino\nAluno: ").append(aluno.getNome()).append("\n\n");
        for (Treino treino : treinoRepository.findByAlunoId(aluno.getId())) {
            sb.append(textoTreino(treino)).append("\n");
        }
        return sb.toString();
    }

    public String linkWhatsapp(String telefone, String texto) {
        if (telefone == null) return null;
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.length() != 10 && digitos.length() != 11) return null;
        return "https://wa.me/55" + digitos + "?text=" + URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
