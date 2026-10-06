# CTI Insights — Front-end

Interface em Vue 3 + Vite para receber planilhas de clientes, tratar os dados no
navegador e apresentar os indicadores da carteira.

## Como rodar

```bash
cd FrontEnd
npm install
npm run dev
```

O servidor sobe em `http://localhost:5173`. O `package.json` fica em `FrontEnd/`,
não na raiz do repositório — rodar `npm run dev` na raiz falha com `ENOENT`.

| Comando | O que faz |
| --- | --- |
| `npm run dev` | Servidor de desenvolvimento com recarga |
| `npm run build` | Build de produção em `dist/` |
| `npm run preview` | Serve o `dist/` para conferência |

## Telas

| Rota | Tela | O que faz |
| --- | --- | --- |
| `/` | Home | Página institucional |
| `/login` | Login | Entra com e-mail e senha conferidos na API; guarda o token (JWT) |
| `/cadastro` | Cadastro | O consultor cria a própria conta e já entra |
| `/upload` | Upload | Prévia e validação no navegador; o envio vai para `POST /api/planilhas` (**exige login**) |
| `/relatorio` | Relatórios | Análise da carteira carregada, com filtros e exportação CSV (**exige login**) |
| `/dashboard` | Dashboard | Gráficos por segmento, nível A/B/C e evolução de contratações, insights e testes da última planilha, lidos de `/api/indicadores` (**exige login**) |

O endereço da API vem de `VITE_API_URL` (padrão `http://localhost:8080`; o `/api` é acrescentado quando falta).

## Fluxo do upload

1. A pessoa escolhe um `.xlsx`, `.xls` ou `.csv` (até 25 MB).
2. O arquivo é lido com **SheetJS** e vira uma matriz de linhas.
3. Cada linha passa pelo tratamento e vira um objeto JavaScript padronizado.
4. Linha com problema vai para a lista de erros; as demais seguem.
5. Tudo fica no store do **Pinia**, que a tela de Relatórios lê sem recarregar.

### Colunas esperadas

`codigo_cliente` · `nome_cliente` · `consultor` · `segmento` · `nivel_cliente` ·
`faturamento_anual` · `servicos_contratados` · `data_contratacao` · `cidade` · `uf`

O cabeçalho é reconhecido por apelidos (sem acento, sem caixa, sem pontuação),
então `codigo_cliente`, `Código Cliente` e `CODIGO CLIENTE` casam com a mesma
coluna. Coluna fora do esquema é lida e reportada, não descartada em silêncio.

### O que o tratamento padroniza

| Campo | Regra |
| --- | --- |
| Todos os textos | Remove espaço sobrando, inclusive o espaço não separável que vem de cópia da web |
| `segmento` | Dicionário de sinônimos → forma canônica. `IND.`, `Industria` e `INDÚSTRIA` viram **Indústria** |
| `consultor`, `cidade` | Nome próprio. `ANA SOUZA` vira **Ana Souza** |
| `nivel_cliente` | Caixa-alta; fora de A/B/C a linha é rejeitada |
| `faturamento_anual` | Aceita `R$ 1.850.000,00` e `1850000.00` → número |
| `servicos_contratados` | Separa por `;` e canoniza cada item |
| `data_contratacao` | `dd/mm/aaaa` → `Date`, rejeitando data inexistente como `31/02` |
| `uf` | Caixa-alta, validada contra as 27 siglas |
| `codigo_cliente` | Caixa-alta; código repetido é rejeitado |

Segmento ou serviço fora do dicionário **não bloqueia** a linha: entra como aviso,
para alguém decidir se vira cadastro novo.

## Onde mexer

| Arquivo | Para quê |
| --- | --- |
| `src/dados/esquemaPlanilha.js` | Colunas, apelidos, dicionários e regras de tratamento |
| `src/stores/uploadStore.js` | Leitura do arquivo, estado e envio para a API |
| `src/pages/Upload.vue` | Tela de envio e prévia |
| `src/pages/Relatorio.vue` | Relatórios e exportação |
| `src/style.css` | Tokens de cor, sombra e animação (Tailwind v4 `@theme`) |
| `src/pages/Dashboard.vue` | Gráficos, insights e testes da última planilha (dados da API) |
| `src/services/api.js` | Axios, token e mensagens de erro da API |

Coluna nova é editar só o `ESQUEMA` em `esquemaPlanilha.js`: a prévia, a
exportação e as tabelas percorrem essa lista.

## Integração com o back-end

```
Planilha → Vue Upload (prévia no navegador) → Axios → Spring Boot → Python → PostgreSQL
                                                          ↓
                              Dashboard ← GET /api/indicadores e /api/analises/ultima
```

O Vue **não acessa o PostgreSQL diretamente**: quem grava no banco é o Spring Boot.

- **`src/services/api.js`**: instância do Axios. Põe o token em cada pedido e, se a API
  responder 401 com token, encerra a sessão e volta ao login. Também traduz o erro
  `ProblemDetail` da API na frase mostrada na tela (`mensagemDeErro`).
- **`enviar()` em `src/stores/uploadStore.js`**: manda o **arquivo original** para
  `POST /api/planilhas`. O servidor trata tudo de novo no Python, porque dado que vem
  do navegador pode ter sido adulterado. A prévia daqui serve para a pessoa corrigir a
  planilha antes de enviar.
- **`src/stores/authStore.js`**: login, cadastro e o token (JWT) no `localStorage`.
  A senha nunca é guardada.
- **`src/router/index.js`**: Upload, Dashboard e Relatório exigem login.

Variável de ambiente: `VITE_API_URL` (padrão `http://localhost:8080`, com ou sem `/api`). Na Vercel,
defina-a no painel e publique de novo, porque ela entra no código no momento do build.

## Aviso de segurança da dependência `xlsx`

`npm audit` acusa duas falhas de severidade alta no `xlsx` (prototype pollution e
ReDoS) com **"No fix available"**. Não é bug do npm: o SheetJS parou de publicar
no registro, e a última versão lá é a `0.18.5`, de março de 2022. As correções só
saem pelo CDN próprio deles.

Mitigação já aplicada no código: a planilha é lida com `header: 1`, o que devolve
matriz de linhas em vez de deixar a biblioteca montar objetos com os nomes do
cabeçalho como chave. Uma coluna chamada `__proto__` nunca vira chave de objeto
nosso.

Para eliminar o aviso, instale a versão corrigida na origem:

```bash
npm uninstall xlsx
npm install https://cdn.sheetjs.com/xlsx-0.20.3/xlsx-0.20.3.tgz
```

O custo é que a dependência passa a vir de URL, não do registro — confira se o
pipeline de build alcança `cdn.sheetjs.com`.

## Pilha

Vue 3.5 · Vite 8 · Vue Router 4 · Pinia · Tailwind CSS 4 · SheetJS

Sem biblioteca de gráficos: os gráficos em `src/components/graficos/` são SVG e
HTML próprios, desenhados sobre os tokens do tema. As cores de dado foram
conferidas com validador de paleta (banda de luminosidade, piso de croma,
separação sob daltonismo e contraste) — não ajuste no olho.

## Planilha de exemplo

`planilhas-exemplo/CTI_Insights_modelo_upload_aula.xlsx` é o modelo da aula.
A pasta fica fora de `src/` e de `public/`, então nada dali entra no build.

O arquivo tem 6 abas; a aplicação lê **só a primeira** (`upload_clientes`) e
reporta as demais como ignoradas. São 39 linhas, das quais 31 passam e **8
reprovam** por 4 motivos distintos:

| Verificação | Onde |
| --- | --- |
| Campos obrigatórios vazios (3) | `F32` faturamento, `G33` serviços, `C34` consultor |
| Dados fora do padrão (4) | nível `F` na linha 2 e `E` nas linhas 38–40 |
| Registros duplicados (1) | `CTI035` repetido nas linhas 36 e 37 |

Passam com aviso outras 34 padronizações: 23 grafias de segmento que o
dicionário reduz a 6, consultores em caixa-alta e o código `Cti004`.

Duas armadilhas de leitura deste arquivo:

- As **células vazias são ausentes**, não string vazia — `ws['F32']` é
  `undefined`, e não `{ v: '' }`. Por isso o tratamento não pode confiar em
  comparar com `''`: quem decide é a flag `obrigatoria` do `ESQUEMA`, sobre o
  valor já normalizado.
- A aba `LEIA-ME` do arquivo promete "espaços extras" que **não existem**.
  Varredura célula a célula: nenhuma tem espaço nas pontas, dobrado ou nbsp.
  O contador "Espaços extras removidos" marcar 0 é o resultado correto. Ela
  também não menciona os níveis inválidos, que são o defeito mais visível.

### Cuidado ao ler células com o SheetJS

A leitura usa `sheet_to_json(..., { raw: true })`, e isso **não é detalhe**.

Com `raw: false` o SheetJS renderiza a célula pelo formato numérico e devolve
`"R$ 1,850,000.00"` e `"1/15/25"` — separador de milhar e ordem de data
americanos, independente do locale da máquina. O tratamento tentaria converter
esse texto de volta e falharia em **todas** as linhas. Com `raw: true`, número
chega como número e data como `Date`; só o CSV, que não carrega tipo, continua
vindo como texto.
