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
Planilha Excel ──▶ Vue 3 (Front-end) ──▶ Spring Boot (API REST) ──▶ Python (Pandas + SciPy)
                        ▲                        │
                        │                        ▼
                   Dashboards ◀──── JSON ──── PostgreSQL
```

1. O consultor faz login no front e envia a planilha. A API devolve um token (JWT), e o front o manda em todos os pedidos.
2. O Spring Boot valida o arquivo e chama o script Python pelo `ProcessBuilder`.
3. O Python limpa, padroniza e valida cada linha, calcula as estatísticas e os testes, e escreve os insights em texto.
4. O Spring Boot grava os clientes (atualizando pelo código CTI), os contratos, os indicadores e os insights.
5. O dashboard lê os resultados pela API.

## Estrutura do repositório

| Pasta | Conteúdo | Disciplina |
| --- | --- | --- |
| [`FrontEnd/`](FrontEnd/) | Vue 3, Vite, Tailwind, Pinia, Vue Router e Axios | Frameworks Front-end |
| [`BackEnd/`](BackEnd/) | Java 21 e Spring Boot 4 (Web MVC, Data JPA, Security com JWT, Validation) | Desenvolvimento Back-end |
| [`CienciaD/`](CienciaD/) | Python com Pandas, SciPy e Matplotlib: limpeza, estatística, gráficos e insights | Ciência de Dados |
| [`database/`](database/) | `schema.sql`: a estrutura do banco no PostgreSQL | Back-end / Nuvem |

O código do back-end fica em `BackEnd/src/main/java/br/com/cti/insights/` e segue as camadas vistas em aula:

| Pacote | Papel |
| --- | --- |
| `controller` | Porta HTTP. Recebe o JSON ou o arquivo e devolve a resposta com o status certo. |
| `service` | Regras de negócio: e-mail único, login, importação da planilha. |
| `repository` | Acesso ao banco com Spring Data JPA. |
| `model` | Entidades: `Cliente`, `Consultor`, `Contrato`, `Servico`, `Insight` (com subclasses), `Telemetria`, `Analise` e `Usuario`. |
| `dto` | O que entra e sai da API. A senha nunca sai. |
| `exception` | Exceções próprias (`ExcelInvalidoException`, `ColunaObrigatoriaException`, `ClienteDuplicadoException`...) e o `GlobalExceptionHandler`, que transforma cada uma numa resposta clara. |
| `analise` | A chamada ao script Python. |

## Como rodar na sua máquina

**Pré-requisitos:**
- JDK 21 ou mais novo (o 25 também serve);
- Node.js 22 ou mais novo;
- Python 3.10 ou mais novo;
- um PostgreSQL.

O banco pode ser local (instalado ou pelo Docker) ou na nuvem.

**1. Dependências do Python** (uma vez):

```bash
cd CienciaD
py -m pip install -r requirements.txt      # Linux/Mac: python3 -m pip ...
```

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

Abra `/cadastro`, crie a conta de consultor e envie a planilha de exemplo [`FrontEnd/planilhas-exemplo/CTI_Insights_modelo_upload_aula.xlsx`](FrontEnd/planilhas-exemplo/).

### Variáveis de ambiente

| Variável | Onde | Para quê | Padrão |
| --- | --- | --- | --- |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Back-end | Conexão com o PostgreSQL | `localhost`, `5432`, `cti`, `postgres`, `postgres` |
| `DB_SSLMODE` | Back-end | Use `require` no Azure e em qualquer banco na nuvem | `prefer` |
| `JWT_SECRET` | Back-end | Chave que assina o token de login. **Obrigatória em produção**, com 32 caracteres ou mais. (`JWT_SEGREDO` também é aceito.) | Aleatória a cada inicialização |
| `JWT_VALIDADE` | Back-end | Por quanto tempo o login vale | `8h` |
| `CORS_ORIGIN` | Back-end | Endereços do front que podem chamar a API, separados por vírgula. (`CORS_ORIGENS` também é aceito.) | `http://localhost:5173` |
| `PYTHON_CMD` | Back-end | Executável do Python | `py` no Windows, `python3` nos outros |
| `PYTHON_SCRIPT` | Back-end | Caminho do `main.py` | `../CienciaD/main.py` |
| `VITE_API_URL` | Front-end | Endereço da API. Com ou sem `/api` no final: o front acrescenta quando falta. | `http://localhost:8080` |

## API

Todas as rotas, menos o cadastro e o login, exigem o header `Authorization: Bearer <token>`.

Todo usuário tem um **perfil**, `USER` ou `ADMIN`, que vai dentro do token. O cadastro sempre cria `USER`. Para promover alguém, use o banco (por exemplo, no DBeaver):

```sql
UPDATE usuarios SET perfil = 'ADMIN' WHERE email = 'pessoa@empresa.com';
```

Por enquanto nenhuma rota é exclusiva de `ADMIN`. O perfil já chega ao Spring Security como `ROLE_USER` ou `ROLE_ADMIN`, então restringir uma rota é só acrescentar `.hasRole("ADMIN")` à regra no `SecurityConfig`. A pessoa precisa entrar de novo depois da promoção, para receber um token com o perfil novo.

| Verbo | Rota | O que faz |
| --- | --- | --- |
| POST | `/api/usuarios` | Cadastro do consultor: nome, e-mail, senha (8 a 72 caracteres, com letras e números), consultor na planilha e matrícula opcional. O e-mail é gravado em minúsculas. |
| POST | `/api/usuarios/login` | Login. Devolve `{ token, expiraEm, usuario }` |
| GET | `/api/usuarios/me` | Quem está logado, lido do próprio token |
| GET, PUT, DELETE | `/api/usuarios`, `/api/usuarios/{id}` | Consultar, alterar e remover usuários |
| POST | `/api/planilhas` | Upload da planilha (multipart, campo `arquivo`). Trata, analisa e grava. |
| GET | `/api/indicadores` | Estatísticas da última planilha (frequências, medidas, testes, contratações por mês) |
| GET | `/api/insights` | Insights da última planilha |
| GET | `/api/analises`, `/api/analises/ultima`, `/api/analises/{id}`, `/api/analises/{id}/problemas` | Histórico de planilhas processadas e as linhas recusadas de cada uma |
| GET, POST, PUT, DELETE | `/api/clientes`, `/api/clientes/{id}` | CRUD de clientes |
| PATCH | `/api/clientes/{id}/nivel` | Altera só o nível A/B/C |
| GET, POST | `/api/clientes/{id}/contratos` | Contratos do cliente |
| PATCH | `/api/contratos/{id}/cancelamento` | Cancela o contrato, que continua no histórico |
| DELETE | `/api/contratos/{id}` | Remove o contrato |

Os erros vêm no formato `{ "mensagem": "..." }`. A validação traz também `campos`, com a mensagem de cada campo recusado:

| Status | Quando acontece |
| --- | --- |
| 400 | Dados inválidos ou arquivo ilegível |
| 401 | Sem login ou token vencido (esta resposta vem sem corpo) |
| 404 | O registro não existe |
| 409 | E-mail, código CTI ou matrícula repetidos |
| 413 | Arquivo acima de 25 MB |
| 422 | Falta uma coluna obrigatória na planilha |

## Testes

```bash
cd BackEnd && ./mvnw test                                  # API, regras, upload com Python e schema.sql
cd CienciaD && py -m unittest discover -s tests -t .       # limpeza, estatística e linha de comando
```

Os testes do back-end usam o H2 em memória, então não precisam de PostgreSQL. O teste de upload roda o Python de verdade; sem Python com pandas, ele é pulado.

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

- [`CienciaD/README.md`](CienciaD/README.md): tratamento dos dados, métodos estatísticos, interpretação dos testes e o contrato com o back-end.
- [`FrontEnd/README.md`](FrontEnd/README.md): telas, regras de padronização da planilha e onde mexer.
