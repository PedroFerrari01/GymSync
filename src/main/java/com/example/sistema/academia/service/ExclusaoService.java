package com.example.sistema.academia.service;

import com.example.sistema.academia.model.AulaColetiva;
import com.example.sistema.academia.model.ExercicioTreino;
import com.example.sistema.academia.model.Treino;
import com.example.sistema.academia.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExclusaoService {

    @Autowired private AlunoRepository alunoRepository;
    @Autowired private ProfessorRepository professorRepository;
    @Autowired private PlanoRepository planoRepository;
    @Autowired private MatriculaRepository matriculaRepository;
    @Autowired private MensalidadeRepository mensalidadeRepository;
    @Autowired private TreinoRepository treinoRepository;
    @Autowired private ExercicioTreinoRepository exercicioTreinoRepository;
    @Autowired private EntradaRepository entradaRepository;
    @Autowired private AvaliacaoFisicaRepository avaliacaoFisicaRepository;
    @Autowired private AulaColetivaRepository aulaColetivaRepository;
    @Autowired private ReservaAulaRepository reservaAulaRepository;
    @Autowired private MensagemRepository mensagemRepository;
    @Autowired private RegistroExercicioRepository registroExercicioRepository;
    @Autowired private EquipamentoRepository equipamentoRepository;
    @Autowired private ManutencaoRepository manutencaoRepository;

    @Transactional
    public void excluirExercicio(Long id) {
        registroExercicioRepository.deleteAll(registroExercicioRepository.findByExercicioId(id));
        exercicioTreinoRepository.deleteById(id);
    }

    @Transactional
    public void excluirTreino(Long id) {
        registroExercicioRepository.deleteAll(registroExercicioRepository.findByExercicioTreinoId(id));
        exercicioTreinoRepository.deleteAll(exercicioTreinoRepository.findByTreinoId(id));
        treinoRepository.deleteById(id);
    }

    @Transactional
    public void excluirAula(Long id) {
        reservaAulaRepository.deleteAll(reservaAulaRepository.findByAulaId(id));
        aulaColetivaRepository.deleteById(id);
    }

    @Transactional
    public void excluirAluno(Long id) {
        entradaRepository.deleteAll(entradaRepository.findByAlunoId(id));
        registroExercicioRepository.deleteAll(registroExercicioRepository.findByAlunoId(id));
        exercicioTreinoRepository.deleteAll(exercicioTreinoRepository.findByTreinoAlunoId(id));
        treinoRepository.deleteAll(treinoRepository.findByAlunoId(id));
        mensalidadeRepository.deleteAll(mensalidadeRepository.findByMatriculaAlunoId(id));
        matriculaRepository.deleteAll(matriculaRepository.findByAlunoId(id));
        avaliacaoFisicaRepository.deleteAll(avaliacaoFisicaRepository.findByAlunoId(id));
        reservaAulaRepository.deleteAll(reservaAulaRepository.findByAlunoId(id));
        mensagemRepository.deleteAll(mensagemRepository.findByAlunoId(id));
        alunoRepository.deleteById(id);
    }

    @Transactional
    public void excluirProfessor(Long id) {
        mensagemRepository.deleteAll(mensagemRepository.findByProfessorId(id));
        for (AulaColetiva aula : aulaColetivaRepository.findByProfessorId(id)) {
            reservaAulaRepository.deleteAll(reservaAulaRepository.findByAulaId(aula.getId()));
            aulaColetivaRepository.delete(aula);
        }
        for (Treino treino : treinoRepository.findByProfessorId(id)) {
            for (ExercicioTreino ex : exercicioTreinoRepository.findByTreinoId(treino.getId())) {
                registroExercicioRepository.deleteAll(registroExercicioRepository.findByExercicioId(ex.getId()));
                exercicioTreinoRepository.delete(ex);
            }
            treinoRepository.delete(treino);
        }
        professorRepository.deleteById(id);
    }

    @Transactional
    public void excluirPlano(Long id) {
        mensalidadeRepository.deleteAll(mensalidadeRepository.findByMatriculaPlanoId(id));
        matriculaRepository.deleteAll(matriculaRepository.findByPlanoId(id));
        planoRepository.deleteById(id);
    }

    @Transactional
    public void excluirEquipamento(Long id) {
        manutencaoRepository.deleteAll(manutencaoRepository.findByEquipamentoId(id));
        equipamentoRepository.deleteById(id);
    }
}
