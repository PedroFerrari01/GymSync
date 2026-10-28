# GymSync

Sistema web de gestão de academias feito em **Java com Spring Boot**: cadastros, planos, matrículas, mensalidades com Pix simulado, catraca, fichas de treino, avaliações físicas, aulas coletivas, equipamentos, mensagens e relatórios. Cada pessoa entra com o próprio perfil (administrador, professor ou aluno) e só vê o que é dela.

Desenvolvi este sistema para um trabalho acadêmico do curso de Análise e Desenvolvimento de Sistemas: modelagem, regras de negócio, back-end, telas e testes.

![Java](https://img.shields.io/badge/Java_25-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL_8-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=flat-square&logo=thymeleaf&logoColor=white)
![JUnit](https://img.shields.io/badge/JUnit_5-25A162?style=flat-square&logo=junit5&logoColor=white)

![Tela de login do GymSync](docs/img/login.jpg)

## Destaques

- **Regras de negócio que rodam sozinhas.** A matrícula gera todas as mensalidades; a mensalidade vira atrasada quando o vencimento passa; a catraca só libera quem está ativo, com matrícula ativa e nada em atraso, e mostra o motivo quando bloqueia.
- **Acesso por perfil, testado.** Um interceptador separa o que admin, professor e aluno podem abrir, inclusive digitando a URL. São 44 testes automatizados, e um deles falha se alguém criar uma rota nova e esquecer de protegê-la.
- **Página de apresentação interativa.** Abaixo do login fica um "Quem somos" com um halter em 3D (Three.js), animações de rolagem (GSAP) e réplicas das telas do sistema que dá para clicar: liberar a catraca, pagar uma mensalidade, reservar aula.
- **Interface pensada para o dia a dia.** Tema claro e escuro, funciona no computador e no celular, roda offline (bibliotecas servidas pelo próprio projeto).

## Telas

| | |
| :---: | :---: |
| ![Painel do administrador](docs/img/painel-admin.jpg) | ![Área do aluno no tema claro](docs/img/area-aluno-claro.jpg) |
| Painel do administrador: indicadores, catraca e mensalidades | Área do aluno no tema claro |
| ![Abertura do Quem somos com o halter 3D](docs/img/quem-somos.jpg) | ![Jornada da matrícula ao treino](docs/img/jornada.jpg) |
| "Quem somos": halter 3D caindo na frente do título | A jornada da matrícula ao treino, passo a passo |
| ![Demonstrações clicáveis](docs/img/demonstracoes.jpg) | ![Menu de cada perfil](docs/img/perfis.jpg) |
| Demonstrações clicáveis das telas | O menu que cada perfil enxerga |

*Os nomes e o CPF nas imagens são de exemplo.*

## Perfis

| Perfil | O que faz |
| :--- | :--- |
| **Administrador** | Cadastra alunos, professores e planos; faz e cancela matrículas; confirma pagamentos; controla a catraca e os equipamentos; acompanha relatórios e avisos. |
| **Professor** | Monta as fichas de treino; registra avaliações físicas; cria aulas coletivas; conversa com os alunos e envia a ficha por WhatsApp ou e-mail. |
| **Aluno** | Vê e imprime o treino e registra a carga; acompanha frequência, mensalidades e avaliações; informa pagamentos; reserva aulas; fala com o professor. |

## Regras de negócio

| Assunto | Regra |
| :--- | :--- |
| Planos | O plano guarda o valor total e a duração; a mensalidade é o total dividido pelos meses (a última parcela ajusta os centavos). |
| Matrícula | Gera todas as mensalidades. Não permite duas matrículas ativas nem matricular aluno inativo. |
| Mensalidade | Pendente, atrasada, em análise, paga ou cancelada. Fica atrasada sozinha depois do vencimento. |
| Pagamento | O aluno paga pelo Pix (simulado) e informa; o pagamento fica em análise até o administrador confirmar ou recusar. |
| Catraca | Libera só aluno ativo, com matrícula ativa e sem mensalidade atrasada. Senão, bloqueia e mostra o motivo. |
| Aulas coletivas | A reserva vale para a próxima aula e respeita o limite de vagas. |
| Treinos | Cada professor altera as fichas que criou; o administrador altera todas. |

## Arquitetura

Aplicação MVC com páginas montadas no servidor:

```text
src/main/java/com/example/sistema/academia
├── config       # interceptador de acesso por perfil e BCrypt
├── controller   # login, cadastros e financeiro, treinos, aulas, avaliações, equipamentos, mensagens
├── dto          # dados de apoio para as telas (avisos, gráficos, contatos)
├── model        # 15 entidades JPA
├── repository   # Spring Data JPA (consultas derivadas do nome do método)
├── service      # situação das mensalidades, avisos, relatórios, exclusão em cascata, ficha
├── util         # validação de CPF, e-mail e telefone
└── view         # formatação de dinheiro, datas e status para os templates
```

- **Telas:** Thymeleaf com fragmentos (`templates/fragments`), Bootstrap 5, CSS e JavaScript próprios em `static/`.
- **Banco:** MySQL; as tabelas são criadas e atualizadas pelo Hibernate, e o banco `academia_db` é criado na primeira execução.
- **Front-end da apresentação:** GSAP + ScrollTrigger, Three.js e Phosphor Icons, todos guardados no projeto para funcionar sem internet.

## Segurança

O que já existe:

- Senhas guardadas com BCrypt.
- Acesso separado por perfil, coberto por testes (`AutenticacaoInterceptorTest` e `RotasProtegidasTest`).
- Ações que alteram dados só por `POST`, com confirmação antes de excluir.
- Validação dos dados também no servidor.
- Sessão só por cookie e senha do banco fora do repositório (`application.properties` no `.gitignore`).

O que falta antes de colocar na internet (próxima etapa do projeto): trocar o interceptador pelo Spring Security, ligar a proteção CSRF, limitar tentativas de login, recuperar senha por link enviado ao e-mail e usar HTTPS.

## Como rodar

**Pré-requisitos:** JDK 25, MySQL 8 ligado e Git. O Maven já vem no projeto (Maven Wrapper).

```bash
git clone https://github.com/PedroFerrari01/GymSync.git
cd GymSync
```

1. Em `src/main/resources`, copie `application.properties.example` para `application.properties` e coloque a senha do seu MySQL em `spring.datasource.password`.
2. Rode:
   - Windows: `.\mvnw.cmd spring-boot:run`
   - Linux/macOS: `./mvnw spring-boot:run`
3. Abra `http://localhost:8080` quando aparecer `Started SistemaAcademiaApplication`.

**Acesso de teste:** administrador `admin` / `123`. Professores e alunos entram com o CPF ou e-mail cadastrado pelo administrador e a senha inicial `123`. Para cadastrar, use CPFs válidos (o sistema confere os dígitos).

**Testes de acesso** (não precisam do banco):

```bash
./mvnw test -Dtest=AutenticacaoInterceptorTest,RotasProtegidasTest
```

| Erro | Solução |
| :--- | :--- |
| `Communications link failure` | O MySQL não está ligado. |
| `Access denied for user 'root'` | A senha no `application.properties` não confere com a do MySQL. |
| `Public Key Retrieval is not allowed` | Acrescente `&allowPublicKeyRetrieval=true` na URL do banco. |
| `Port 8080 was already in use` | Já existe outra execução aberta; feche-a ou mude `server.port`. |

## Próximos passos

- [ ] Spring Security, CSRF e limite de tentativas de login
- [ ] Recuperação de senha por e-mail
- [ ] Pagamento real (Pix ou cartão) com um provedor de pagamento
- [ ] Avisos por e-mail ou WhatsApp e chat em tempo real
- [ ] Leitor biométrico e catraca de verdade
- [ ] Publicação na nuvem e aplicativo para o aluno

## Autoria

Desenvolvido por **Pedro Ferrari** ([GitHub](https://github.com/PedroFerrari01) · [LinkedIn](https://www.linkedin.com/in/pedrohpferrari/)): modelagem, regras de negócio, back-end, telas e testes.

O trabalho acadêmico foi feito em grupo: os colegas contribuíram com ideias, a proposta, o canvas e os slides da apresentação.

A página de apresentação "Quem somos" (halter 3D, animações e demonstrações) foi feita com apoio de IA.
