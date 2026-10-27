# Sistema CTI Insights

Projeto Integrador II (Tecnologia em Análise e Desenvolvimento de Sistemas, SENAI "Roberto Mange"). A empresa parceira é a **Provedor CTI** (Cti Comunicação de Dados e Tecnologia LTDA).

> **Pergunta-guia:** como transformar a planilha Excel da CTI em um sistema web que trata os dados automaticamente e mostra insights estratégicos para a empresa?

A CTI mantém sua carteira de clientes em planilhas preenchidas à mão, e por isso:
- a mesma informação aparece escrita de formas diferentes ("IND.", "Industria", "INDUSTRIA");
- há campos em branco;
- não existe nenhuma análise sobre segmentos, níveis A/B/C ou serviços.

O CTI Insights recebe a planilha, padroniza e valida os dados, grava tudo num banco PostgreSQL, calcula estatísticas e mostra os resultados em dashboards.

## Como as partes se conectam

```
Planilha ─▶ Pinia valida ─▶ Spring valida e grava o lote ─▶ POST /etl ─▶ Python (FastAPI + pandas)
                                                                              │ limpa, calcula, grava
   Dashboard (Chart.js) ◀── GET /api/dashboard ◀── Spring lê ◀── PostgreSQL ◀─┘
```

É um ETL (aula 11): **E**xtrair, o Spring entrega ao Python só as linhas válidas, em JSON; **T**ransformar, o Python limpa, padroniza, calcula e escreve as frases; **C**arregar, o Python grava no banco.

1. O consultor faz login no front e carrega a planilha. O Pinia valida no navegador e o Spring valida de novo (`ClienteDTO` + `ClienteValidacaoService`, aula 10).
2. No envio, o Spring grava o lote (`upload_lote`, status `AGUARDANDO_ETL`) e as linhas válidas (`upload_linha`).
3. Depois do commit, o `PythonEtlService` faz `POST /etl` na API do Python com `{loteId, recebidas, linhas}`.
4. O Python (`analytics-python/`) limpa (`limpeza.py`), calcula média, mediana, desvio e agrupamentos (`analise.py`), escreve os insights (`insights.py`) e grava clientes, contratos e o JSON do dashboard em `estatistica_lote` (`banco.py`). O lote vira `PROCESSADO`, ou `ERRO` com a mensagem.
5. O dashboard lê esse JSON por `GET /api/dashboard` e desenha os gráficos com Chart.js.

## Estrutura do repositório

| Pasta | Conteúdo | Disciplina |
| --- | --- | --- |
| [`FrontEnd/`](FrontEnd/) | Vue 3, Vite, Tailwind, Pinia, Vue Router e Axios | Frameworks Front-end |
| [`BackEnd/`](BackEnd/) | Java 21 e Spring Boot 4 (Web MVC, Data JPA, Security com JWT, Validation) | Desenvolvimento Back-end |
| [`analytics-python/`](analytics-python/) | Python com FastAPI, pandas, SciPy, SQLAlchemy e Matplotlib: o ETL que limpa, calcula, gera os insights e grava no banco, e o relatório com os testes estatísticos e os 4 gráficos (`output/`) | Ciência de Dados |
| [`database/`](database/) | `schema.sql`: a estrutura do banco no PostgreSQL. `migracao-aula11.sql`: renomeia as tabelas antigas para as da aula 11 | Back-end / Nuvem |

O código do back-end fica em `BackEnd/src/main/java/br/com/cti/insights/` e segue as camadas vistas em aula:

| Pacote | Papel |
| --- | --- |
| `controller` | Porta HTTP. Recebe o JSON ou o arquivo e devolve a resposta com o status certo. Aqui também fica o `ErroHandler`, que transforma cada erro numa resposta JSON clara. |
| `service` | Regras de negócio: e-mail único, login, gravação do lote (`UploadService`), chamada ao Python (`PythonEtlService`), leitura do dashboard e a `Telemetria` de cada envio e de cada execução do Python (`TelemetriaService`). |
| `repository` | Acesso ao banco com Spring Data JPA. |
| `model` | Entidades: `Cliente`, `Consultor`, `Contrato`, `Servico`, `UploadLote`, `UploadLinha`, `EstatisticaLote`, `Insight` (com subclasses), `Telemetria` e `Usuario`. |
| `dto` | O que entra e sai da API, com as validações declarativas (`@NotBlank`, `@Positive`, `@Pattern`...). `ClienteDTO` é uma linha da planilha; os `...RespostaDTO` são o que a API devolve. A senha nunca sai. |
| `exception` | Exceções próprias tratadas pelo `ErroHandler`, entre elas as três do plano: `ExcelInvalidoException` (formato não aceito ou planilha sem linhas), `ColunaObrigatoriaException` (falta uma coluna, ex. "segmento") e `ClienteDuplicadoException` (código CTI repetido). |
| `config` | Segurança, CORS e `AppProperties` (endereço e chave da API do Python). |

## Como rodar na sua máquina

**Pré-requisitos:**
- JDK 21 ou mais novo (o 25 também serve);
- Node.js 22 ou mais novo;
- Python 3.11 ou mais novo;
- um PostgreSQL.

O banco pode ser local (instalado ou pelo Docker) ou na nuvem.

**1. API do Python** (`http://localhost:8000`), ligada **antes** de enviar a planilha:

```bash
cd analytics-python
python -m venv .venv
.venv\Scripts\Activate.ps1                 # Linux/Mac: source .venv/bin/activate
pip install -r requirements.txt
copy .env.example .env                     # Linux/Mac: cp; preencha com os dados do banco
python testar_conexao.py                   # deve mostrar "Conectou!"
uvicorn api:app --port 8000                # confira http://localhost:8000/saude
```

Relatório em arquivos, sem banco: `python relatorio.py` lê `amostras/lote_exemplo.json` e gera em `output/` o `indicadores.json` (com os testes de Shapiro-Wilk e qui-quadrado), o `insights.json` e os 4 gráficos (linha, dispersão, histograma e boxplot).

Sem o Python no ar, o lote fica com status `ERRO` e a mensagem "Não foi possível falar com o serviço Python". Para testar só o Python, sem banco: `http://localhost:8000/docs` > `POST /testar` com o conteúdo de `amostras/clientes.json`.

**Banco que já existia antes da aula 11:** rode [`database/migracao-aula11.sql`](database/migracao-aula11.sql) uma vez (no DBeaver) **antes** de subir o back-end novo. Ele renomeia `clientes`, `contratos`, `consultores` e `servicos` para os nomes que o `banco.py` usa, sem perder dados.

**2. Back-end** (`http://localhost:8080`):

```bash
cd BackEnd
./mvnw spring-boot:run
```

Para usar o banco da Azure, copie `BackEnd/src/main/resources/application-local.properties.exemplo` para `application-local.properties` (o arquivo fica fora do Git), preencha com os dados do servidor e rode com o perfil `local`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local                 # Linux/macOS/Git Bash
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"           # Windows (PowerShell)
```

Sem perfil nem variáveis de ambiente, ele procura um PostgreSQL em `localhost:5432`, banco `cti`, usuário e senha `postgres`. As tabelas são criadas sozinhas na primeira execução. Para subir um banco assim com Docker:

```bash
docker run -d --name cti-postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=cti -p 5432:5432 postgres:17
```

**3. Front-end** (`http://localhost:5173`):

```bash
cd FrontEnd
npm install
npm run dev
```

Abra `/cadastro`, crie a conta de consultor e envie a planilha de exemplo [`FrontEnd/planilhas-exemplo/dados.xlsx`](FrontEnd/planilhas-exemplo/).

### Variáveis de ambiente

| Variável | Onde | Para quê | Padrão |
| --- | --- | --- | --- |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Back-end | Conexão com o PostgreSQL | `localhost`, `5432`, `cti`, `postgres`, `postgres` |
| `DB_SSLMODE` | Back-end | Use `require` no Azure e em qualquer banco na nuvem | `prefer` |
| `JWT_SECRET` | Back-end | Chave que assina o token de login. **Obrigatória em produção**, com 32 caracteres ou mais. (`JWT_SEGREDO` também é aceito.) | Aleatória a cada inicialização |
| `JWT_VALIDADE` | Back-end | Por quanto tempo o login vale | `8h` |
| `CORS_ORIGIN` | Back-end | Endereços do front que podem chamar a API, separados por vírgula. (`CORS_ORIGENS` também é aceito.) | `http://localhost:5173` |
| `ETL_URL` | Back-end | Endereço da API do Python | `http://localhost:8000/etl` |
| `ETL_KEY` | Back-end | Chave enviada no cabeçalho `X-ETL-Key`; igual ao `ETL_API_KEY` do Python | vazia (sem chave) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Back-end | Primeiro administrador, garantido ao subir (a senha só é usada se a conta não existir) | vazios (nenhum) |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_SSLMODE` | Python (`analytics-python/.env`) | O mesmo banco do back-end | `localhost`, `5432`, `ctiinsights`, `cti`, `cti123`, `prefer` |
| `ETL_API_KEY` | Python (`.env`) | Chave exigida no `POST /etl` (errada = 401) | vazia (sem chave) |
| `VITE_API_URL` | Front-end | Endereço da API. Com ou sem `/api` no final: o front acrescenta quando falta. | `http://localhost:8080` |

## API

Todas as rotas, menos o cadastro e o login, exigem o header `Authorization: Bearer <token>`.

**Aprovação de contas.** O cadastro é público, mas a conta nasce **pendente** e só entra depois que um administrador aprova na tela **Usuários** (menu visível só para `ADMIN`). Cada conta tem:
- um **perfil**, `USER` ou `ADMIN`, que vai dentro do token;
- um **status**, `PENDENTE`, `APROVADO` ou `BLOQUEADO`.

Bloquear tira o acesso na hora: a cada pedido o servidor confere se a conta continua aprovada, mesmo com o token ainda válido.

**Primeiro administrador:** defina `ADMIN_EMAIL` (e `ADMIN_PASSWORD`, se a conta ainda não existir) e suba o back-end. A conta vira `ADMIN` e aprovada. Depois disso, os próprios administradores promovem outras pessoas na tela Usuários; o perfil novo vale a partir do próximo login. As contas que já existiam antes da aprovação continuam entrando (status `APROVADO`).

| Verbo | Rota | O que faz |
| --- | --- | --- |
| POST | `/api/usuarios` | Cadastro do consultor: nome (o mesmo da coluna consultor da planilha), e-mail, senha (8 a 72 caracteres, com letras e números) e matrícula opcional. `consultor` só é preciso se for diferente do nome. O e-mail é gravado em minúsculas. |
| POST | `/api/usuarios/login` | Login. Devolve `{ token, expiraEm, usuario }` |
| GET | `/api/usuarios/me` | Quem está logado, lido do próprio token |
| GET, PUT, DELETE | `/api/usuarios`, `/api/usuarios/{id}` | Consultar, alterar e remover usuários (**só ADMIN**) |
| GET | `/api/admin/usuarios?status=` | Contas para aprovação, filtradas por `PENDENTE`, `APROVADO` ou `BLOQUEADO` (**só ADMIN**) |
| PATCH | `/api/admin/usuarios/{id}/status`, `/api/admin/usuarios/{id}/perfil` | Aprovar, bloquear ou reativar; tornar ou deixar de ser `ADMIN` (**só ADMIN**; ninguém altera a própria conta) |
| POST | `/api/clientes/validar` | Recebe em JSON as linhas que o Pinia aprovou, valida de novo (DTO + `ClienteValidacaoService`: código repetido e padronização) e devolve a lista padronizada. Não grava e não chama Python. |
| POST | `/api/uploads/validadas?arquivo=&recebidas=` | Recebe as linhas que o Pinia aprovou (lista de `ClienteDTO`), valida de novo, grava o lote em `AGUARDANDO_ETL` e chama o Python. Responde 201 com o lote. |
| GET | `/api/uploads` | Lotes enviados, do mais recente para o mais antigo, com status e mensagem |
| GET | `/api/dashboard?loteId=` | `{ lote, geradoEm, estatisticas }`: o JSON que o Python gravou em `estatistica_lote`. Sem `loteId`, o último lote processado. `estatisticas` vem nulo enquanto o Python não terminou. |
| GET, POST, PUT, DELETE | `/api/clientes`, `/api/clientes/{id}` | CRUD de clientes |
| PATCH | `/api/clientes/{id}/nivel` | Altera só o nível A/B/C |
| GET, POST | `/api/clientes/{id}/contratos` | Contratos do cliente |
| PATCH | `/api/contratos/{id}/cancelamento` | Cancela o contrato, que continua no histórico |
| DELETE | `/api/contratos/{id}` | Remove o contrato |

Os erros vêm no formato `{ "mensagem": "..." }`. Na validação vêm também `campo` e `erro` (o primeiro problema), `posicao` (o item da lista, quando o corpo é uma lista) e `campos` (todos). Uma regra do Service vem como `{ "mensagem": "Regra de validação não atendida", "erro": "Código repetido na planilha: CTI001" }`:

| Status | Quando acontece |
| --- | --- |
| 400 | Dados inválidos ou formato de arquivo não aceito |
| 401 | Sem login, token vencido ou conta bloqueada durante a sessão |
| 403 | Conta pendente ou bloqueada no login, ou rota só de `ADMIN` |
| 404 | O registro não existe |
| 409 | E-mail, código CTI ou matrícula repetidos |
| 422 | Falta uma coluna obrigatória na planilha |

## Testes

```bash
cd BackEnd && ./mvnw test                                  # API, regras, envio do lote ao Python e schema.sql
```

Os testes do back-end usam o H2 em memória, então não precisam de PostgreSQL nem do Python: o teste do `PythonEtlService` sobe um servidor HTTP falso no lugar da API do Python e confere o JSON e a chave enviados.

O Python se testa como na aula: `POST /testar` com `amostras/clientes.json` deve dar 2 clientes e média 1.135.000. Com a planilha de exemplo, o lote deve terminar `PROCESSADO` com 32 clientes, 61 contratos e média 2.261.250 (consultas no DBeaver no PASSO 12 do guia da aula).

Mudou uma entidade? Regere o [`database/schema.sql`](database/schema.sql) com:

```bash
cd BackEnd && ./mvnw test -Dtest=SchemaSqlTests -DatualizarSchema=true
```

## Dados e privacidade

A planilha deste repositório é **fictícia**, feita para a aula. Os dados reais da CTI são confidenciais e usados só para fins acadêmicos: não devem ser commitados nem publicados.

No sistema:
- as senhas ficam guardadas só como hash;
- o acesso exige login;
- a API só aceita chamadas dos endereços do front configurados.

## Mais detalhes

- [`FrontEnd/README.md`](FrontEnd/README.md): telas, regras de padronização da planilha e onde mexer.
