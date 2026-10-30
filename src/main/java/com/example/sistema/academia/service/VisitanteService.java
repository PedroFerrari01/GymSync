package com.example.sistema.academia.service;

import com.example.sistema.academia.model.*;
import com.example.sistema.academia.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Acesso de visitante: entra no sistema só para olhar, com dados de exemplo, sem poder salvar nada.
 * Fica desligado por padrão (gymsync.visitante=false), porque o visitante enxerga o painel inteiro.
 * Ligado e com o banco vazio, cria os dados de exemplo na partida do sistema.
 */
@Service
public class VisitanteService {

    public static final String EMAIL_ALUNO = "ana.souza@exemplo.gymsync";
    public static final String EMAIL_PROFESSOR = "marcos.lima@exemplo.gymsync";
    private static final Logger log = LoggerFactory.getLogger(VisitanteService.class);

    @Value("${gymsync.visitante:false}") private boolean habilitado;

    @Autowired private AlunoRepository alunoRepository;
    @Autowired private ProfessorRepository professorRepository;
    @Autowired private PlanoRepository planoRepository;
    @Autowired private MatriculaRepository matriculaRepository;
    @Autowired private MensalidadeRepository mensalidadeRepository;
    @Autowired private TreinoRepository treinoRepository;
    @Autowired private ExercicioTreinoRepository exercicioTreinoRepository;
    @Autowired private RegistroExercicioRepository registroExercicioRepository;
    @Autowired private EntradaRepository entradaRepository;
    @Autowired private AvaliacaoFisicaRepository avaliacaoFisicaRepository;
    @Autowired private AulaColetivaRepository aulaColetivaRepository;
    @Autowired private EquipamentoRepository equipamentoRepository;
    @Autowired private ManutencaoRepository manutencaoRepository;
    @Autowired private MensagemRepository mensagemRepository;
    @Autowired private PasswordEncoder encoder;

    public boolean habilitado() {
        return habilitado;
    }

    public Optional<Aluno> alunoExemplo() {
        return alunoRepository.findFirstByCpfOrEmailIgnoreCase(EMAIL_ALUNO, EMAIL_ALUNO);
    }

    public Optional<Professor> professorExemplo() {
        return professorRepository.findFirstByCpfOrEmailIgnoreCase(EMAIL_PROFESSOR, EMAIL_PROFESSOR);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void criarDadosDeExemplo() {
        if (!habilitado) return;
        if (alunoRepository.count() > 0 || professorRepository.count() > 0 || planoRepository.count() > 0) return;
        log.info("Banco vazio e acesso de visitante ligado: criando os dados de exemplo.");

        LocalDate hoje = LocalDate.now();
        Random sorteio = new Random(42); // sempre os mesmos dados

        Plano mensal = plano("Mensal", 1, "99.90");
        Plano trimestral = plano("Trimestral", 3, "269.70");
        Plano anual = plano("Anual", 12, "1078.80");

        Professor marcos = professor("Marcos Lima", EMAIL_PROFESSOR, "Musculação", 1);
        Professor carla = professor("Carla Nunes", "carla.nunes@exemplo.gymsync", "Funcional e spinning", 2);

        Aluno ana = aluno("Ana Souza", EMAIL_ALUNO, LocalDate.of(1998, 4, 12), "ATIVO", 3);
        Aluno bruno = aluno("Bruno Lima", "bruno.lima@exemplo.gymsync", LocalDate.of(1995, 9, 3), "ATIVO", 4);
        Aluno carlaDias = aluno("Carla Dias", "carla.dias@exemplo.gymsync", LocalDate.of(2001, 1, 22), "INATIVO", 5);
        Aluno diego = aluno("Diego Alves", "diego.alves@exemplo.gymsync", LocalDate.of(1990, 6, 30), "ATIVO", 6);
        Aluno elisa = aluno("Elisa Rocha", "elisa.rocha@exemplo.gymsync", LocalDate.of(2003, 11, 8), "ATIVO", 7);
        Aluno felipe = aluno("Felipe Costa", "felipe.costa@exemplo.gymsync", LocalDate.of(1987, 2, 17), "ATIVO", 8);

        // Matrículas: Ana em dia (anual, a próxima vence em 2 dias para o visitante ver o aviso com "Pagar agora"),
        // Bruno com mensalidade atrasada, os outros em dia
        matricula(ana, anual, hoje.minusMonths(4).minusDays(8), 4);
        matricula(bruno, trimestral, hoje.minusMonths(2), 1);
        matricula(carlaDias, mensal, hoje.minusMonths(1), 1);
        matricula(diego, anual, hoje.minusMonths(7), 7);
        matricula(elisa, trimestral, hoje.minusMonths(1), 1);
        matricula(felipe, mensal, hoje.minusDays(5), 0);

        // Ficha da Ana montada pelo Marcos, com cargas registradas
        Treino treinoAna = new Treino();
        treinoAna.setAluno(ana);
        treinoAna.setProfessor(marcos);
        treinoAna.setDescricao("Treino A - superiores");
        treinoAna.setDataCriacao(hoje.minusDays(20));
        treinoRepository.save(treinoAna);
        ExercicioTreino supino = exercicio(treinoAna, "Supino reto", 4, 10);
        ExercicioTreino remada = exercicio(treinoAna, "Remada curvada", 3, 12);
        exercicio(treinoAna, "Desenvolvimento com halteres", 3, 12);
        registro(supino, ana, hoje.minusDays(7), 30.0);
        registro(supino, ana, hoje.minusDays(3), 32.5);
        registro(remada, ana, hoje.minusDays(3), 25.0);

        Treino treinoDiego = new Treino();
        treinoDiego.setAluno(diego);
        treinoDiego.setProfessor(marcos);
        treinoDiego.setDescricao("Treino B - inferiores");
        treinoDiego.setDataCriacao(hoje.minusDays(12));
        treinoRepository.save(treinoDiego);
        exercicio(treinoDiego, "Agachamento livre", 4, 10);
        exercicio(treinoDiego, "Leg press", 4, 12);

        // Bruno também treina com o Marcos, mas parou de vir (aparece em "Alunos sumidos")
        Treino treinoBruno = new Treino();
        treinoBruno.setAluno(bruno);
        treinoBruno.setProfessor(marcos);
        treinoBruno.setDescricao("Treino A - corpo inteiro");
        treinoBruno.setDataCriacao(hoje.minusDays(40));
        treinoRepository.save(treinoBruno);
        exercicio(treinoBruno, "Agachamento livre", 3, 12);
        exercicio(treinoBruno, "Puxada frontal", 3, 12);

        // Entradas na catraca nos últimos 60 dias (o relatório compara com o mês passado), com pico no fim da tarde
        int[] horas = {6, 7, 8, 12, 17, 18, 18, 19, 19, 20};
        List<Aluno> frequentes = List.of(ana, ana, diego, diego, elisa, felipe, bruno);
        for (int dia = 59; dia >= 0; dia--) {
            LocalDate data = hoje.minusDays(dia);
            if (data.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            for (Aluno aluno : frequentes) {
                if (aluno == bruno && dia < 12) continue;
                if (aluno == felipe && dia > 5) continue; // matriculou há 5 dias
                if (sorteio.nextInt(10) < 5) continue;
                Entrada e = new Entrada();
                e.setAluno(aluno);
                e.setDataHoraEntrada(data.atTime(horas[sorteio.nextInt(horas.length)], sorteio.nextInt(60)));
                if (!e.getDataHoraEntrada().isAfter(java.time.LocalDateTime.now())) entradaRepository.save(e);
            }
        }

        // Avaliações da Ana: o peso e o percentual de gordura caem aos poucos
        avaliacao(ana, marcos, hoje.minusDays(90), 68.0, 27.5, 76.0);
        avaliacao(ana, marcos, hoje.minusDays(45), 66.4, 25.8, 73.5);
        avaliacao(ana, marcos, hoje.minusDays(5), 65.1, 24.2, 71.0);

        aula("Spinning", carla, DayOfWeek.MONDAY, LocalTime.of(18, 0), 12);
        aula("Funcional", carla, DayOfWeek.WEDNESDAY, LocalTime.of(7, 0), 10);
        aula("Yoga", marcos, DayOfWeek.FRIDAY, LocalTime.of(19, 0), 15);

        equipamento("Esteira 01", "Cardio", "EST-01", "OPERANDO", hoje.plusDays(40));
        equipamento("Leg press 45°", "Musculação", "LEG-01", "OPERANDO", hoje.plusDays(15));
        Equipamento bike = equipamento("Bicicleta ergométrica 03", "Cardio", "BIK-03", "EM_MANUTENCAO", hoje.plusDays(2));
        Manutencao m = new Manutencao();
        m.setEquipamento(bike);
        m.setDataManutencao(hoje.minusDays(1));
        m.setTipo("CORRETIVA");
        m.setDescricao("Troca da correia do pedal.");
        m.setResponsavel("Assistência técnica");
        m.setCusto(180.0);
        manutencaoRepository.save(m);

        mensagem(ana, marcos, "ALUNO", "Professor, posso aumentar a carga do supino?", hoje.minusDays(2).atTime(19, 10), true);
        mensagem(ana, marcos, "PROFESSOR", "Pode sim, sobe 2,5 kg e mantém as 10 repetições.", hoje.minusDays(2).atTime(20, 5), false);
    }

    // ---------- ajudantes ----------

    private Plano plano(String nome, int meses, String valor) {
        Plano p = new Plano();
        p.setNome(nome);
        p.setDuracaoMeses(meses);
        p.setValor(new BigDecimal(valor));
        return planoRepository.save(p);
    }

    private Professor professor(String nome, String email, String especialidade, int n) {
        Professor p = new Professor();
        p.setNome(nome);
        p.setEmail(email);
        p.setCpf(cpfDeExemplo(n));
        p.setContato("(11) 90000-000" + n);
        p.setEspecialidade(especialidade);
        p.setSenha(encoder.encode(UUID.randomUUID().toString())); // ninguém entra com senha nestas contas
        return professorRepository.save(p);
    }

    private Aluno aluno(String nome, String email, LocalDate nascimento, String situacao, int n) {
        Aluno a = new Aluno();
        a.setNome(nome);
        a.setEmail(email);
        a.setCpf(cpfDeExemplo(n));
        a.setDataNascimento(nascimento);
        a.setContato("(11) 90000-000" + n);
        a.setSituacao(situacao);
        a.setSenha(encoder.encode(UUID.randomUUID().toString()));
        return alunoRepository.save(a);
    }

    /** Matrícula com mensalidades mensais; as {@code pagas} primeiras já foram pagas, as outras ficam pendentes. */
    private void matricula(Aluno aluno, Plano plano, LocalDate inicio, int pagas) {
        Matricula mat = new Matricula();
        mat.setAluno(aluno);
        mat.setPlano(plano);
        mat.setDataInicio(inicio);
        mat.setDataFim(inicio.plusMonths(plano.getDuracaoMeses()));
        mat.setSituacao("ATIVA");
        matriculaRepository.save(mat);
        BigDecimal parcela = plano.getValor().divide(BigDecimal.valueOf(plano.getDuracaoMeses()), 2, RoundingMode.HALF_UP);
        for (int i = 0; i < plano.getDuracaoMeses(); i++) {
            Mensalidade mens = new Mensalidade();
            mens.setMatricula(mat);
            mens.setValor(parcela);
            mens.setVencimento(inicio.plusDays(10).plusMonths(i));
            if (i < pagas) {
                mens.setSituacao("PAGA");
                mens.setDataPagamento(mens.getVencimento().minusDays(2));
            } else {
                mens.setSituacao("PENDENTE"); // vencida e não paga vira ATRASADA sozinha (SituacaoService)
            }
            mensalidadeRepository.save(mens);
        }
    }

    private ExercicioTreino exercicio(Treino treino, String nome, int series, int repeticoes) {
        ExercicioTreino e = new ExercicioTreino();
        e.setTreino(treino);
        e.setExercicio(nome);
        e.setSeries(series);
        e.setRepeticoes(repeticoes);
        return exercicioTreinoRepository.save(e);
    }

    private void registro(ExercicioTreino exercicio, Aluno aluno, LocalDate data, double carga) {
        RegistroExercicio r = new RegistroExercicio();
        r.setExercicio(exercicio);
        r.setAluno(aluno);
        r.setDataRegistro(data);
        r.setCarga(carga);
        registroExercicioRepository.save(r);
    }

    private void avaliacao(Aluno aluno, Professor professor, LocalDate data, double peso, double gordura, double cintura) {
        AvaliacaoFisica a = new AvaliacaoFisica();
        a.setAluno(aluno);
        a.setProfessorId(professor.getId());
        a.setRegistradoPor(professor.getNome());
        a.setDataAvaliacao(data);
        a.setPeso(peso);
        a.setAltura(1.65);
        a.setPercentualGordura(gordura);
        a.setCintura(cintura);
        a.setQuadril(98.0);
        a.setBraco(28.0);
        a.setCoxa(55.0);
        avaliacaoFisicaRepository.save(a);
    }

    private void aula(String nome, Professor professor, DayOfWeek dia, LocalTime horario, int vagas) {
        AulaColetiva a = new AulaColetiva();
        a.setNome(nome);
        a.setProfessor(professor);
        a.setDiaSemana(dia);
        a.setHorario(horario);
        a.setVagas(vagas);
        aulaColetivaRepository.save(a);
    }

    private Equipamento equipamento(String nome, String tipo, String codigo, String situacao, LocalDate proxima) {
        Equipamento e = new Equipamento();
        e.setNome(nome);
        e.setTipo(tipo);
        e.setCodigo(codigo);
        e.setDataAquisicao(LocalDate.now().minusYears(2));
        e.setSituacao(situacao);
        e.setProximaManutencao(proxima);
        return equipamentoRepository.save(e);
    }

    private void mensagem(Aluno aluno, Professor professor, String remetente, String texto, java.time.LocalDateTime quando, boolean lida) {
        Mensagem m = new Mensagem();
        m.setAluno(aluno);
        m.setProfessor(professor);
        m.setRemetente(remetente);
        m.setTexto(texto);
        m.setDataHora(quando);
        m.setLida(lida);
        mensagemRepository.save(m);
    }

    /** CPF fictício de exemplo (900.000.00n-xx), com os dígitos verificadores certos para passar na validação. */
    static String cpfDeExemplo(int n) {
        int[] d = new int[11];
        String base = String.format("900%06d", n);
        for (int i = 0; i < 9; i++) d[i] = base.charAt(i) - '0';
        for (int pos = 9; pos <= 10; pos++) {
            int soma = 0;
            for (int i = 0; i < pos; i++) soma += d[i] * (pos + 1 - i);
            int resto = soma % 11;
            d[pos] = resto < 2 ? 0 : 11 - resto;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            sb.append(d[i]);
            if (i == 2 || i == 5) sb.append('.');
            if (i == 8) sb.append('-');
        }
        return sb.toString();
    }
}
