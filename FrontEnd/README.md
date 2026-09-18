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
| `/login` | Login | Formulário de acesso (**sem autenticação ainda**) |
| `/sidebar/upload` | Upload | Recebe a planilha, trata e mostra a prévia |
| `/sidebar/relatorio` | Relatórios | Análise da carteira carregada, com filtros e exportação CSV |
| `/sidebar/dashboard` | Dashboard | Indicadores de processamento (**dados de exemplo**) |

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
| `src/stores/uploadStore.js` | Leitura do arquivo, estado e **ponto de integração com a API** |
| `src/pages/Upload.vue` | Tela de envio e prévia |
| `src/pages/Relatorio.vue` | Relatórios e exportação |
| `src/style.css` | Tokens de cor, sombra e animação (Tailwind v4 `@theme`) |
| `src/dados/painel.js` | Dados de exemplo do Dashboard — trocar por API |

Coluna nova é editar só o `ESQUEMA` em `esquemaPlanilha.js`: a prévia, a
exportação e as tabelas percorrem essa lista.

## Próxima etapa: backend e Azure

**Nada sai da máquina ainda.** O envio é simulado.

```
Hoje    Planilha → Vue Upload → Pinia → Tratamento → Prévia
Depois  Planilha → Vue Upload → Pinia → Axios → Spring Boot → PostgreSQL (Azure)
```

O Vue **não acessa o PostgreSQL diretamente**. Quem grava no banco é o Spring Boot.

O ponto de integração é a função `enviar()` em `src/stores/uploadStore.js`, marcada
com `--- TROQUE DAQUI ---`. O `FormData` já está montado com o arquivo original e
a contagem de linhas. Falta decidir com o backend:

- **Enviar o arquivo original** e reprocessar no Java com Apache POI — o
  tratamento do navegador vira só pré-visualização; ou
- **Enviar as linhas já tratadas** (`JSON.stringify(dados)`) — menos trabalho no
  servidor, mas ele **ainda precisa revalidar**: dado que chega do navegador é
  dado que o usuário pode ter adulterado.

O arquivo futuro é `src/services/api.js`:

```js
import axios from 'axios'

export const api = axios.create({
  // Nunca fixe o host: no build para o Azure isto aponta para a máquina errada.
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
})
```

### Quatro pedras no caminho, já visíveis no backend atual

Verificadas em `BackEnd/src/main/java/com/synthetix/cti/config/SecurityConfig.java`
e `application.properties`.

**1. CORS não está configurado.** O Vue roda em `:5173` e o Spring em `:8080` —
origens diferentes. O navegador bloqueia o POST antes mesmo de sair. É o primeiro
erro que vai aparecer. Em desenvolvimento, o caminho limpo é o proxy do Vite, que
faz o navegador falar só com `:5173`:

```js
// vite.config.js
export default defineConfig({
  plugins: [vue(), tailwindcss()],
  server: {
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } },
  },
})
```

Em produção, se front e back ficarem em domínios distintos no Azure, aí sim o
Spring precisa liberar CORS de verdade.

**2. Tudo exige autenticação.** O `SecurityConfig` tem
`.anyRequest().authenticated()` com `httpBasic`. Um `POST /api/upload` sem
credencial volta **401**, e o `WWW-Authenticate: Basic` faz o navegador abrir
aquele popup nativo de senha — surpresa ruim no meio de uma demonstração. Ou
libere a rota de upload, ou mande credencial/token pelo Axios.

**3. O limite de upload não bate.** Esta tela aceita até 25 MB, mas o padrão do
Spring Boot é **1 MB por arquivo** e 10 MB por requisição. Uma planilha de 5 MB
morre com `MaxUploadSizeExceededException`. Alinhe os dois lados:

```properties
spring.servlet.multipart.max-file-size=25MB
spring.servlet.multipart.max-request-size=30MB
```

**4. O comentário do CSRF fica desatualizado.** Ele diz "API stateless consumida
por clientes nao-browser" — e a partir da integração o cliente passa a ser um
navegador. Com Basic + stateless, manter o CSRF desligado continua defensável,
mas a justificativa escrita ali deixa de valer.

Também ficam para esta etapa: autenticação de verdade (hoje o login e o botão
Sair só navegam), persistência no PostgreSQL e a publicação no Azure.

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
reporta as demais como ignoradas. São 36 linhas com **3 erros propositais** —
faturamento em branco, consultor em branco e código repetido — além de 23
grafias diferentes de segmento, que o dicionário reduz a 6.

### Cuidado ao ler células com o SheetJS

A leitura usa `sheet_to_json(..., { raw: true })`, e isso **não é detalhe**.

Com `raw: false` o SheetJS renderiza a célula pelo formato numérico e devolve
`"R$ 1,850,000.00"` e `"1/15/25"` — separador de milhar e ordem de data
americanos, independente do locale da máquina. O tratamento tentaria converter
esse texto de volta e falharia em **todas** as linhas. Com `raw: true`, número
chega como número e data como `Date`; só o CSV, que não carrega tipo, continua
vindo como texto.
