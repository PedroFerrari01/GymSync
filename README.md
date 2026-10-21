# GymSync | Sistema de Gestão e Controle Operacional de Academias

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-005C84?style=for-the-badge&logo=mysql&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)
![Bootstrap](https://img.shields.io/badge/Bootstrap-563D7C?style=for-the-badge&logo=bootstrap&logoColor=white)

Aplicação web full-stack desenvolvida em **Java (Spring Boot)** e banco de dados relacional **MySQL** para a gestão completa de academias: alunos, professores, planos, matrículas, mensalidades, treinos, frequência por catraca, avaliações físicas, aulas coletivas e equipamentos. Interface responsiva que funciona perfeitamente no navegador, no computador e no celular.

---

## 📌 Contexto e Problema de Negócio

Academias de pequeno e médio porte costumam controlar o dia a dia com fichas de papel e planilhas descentralizadas. Essa falta de automação gera problemas críticos:
* **Inadimplência invisível:** Mensalidades atrasadas que ninguém percebe a tempo;
* **Falha de segurança:** Frequência marcada à mão, sem confiabilidade ou bloqueio de acesso;
* **Desorganização técnica:** Fichas de treino perdidas, desatualizadas ou sem histórico de evolução de cargas.

O **GymSync** foi desenvolvido para resolver essa dor: um sistema único, simples de usar e seguro, que organiza cadastros, financeiro, treinos e frequência, com uma tela e permissões próprias para cada tipo de usuário.

---

## 👥 Perfis de Usuário

| Perfil | O que faz no sistema |
| :--- | :--- |
| **Administrador** | Cadastra e edita alunos, professores e planos; realiza e cancela matrículas; registra e confirma pagamentos; libera a catraca; cuida dos equipamentos; acompanha relatórios e avisos. |
| **Professor** | Monta e altera fichas de treino; registra avaliações físicas; cria aulas coletivas; conversa com os alunos (pelo sistema ou WhatsApp); envia a ficha por WhatsApp ou e-mail. |
| **Aluno** | Consulta e imprime o treino; registra a carga de cada exercício; acompanha frequência, mensalidades e avaliações; informa pagamentos; reserva aulas coletivas; fala com o professor. |

---

## ✨ Funcionalidades Completas

* **Cadastros:** Alunos, professores e planos, com validação de CPF, e-mail e telefone no servidor e e-mail único por perfil.
* **Matrículas e Mensalidades:** A matrícula gera uma mensalidade por mês; o valor total do plano é dividido automaticamente entre as parcelas.
* **Pagamento pelo Aluno:** Código Pix de demonstração; o pagamento fica em análise até o administrador confirmar ou recusar.
* **Treinos:** Ficha com exercícios, séries e repetições; impressão com cabeçalho e espaço para anotar a carga; registro da carga usada pelo aluno.
* **Frequência por Catraca:** A entrada do aluno é registrada com data e hora (biometria simulada).
* **Avaliação Física:** Peso, altura, IMC, percentual de gordura e medidas, com gráfico de evolução do peso.
* **Aulas Coletivas:** Aulas semanais com controle de vagas, reservas, cancelamento e lista de inscritos.
* **Equipamentos:** Cadastro, situação e histórico de manutenções preventivas e corretivas.
* **Mensagens:** Conversa interna entre aluno e professor, com contador de não lidas e atalho direto para o WhatsApp.
* **Avisos Automáticos:** Alertas de mensalidades atrasadas ou a vencer, pagamentos para confirmar, matrículas terminando e manutenções previstas.
* **Relatórios Gerenciais:** Horários e dias de maior movimento, alunos mais frequentes, inadimplência e faturamento dos últimos 6 meses.
* **Interface:** Tema escuro e claro (Dark/Light Mode), adaptada para computador e celular.

---

## 🏗️ Arquitetura e Engenharia do Software

O sistema segue uma arquitetura em camadas (**MVC**) com renderização no servidor:

* **Camada Visual (Thymeleaf + Bootstrap):** Páginas HTML montadas no servidor a partir do modelo, divididas em fragmentos reutilizáveis (`templates/fragments`), com CSS e JavaScript próprios em `static/`.
* **Controllers (Spring MVC):** Recebem as requisições, aplicam as regras de entrada e decidem o fluxo. São separados por domínio: login e senha, cadastros e financeiro, treinos, aulas, avaliações, equipamentos e mensagens.
* **Services:** Concentram regras reutilizáveis, como a atualização automática de mensalidades atrasadas e matrículas vencidas, os avisos, os relatórios e a exclusão em cascata.
* **Repositories (Spring Data JPA):** Acesso ao banco com consultas derivadas dos nomes dos métodos (*Query Methods*).
* **Models (JPA/Hibernate):** Entidades que espelham as tabelas do MySQL.
* **Configuração:** Interceptador de acesso por perfil e criptografia de senhas com `BCrypt`.

~~~text
src/main/java/com/example/sistema/academia
├── config       # Proteção das rotas por perfil e criptografia de senha
├── controller   # Login, cadastros, treinos, aulas, avaliações, equipamentos, mensagens
├── dto          # Classes de apoio para as telas (avisos, gráficos, contatos)
├── model        # Entidades JPA (Mapeamento Objeto-Relacional)
├── repository   # Interfaces Spring Data JPA
├── service      # Regras de negócio reutilizáveis
├── util         # Validador de CPF, e-mail e telefone
└── view         # Formatadores usados nos templates
~~~

---

## 🔐 Segurança

* **Criptografia:** Senhas guardadas com `BCrypt` (senhas antigas em texto puro são convertidas automaticamente no primeiro login).
* **Controle de Acesso (RBAC):** Acesso separado por perfil via *Interceptor*; um aluno não consegue abrir as telas do administrador, nem digitando a URL diretamente no navegador.
* **Proteção de Requisições:** Ações que alteram ou removem dados usam obrigatoriamente o método `POST` e pedem confirmação antes de excluir.
* **Validação Backend:** Validação rigorosa dos dados também no servidor, e não apenas no navegador.

---

## 🔄 Regras de Negócio

| Assunto | Regra Implementada |
| :--- | :--- |
| **Planos** | O plano guarda o valor total e a duração em meses; a mensalidade é o total dividido pelos meses (a última parcela ajusta automaticamente os centavos). |
| **Matrícula** | Gera todas as mensalidades, com vencimento 10 dias após a matrícula e a cada mês seguinte. Não permite duas matrículas ativas nem matricular aluno inativo. Passa a vencida no fim do prazo. |
| **Mensalidade** | Situações: `pendente`, `atrasada`, `em análise`, `paga` e `cancelada`. Vira atrasada sozinha depois do vencimento. |
| **Catraca** | Só libera a entrada se o aluno estiver ativo, com matrícula ativa e sem mensalidade atrasada. Caso contrário, bloqueia e mostra o motivo exato na tela. |
| **Aulas Coletivas** | Aulas semanais; a reserva vale para a próxima aula e respeita o limite de vagas. Só reserva quem está ativo e com matrícula ativa. |
| **Treinos** | Cada professor altera apenas as próprias fichas que criou; o administrador possui permissão para alterar todas. |

---

## 🛠️ Tecnologias Utilizadas

* **Linguagem:** Java 25 (configurado no `pom.xml`)
* **Framework:** Spring Boot 4 (Spring MVC, Spring Data JPA)
* **Interface:** Thymeleaf, Bootstrap 5, Bootstrap Icons, JavaScript
* **Banco de Dados:** MySQL 8
* **Segurança:** `spring-security-crypto` (BCrypt)
* **E-mail (opcional):** Spring Mail (Gmail SMTP)
* **Build:** Maven (Maven Wrapper incluído)
* **Versionamento:** Git & GitHub

---

## 🗄️ Estrutura do Banco de Dados

As tabelas são criadas e atualizadas automaticamente pelo Hibernate (`spring.jpa.hibernate.ddl-auto=update`). O banco `academia_db` também é criado sozinho na primeira execução.

| Tabela | Conteúdo Armazenado |
| :--- | :--- |
| `alunos` | Dados pessoais, senha criptografada e situação (ativo/inativo). |
| `professores` | Dados pessoais, especialidade e senha criptografada. |
| `planos` | Nome, duração em meses e valor total. |
| `matriculas` | Aluno, plano, datas de vigência e situação. |
| `mensalidades` | Matrícula, valor, vencimento, data de pagamento e situação. |
| `treinos` | Ficha do aluno criada por um professor. |
| `exercicios_treino` | Exercícios, séries e repetições de cada ficha. |
| `registros_exercicio` | Carga e observação registradas pelo próprio aluno. |
| `entradas` | Registros de acesso na catraca (data e hora). |
| `avaliacoes_fisicas` | Peso, altura, percentual de gordura e medidas corporais. |
| `aulas_coletivas` | Aula, professor responsável, dia, horário e limite de vagas. |
| `reservas_aula` | Reserva de aluno em uma aula coletiva. |
| `equipamentos` | Máquinas, situação atual e data da próxima manutenção. |
| `manutencoes` | Histórico de manutenções preventivas e corretivas de cada equipamento. |
| `mensagens` | Conversas internas entre aluno e professor. |

---

## 🚀 Passo a Passo — Como Rodar a Aplicação

### Pré-requisitos
* **JDK 25** (o Spring Boot 4 exige no mínimo o Java 17)
* **MySQL 8** ou superior em execução
* **Git** (o Maven já vem integrado no projeto pelo Maven Wrapper)

**1. Clonar o repositório**
~~~bash
git clone https://github.com/PedroFerrari01/GymSync.git
cd GymSync
~~~

**2. Configurar o banco de dados**
* Verifique se o serviço do MySQL está ligado na sua máquina.
* Na pasta `src/main/resources`, faça uma cópia do arquivo `application.properties.example` e renomeie para `application.properties`.
* Abra o `application.properties` e troque `SUA_SENHA_DO_MYSQL` pela senha do seu MySQL (e o usuário, caso não utilize `root`):
~~~properties
spring.datasource.username=root
spring.datasource.password=SUA_SENHA_DO_MYSQL
~~~

**3. Executar pelo terminal**
* **No Windows:**
~~~bash
.\mvnw.cmd spring-boot:run
~~~
* **No Linux / macOS:**
~~~bash
./mvnw spring-boot:run
~~~
*(Na primeira vez, o Maven baixará as dependências automaticamente, sendo necessário estar conectado à internet).*

**4. Acessar o sistema**
Abra `http://localhost:8080` no navegador assim que aparecer a mensagem `Started SistemaAcademiaApplication` no terminal.

### ⚠️ Problemas Comuns (Troubleshooting)

| Erro Apresentado | Solução |
| :--- | :--- |
| `Communications link failure` | O MySQL não está ligado. Inicie o serviço do MySQL na sua máquina. |
| `Access denied for user 'root'` | A senha configurada no `application.properties` não confere com a do seu MySQL local. |
| `Port 8080 was already in use` | Há outra instância do sistema rodando. Feche-a ou mude a porta com `server.port` no `application.properties`. |

---

## 🔑 Acessos para Teste

| Perfil | Login | Senha |
| :--- | :--- | :--- |
| **Administrador** | `admin` | `123` |
| **Professor / Aluno** | CPF ou e-mail cadastrado pelo administrador | `123` *(senha inicial, que pode ser trocada pelo próprio usuário)* |

> **Nota:** O login e a senha do administrador podem ser alterados no `application.properties` (`gymsync.admin.usuario` e `gymsync.admin.senha`). Para cadastrar novas pessoas no sistema, é necessário utilizar CPFs matematicamente válidos (o validador confere os dígitos verificadores).

---

## 📄 Material de Apresentação

Os documentos acadêmicos e de modelagem do projeto estão localizados na pasta `docs/`:
* Proposta do projeto (documento Word);
* Canvas do projeto;
* Slides da apresentação.

---

## 🔭 Próximos Passos (Roadmap)

* [ ] Pagamento online real (Pix ou cartão) integrado com provedor de pagamento (substituindo a simulação);
* [ ] Integração via hardware com leitor biométrico e catraca física;
* [ ] Publicação do sistema em nuvem e aplicativo mobile para o aluno;
* [ ] Disparo de avisos automáticos por e-mail ou API do WhatsApp;
* [ ] Chat em tempo real (WebSockets) e recuperação de senha por link enviado ao e-mail.
