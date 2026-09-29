// Esquema da planilha de clientes e regras de tratamento.
//
// Modelado sobre a planilha real: codigo_cliente, nome_cliente, consultor,
// segmento, nivel_cliente, faturamento_anual, servicos_contratados,
// data_contratacao, cidade, uf.
//
// `apelidos` existe porque cabeçalho de planilha nunca vem padronizado. A
// comparação é feita sobre a forma achatada (sem acento, minúscula, sem espaço
// nem pontuação sobrando), então listar a variação esperada basta.

export const NIVEIS = ['A', 'B', 'C']

export const UFS = [
  'AC', 'AL', 'AM', 'AP', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MG', 'MS', 'MT',
  'PA', 'PB', 'PE', 'PI', 'PR', 'RJ', 'RN', 'RO', 'RR', 'RS', 'SC', 'SE', 'SP', 'TO',
]

// Dicionário de segmentos.
//
// É o coração do tratamento. Caixa-alta e remoção de acento resolvem
// "Indústria" x "industria", mas NÃO resolvem "IND." — que na planilha real
// aparece como abreviação. Sem sinônimo declarado, "IND." viraria um segmento
// próprio e o agrupamento mentiria. Acrescente aqui toda abreviação nova que
// aparecer, em vez de tentar adivinhar por regra.
export const SEGMENTOS = [
  { canonico: 'Indústria', sinonimos: ['industria', 'ind', 'industrial', 'fabrica'] },
  { canonico: 'Comércio', sinonimos: ['comercio', 'com', 'comercial', 'varejo'] },
  { canonico: 'Serviços', sinonimos: ['servicos', 'servico', 'serv'] },
  { canonico: 'Saúde', sinonimos: ['saude', 'hospitalar'] },
  { canonico: 'Tecnologia', sinonimos: ['tecnologia', 'tec', 'ti'] },
  { canonico: 'Educação', sinonimos: ['educacao', 'ensino', 'educacional'] },
]

// Serviços contratados. A célula é multivalorada: o padrão do dicionário é
// ponto e vírgula, mas vírgula e barra também separam — são as variações que
// mais aparecem, e sem isso "Internet Dedicada, Firewall" virava UM serviço
// inexistente e a empresa sumia da contagem dos dois. Nenhum nome do catálogo
// abaixo contém esses caracteres; se um dia contiver, SEPARADOR_SERVICOS
// precisa mudar junto. " e " fica de fora de propósito: é palavra comum e
// partiria um serviço que a tivesse no nome.
export const SEPARADOR_SERVICOS = /[;,/]/

export const SERVICOS = [
  { canonico: 'Internet Dedicada', sinonimos: ['internet dedicada', 'internet'] },
  { canonico: 'Link Empresarial', sinonimos: ['link empresarial', 'link'] },
  { canonico: 'MPLS', sinonimos: ['mpls'] },
  { canonico: 'Firewall', sinonimos: ['firewall'] },
  { canonico: 'Backup', sinonimos: ['backup'] },
  { canonico: 'Cloud', sinonimos: ['cloud', 'nuvem'] },
]

// ---------------------------------------------------------------------------
// Catálogo de tipos de problema
//
// O relatório precisa responder "quantos campos vazios, quantos duplicados" —
// e para isso o problema tem que NASCER com um tipo. Antes o motivo era só
// frase concatenada: dava para ler o erro, nunca para agrupar por natureza.
//
// O rótulo mora junto do código de propósito. Se a tela mantivesse o seu
// próprio dicionário de tipo → texto, um tipo novo apareceria no relatório
// como "espacos" até alguém lembrar de traduzi-lo no outro arquivo.
// ---------------------------------------------------------------------------

// Erro derruba a linha: ela não entra em dadosValidos.
export const TIPOS_ERRO = [
  { tipo: 'vazio', rotulo: 'Campos obrigatórios vazios' },
  { tipo: 'email', rotulo: 'E-mails inválidos' },
  { tipo: 'duplicado', rotulo: 'Registros duplicados' },
  { tipo: 'formato', rotulo: 'Dados fora do padrão' },
  { tipo: 'estrutura', rotulo: 'Problemas na estrutura do arquivo' },
]

// Aviso não derruba: a linha passa e o assunto é reportado. Os dois primeiros
// são correções que o tratamento JÁ aplicou — ficam registrados porque
// "identificar espaços desnecessários" é resultado de análise, e uma limpeza
// silenciosa não deixa a pessoa corrigir a planilha na origem.
// "Espaços em branco" foi trocado por "Espaços extras" de propósito. Em
// português a primeira forma se lê como CÉLULA vazia, e é assim que o
// enunciado e o dicionário a usam. Com o rótulo anterior, o relatório mostrava
// "Espaços em branco removidos: 0" numa planilha que tem três células vazias —
// parecia que a checagem havia falhado, quando as células vazias estão
// contadas em "Campos obrigatórios vazios", que é onde reprovam a linha.
// Este contador é só do trim: espaço nas pontas ou dobrado dentro do texto.
export const TIPOS_AVISO = [
  { tipo: 'espacos', rotulo: 'Espaços extras removidos' },
  { tipo: 'padronizacao', rotulo: 'Textos padronizados' },
  { tipo: 'dicionario', rotulo: 'Valores fora do dicionário' },
]

export function rotularTipo(tipo) {
  return [...TIPOS_ERRO, ...TIPOS_AVISO].find((t) => t.tipo === tipo)?.rotulo ?? tipo
}

export const ESQUEMA = [
  {
    chave: 'codigo',
    rotulo: 'Código',
    apelidos: ['codigo cliente', 'codigo', 'cod cliente', 'cod'],
    obrigatoria: true,
    tratamento: 'codigo',
  },
  {
    chave: 'nome',
    rotulo: 'Cliente',
    apelidos: ['nome cliente', 'cliente', 'nome', 'razao social'],
    obrigatoria: true,
    tratamento: 'nomeProprio',
  },
  {
    chave: 'consultor',
    rotulo: 'Consultor',
    apelidos: ['consultor', 'vendedor', 'responsavel'],
    obrigatoria: true,
    tratamento: 'nomeProprio',
  },
  {
    chave: 'segmento',
    rotulo: 'Segmento',
    apelidos: ['segmento', 'setor', 'ramo'],
    obrigatoria: true,
    tratamento: 'segmento',
  },
  {
    chave: 'nivel',
    rotulo: 'Nível',
    apelidos: ['nivel cliente', 'nivel', 'classificacao', 'curva'],
    obrigatoria: true,
    tratamento: 'nivelABC',
  },
  {
    chave: 'faturamento',
    rotulo: 'Faturamento anual',
    apelidos: ['faturamento anual', 'faturamento', 'receita', 'valor'],
    obrigatoria: true,
    tratamento: 'moeda',
  },
  {
    chave: 'servicos',
    rotulo: 'Serviços',
    apelidos: ['servicos contratados', 'servicos', 'produtos'],
    // Cliente sem nenhum serviço é contrato sem objeto: entraria no total de
    // faturamento sem aparecer em nenhuma contagem de serviço, e a soma das
    // partes nunca fecharia com o todo.
    obrigatoria: true,
    tratamento: 'listaServicos',
  },
  {
    chave: 'contratacao',
    rotulo: 'Contratação',
    apelidos: ['data contratacao', 'contratacao', 'data'],
    obrigatoria: true,
    tratamento: 'dataBR',
  },
  {
    chave: 'cidade',
    rotulo: 'Cidade',
    apelidos: ['cidade', 'municipio'],
    // Cidade e UF opcionais, como na aba dicionario_dados da planilha modelo.
    // O guia de aplicação do Pinia (aula 08) lista as duas como obrigatórias;
    // os documentos divergem, e prevaleceu o dicionário. Em branco, o registro
    // cai no grupo "(em branco)" do resumo. Se a regra mudar, é só esta flag:
    // o tratamento de UF não reclama de vazio por conta própria.
    obrigatoria: false,
    tratamento: 'nomeProprio',
  },
  {
    chave: 'uf',
    rotulo: 'UF',
    apelidos: ['uf', 'estado', 'sigla'],
    obrigatoria: false,
    tratamento: 'uf',
  },
  {
    chave: 'email',
    rotulo: 'E-mail',
    apelidos: ['email', 'e mail', 'email cliente', 'email contato', 'contato'],
    // A planilha modelo NÃO tem esta coluna — as 10 colunas da aba
    // upload_clientes vão de codigo_cliente a uf. O enunciado, porém, pede
    // validação de e-mail entre as mínimas. Registrar como opcional resolve a
    // divergência sem falsear o modelo: onde a coluna não existe, nada é
    // cobrado; no dia em que ela aparecer, o formato passa a ser validado sem
    // tocar em código. Marcar obrigatoria: true reprovaria as 36 linhas da
    // planilha de aula por um campo que ela não tem.
    //
    // Fica no fim do ESQUEMA para não abrir uma coluna vazia no meio da prévia,
    // que percorre esta lista na ordem.
    obrigatoria: false,
    tratamento: 'email',
  },
]

// ---------------------------------------------------------------------------
// Limpeza de texto
// ---------------------------------------------------------------------------

//   é o espaço não separável, que entra sozinho quando a planilha veio de
// uma cópia da web. Ele é invisível na tela e é a causa mais comum de "esse
// trim não funcionou".
function limpar(bruto) {
  if (bruto === null || bruto === undefined) return ''
  return String(bruto).replace(/[\s ]+/g, ' ').trim()
}

// ̀-ͯ é a faixa das marcas de acento que o NFD separa da letra.
function semAcento(texto) {
  return texto.normalize('NFD').replace(/[̀-ͯ]/g, '')
}

// Forma achatada: só para casar cabeçalho e consultar dicionário, nunca para
// exibir. Tira acento, caixa, pontuação e underscore — assim "codigo_cliente",
// "Código Cliente" e "CODIGO  CLIENTE" caem no mesmo lugar, e "IND." vira "ind".
export function achatar(bruto) {
  return semAcento(limpar(bruto))
    .toLowerCase()
    .replace(/[._-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

// Partículas que o português mantém em minúscula no meio do nome.
const PARTICULAS = new Set(['de', 'da', 'do', 'das', 'dos', 'e', 'em', 'a', 'o'])

// Corrige "ANA SOUZA" para "Ana Souza".
//
// A regra de preservar sigla curta só vale quando o valor tem caixa mista: em
// "Consultoria TI" o TI é sigla de verdade. Já quando o texto INTEIRO vem em
// caixa-alta, isso é digitação gritada, não sigla — e sem esta distinção
// "ANA SOUZA" continuaria "ANA Souza", virando um consultor separado de
// "Ana Souza" no agrupamento. Foi exatamente o que aconteceu antes de existir
// esta verificação.
function nomeProprio(texto) {
  const limpo = limpar(texto)
  if (!limpo) return ''

  const gritado = limpo === limpo.toUpperCase()

  return limpo
    .split(' ')
    .map((palavra, indice) => {
      if (!palavra) return palavra
      const achatada = achatar(palavra)
      if (indice > 0 && PARTICULAS.has(achatada)) return achatada
      if (
        !gritado &&
        palavra.length <= 3 &&
        palavra === palavra.toUpperCase() &&
        /^[A-ZÀ-Ý]+$/.test(palavra)
      ) {
        return palavra
      }
      return palavra.charAt(0).toUpperCase() + palavra.slice(1).toLowerCase()
    })
    .join(' ')
}

function consultarDicionario(dicionario, bruto) {
  const chave = achatar(bruto)
  if (!chave) return null
  const achado = dicionario.find((item) => item.sinonimos.includes(chave))
  return achado ? achado.canonico : null
}

// ---------------------------------------------------------------------------
// Tratamentos por coluna
//
// Cada um devolve { valor, erro?, aviso? }, onde erro e aviso são
// { tipo, mensagem }:
//   erro  → a linha inteira é rejeitada e vai para a lista de erros;
//   aviso → a linha passa, mas o assunto é reportado no relatório.
//
// O `tipo` vem de TIPOS_ERRO / TIPOS_AVISO e é o que permite contar por
// natureza. A mensagem vai direto para a tela: escreva para quem vai corrigir
// a planilha. Um mesmo tratamento produz tipos diferentes — moeda em branco é
// 'vazio', moeda com letra é 'formato' — e é por isso que o tipo é decidido
// aqui, no ponto que sabe o que aconteceu, e não depois por palpite no texto.
// ---------------------------------------------------------------------------

// Nenhum tratamento reclama de célula vazia por conta própria: quem decide se
// a coluna pode faltar é o `obrigatoria` do ESQUEMA, e ele já emite
// "<Rótulo> em branco" para toda coluna. Quando os dois emitiam, a mesma
// tabela misturava "Cliente em branco" com "segmento em branco" — a segunda
// escrita pelo tratamento, em minúscula e com o nome interno do campo. Deixar
// a origem única resolve a caixa e faz a flag valer para todas as colunas.
const erroFormato = (mensagem) => ({ tipo: 'formato', mensagem })

const TRATAMENTOS = {
  texto(bruto) {
    return { valor: limpar(bruto) }
  },

  codigo(bruto) {
    return { valor: limpar(bruto).toUpperCase() }
  },

  nomeProprio(bruto) {
    return { valor: nomeProprio(bruto) }
  },

  // Validação de formato, não de existência: só o envio de verdade prova que
  // uma caixa existe. A regra cobre o que estraga cadastro na prática — espaço
  // no meio, arroba faltando ou dobrada, domínio sem ponto, TLD de uma letra.
  // Deliberadamente mais simples que o RFC 5322: a expressão completa é
  // ilegível, e o que ela libera a mais (comentários, aspas, IP literal) não
  // aparece em planilha comercial.
  email(bruto) {
    const valor = limpar(bruto).toLowerCase()
    if (!valor) return { valor: '' }
    if (!/^[^\s@]+@[^\s@.]+(\.[^\s@.]+)*\.[a-z]{2,}$/.test(valor)) {
      return { valor, erro: { tipo: 'email', mensagem: `e-mail "${valor}" não é válido` } }
    }
    return { valor }
  },

  // Devolve a forma canônica COM acento e caixa correta ("Indústria"), porque
  // ela vai para a tela. O achatamento é usado só na consulta, então agrupar
  // continua certo sem precisar exibir tudo em caixa-alta.
  segmento(bruto) {
    const limpo = limpar(bruto)
    if (!limpo) return { valor: '' }

    const canonico = consultarDicionario(SEGMENTOS, limpo)
    if (canonico) return { valor: canonico }

    // Desconhecido não derruba a linha: o negócio pode ter criado um segmento
    // novo. Passa adiante em forma apresentável e o relatório cobra o cadastro.
    return {
      valor: nomeProprio(limpo),
      aviso: { tipo: 'dicionario', mensagem: `segmento "${limpo}" não está no dicionário` },
    }
  },

  nivelABC(bruto) {
    const valor = limpar(bruto).toUpperCase()
    if (!valor) return { valor: '' }
    if (!NIVEIS.includes(valor)) {
      return { valor, erro: erroFormato(`nível "${valor}" fora de A, B ou C`) }
    }
    return { valor }
  },

  // Aceita "R$ 1.850.000,00" e também 1850000 ou 1850000.00, porque a mesma
  // planilha costuma trazer os dois quando passou por mãos diferentes.
  moeda(bruto) {
    // Vindo do xlsx a célula já é número; só o CSV entrega texto para converter.
    if (typeof bruto === 'number') {
      if (!Number.isFinite(bruto)) return { valor: null, erro: erroFormato('faturamento inválido') }
      if (bruto < 0) return { valor: bruto, erro: erroFormato('faturamento negativo') }
      return { valor: bruto }
    }

    const texto = limpar(bruto)
    if (!texto) return { valor: null }

    const semMoeda = texto.replace(/R\$/gi, '').replace(/[\s ]/g, '')
    const brasileiro = /^-?\d{1,3}(\.\d{3})*(,\d+)?$/.test(semMoeda) || /^-?\d+,\d+$/.test(semMoeda)
    const normalizado = brasileiro ? semMoeda.replace(/\./g, '').replace(',', '.') : semMoeda

    const numero = Number(normalizado)
    if (!Number.isFinite(numero)) {
      return { valor: null, erro: erroFormato(`faturamento "${texto}" não é numérico`) }
    }
    if (numero < 0) return { valor: numero, erro: erroFormato('faturamento negativo') }
    return { valor: numero }
  },

  listaServicos(bruto) {
    const texto = limpar(bruto)
    if (!texto) return { valor: [] }

    const desconhecidos = []
    const itens = []

    for (const pedaco of texto.split(SEPARADOR_SERVICOS)) {
      const limpo = limpar(pedaco)
      if (!limpo) continue
      const canonico = consultarDicionario(SERVICOS, limpo)
      if (canonico) {
        if (!itens.includes(canonico)) itens.push(canonico)
      } else {
        const apresentavel = nomeProprio(limpo)
        if (!itens.includes(apresentavel)) itens.push(apresentavel)
        desconhecidos.push(limpo)
      }
    }

    if (desconhecidos.length) {
      return {
        valor: itens,
        aviso: {
          tipo: 'dicionario',
          mensagem: `serviço não catalogado: ${desconhecidos.join(', ')}`,
        },
      }
    }
    return { valor: itens }
  },

  // dd/mm/aaaa. new Date("15/01/2025") não serve: o motor interpreta como
  // mês 15 e devolve Invalid Date — ou, pior, com "03/02/2025" devolve
  // 3 de fevereiro em vez de 2 de março, silenciosamente.
  dataBR(bruto) {
    const texto = limpar(bruto)
    if (!texto) return { valor: null }

    // O SheetJS pode entregar a célula já como Date quando a planilha tem o
    // formato de data aplicado; nesse caso não há o que interpretar.
    if (bruto instanceof Date && !Number.isNaN(bruto.getTime())) return { valor: bruto }

    const partes = texto.match(/^(\d{1,2})[/-](\d{1,2})[/-](\d{4})$/)
    if (!partes) return { valor: null, erro: erroFormato(`data "${texto}" fora do formato dd/mm/aaaa`) }

    const [, dia, mes, ano] = partes.map(Number)
    const data = new Date(ano, mes - 1, dia)

    // Rejeita 31/02: o Date "corrige" para 03/03 sem avisar.
    if (data.getDate() !== dia || data.getMonth() !== mes - 1 || data.getFullYear() !== ano) {
      return { valor: null, erro: erroFormato(`data "${texto}" não existe no calendário`) }
    }
    return { valor: data }
  },

  // Em branco não é erro aqui: quem decide se a coluna pode faltar é o
  // `obrigatoria` do ESQUEMA, e deixar o tratamento reclamar por conta própria
  // tornaria a flag decorativa — foi o que mantinha UF obrigatória na prática
  // mesmo depois de o dicionário da planilha passar a listá-la como opcional.
  // Sigla preenchida, porém, continua tendo que ser uma das 27.
  uf(bruto) {
    const valor = semAcento(limpar(bruto)).toUpperCase()
    if (!valor) return { valor: '' }
    if (!UFS.includes(valor)) {
      return { valor, erro: erroFormato(`UF "${valor}" não é uma sigla válida`) }
    }
    return { valor }
  },
}

// ---------------------------------------------------------------------------
// Casamento do cabeçalho
// ---------------------------------------------------------------------------

export function casarCabecalho(cabecalho) {
  const achatado = cabecalho.map(achatar)
  const mapa = {}
  const usadas = new Set()

  for (const coluna of ESQUEMA) {
    const indice = achatado.findIndex((nome, i) => !usadas.has(i) && coluna.apelidos.includes(nome))
    if (indice !== -1) {
      mapa[coluna.chave] = indice
      usadas.add(indice)
    }
  }

  const faltando = ESQUEMA.filter((c) => c.obrigatoria && mapa[c.chave] === undefined).map(
    (c) => c.rotulo,
  )

  const ignoradas = cabecalho
    .map((nome, i) => ({ nome: limpar(nome), i }))
    .filter(({ nome, i }) => nome && !usadas.has(i))
    .map(({ nome }) => nome)

  return { mapa, faltando, ignoradas }
}

// ---------------------------------------------------------------------------
// Tratamento das linhas
// ---------------------------------------------------------------------------

function linhaVazia(celulas) {
  return celulas.every((celula) => limpar(celula) === '')
}

/**
 * Detecta o que o tratamento corrigiu em silêncio.
 *
 * Só faz sentido sobre célula de TEXTO: número e Date vêm tipados do SheetJS e
 * não passaram por limpeza nenhuma. A comparação é contra o bruto original, e
 * não contra o valor final, porque as duas coisas são independentes — uma
 * célula pode ter só espaço sobrando (" Campinas "), só caixa errada
 * ("ANA SOUZA") ou as duas (" industria ").
 *
 * Os dois avisos são emitidos juntos quando os dois ocorrem: são achados de
 * natureza diferente, e colapsá-los em um esconderia metade do que o enunciado
 * pede para identificar.
 */
function correcoesSilenciosas(bruto, valor, coluna) {
  if (typeof bruto !== 'string') return []

  const achados = []
  const aparado = limpar(bruto)

  // A frase põe o rótulo antes dos dois-pontos, e não como sujeito do
  // particípio, para não depender do gênero da coluna: "Cidade padronizado" e
  // "Serviços padronizado" saíam errados, e concordar de verdade exigiria
  // declarar o gênero de cada rótulo no ESQUEMA por causa de uma frase.
  if (aparado !== bruto) {
    achados.push({
      tipo: 'espacos',
      mensagem: `${coluna.rotulo}: espaço sobrando removido de "${bruto}"`,
    })
  }

  // Serviços devolve array; a padronização dele já é reportada como
  // 'dicionario' quando o nome não está no catálogo.
  if (typeof valor === 'string' && aparado && valor !== aparado) {
    achados.push({
      tipo: 'padronizacao',
      mensagem: `${coluna.rotulo}: "${aparado}" padronizado para "${valor}"`,
    })
  }

  return achados
}

/**
 * Recebe a matriz crua da planilha (primeira linha = cabeçalho) e devolve
 * { dados, erros, avisos, colunasIgnoradas }.
 *
 * Uma linha com problema NÃO derruba o arquivo: ela vai para `erros` com o
 * número da linha como aparece no Excel, e as demais seguem para `dados`.
 *
 * Cada item de `erros` traz `problemas`, a lista tipada do que deu errado
 * naquela linha — é dela que sai a contagem por tipo e o campo culpado. O
 * `motivo` continua existindo como a mesma lista em frase única, porque é o que
 * a tabela de erros mostra numa célula só.
 */
export function tratarLinhas(matriz) {
  // Problema de ESTRUTURA não pertence a nenhuma coluna: vale para o arquivo
  // inteiro. Entra com coluna '—' para que a tabela de ocorrências não precise
  // de um caso especial.
  const erroEstrutura = (linha, mensagem) => ({
    linha,
    codigo: '—',
    nome: '(sem nome)',
    motivo: mensagem,
    problemas: [{ tipo: 'estrutura', coluna: '—', mensagem }],
  })

  if (!matriz.length) {
    return {
      dados: [],
      erros: [erroEstrutura(0, 'A planilha está vazia')],
      avisos: [],
      colunasIgnoradas: [],
    }
  }

  const [cabecalho, ...corpo] = matriz
  const { mapa, faltando, ignoradas } = casarCabecalho(cabecalho)

  if (faltando.length) {
    return {
      dados: [],
      erros: [
        erroEstrutura(
          1,
          `Cabeçalho sem a(s) coluna(s) obrigatória(s): ${faltando.join(', ')}`,
        ),
      ],
      avisos: [],
      colunasIgnoradas: ignoradas,
    }
  }

  const dados = []
  const erros = []
  const avisos = []
  const vistos = new Map()

  corpo.forEach((celulas, indice) => {
    const linha = indice + 2 // +1 pelo cabeçalho, +1 porque o Excel conta de 1
    if (linhaVazia(celulas)) return

    // O registro só recebe chaves vindas do ESQUEMA, nunca nomes do arquivo.
    // Isso fecha a porta para a poluição de protótipo do SheetJS: um cabeçalho
    // chamado "__proto__" nunca chega a virar chave de objeto aqui.
    const registro = {}
    const problemas = []

    for (const coluna of ESQUEMA) {
      const posicao = mapa[coluna.chave]
      const bruto = posicao === undefined ? '' : celulas[posicao]
      const { valor, erro, aviso } = TRATAMENTOS[coluna.tratamento](bruto)

      registro[coluna.chave] = valor

      // O problema guarda a coluna que o produziu. Antes o nome do campo só
      // existia dentro da frase, o que dava para ler e não para filtrar nem
      // agrupar — e o relatório pede o campo como informação própria.
      if (erro) problemas.push({ tipo: erro.tipo, coluna: coluna.rotulo, mensagem: erro.mensagem })
      if (aviso) {
        avisos.push({ linha, coluna: coluna.rotulo, tipo: aviso.tipo, motivo: aviso.mensagem })
      }

      // Espaço sobrando e caixa padronizada só são reportados quando a linha
      // não tem erro naquela coluna: apontar "espaço sobrando em Consultor" numa
      // célula que está simplesmente vazia é ruído em cima do problema real.
      if (!erro) {
        for (const achado of correcoesSilenciosas(bruto, valor, coluna)) {
          avisos.push({
            linha,
            coluna: coluna.rotulo,
            tipo: achado.tipo,
            motivo: achado.mensagem,
          })
        }
      }

      // O `obrigatoria` do esquema precisa valer para TODA coluna. Tratamento de
      // texto nunca devolve erro por conta própria, então sem esta verificação
      // um consultor em branco passaria batido — a coluna estaria marcada como
      // obrigatória só no papel.
      const vazio = valor === '' || valor === null || (Array.isArray(valor) && !valor.length)
      if (!erro && coluna.obrigatoria && vazio) {
        problemas.push({
          tipo: 'vazio',
          coluna: coluna.rotulo,
          mensagem: `${coluna.rotulo} em branco`,
        })
      }
    }

    // Código repetido é erro de cadastro, não de digitação: duas linhas com o
    // mesmo CTI vão virar dois registros do mesmo cliente lá na frente.
    if (registro.codigo) {
      const anterior = vistos.get(registro.codigo)
      if (anterior) {
        problemas.push({
          tipo: 'duplicado',
          coluna: 'Código',
          mensagem: `código ${registro.codigo} repetido (já apareceu na linha ${anterior})`,
        })
      } else {
        vistos.set(registro.codigo, linha)
      }
    }

    if (problemas.length) {
      erros.push({
        linha,
        codigo: registro.codigo || '—',
        nome: registro.nome || '(sem nome)',
        problemas,
        motivo: problemas.map((p) => p.mensagem).join('; '),
      })
      return
    }

    registro.linha = linha
    dados.push(registro)
  })

  return { dados, erros, avisos, colunasIgnoradas: ignoradas }
}

// ---------------------------------------------------------------------------
// Resumo
// ---------------------------------------------------------------------------

function agrupar(dados, obterChave) {
  const grupos = new Map()
  for (const registro of dados) {
    const chave = obterChave(registro) || '(em branco)'
    const atual = grupos.get(chave) ?? { chave, linhas: 0, faturamento: 0 }
    atual.linhas += 1
    atual.faturamento += registro.faturamento ?? 0
    grupos.set(chave, atual)
  }
  return [...grupos.values()]
}

export function resumir(dados, erros, avisos = [], colunasIgnoradas = []) {
  // Somar float acumula resíduo (0,1 + 0,2 não dá 0,3), e um total sairia como
  // 59215.310000000005. Sem efeito na tela, que formata, mas envenena qualquer
  // comparação de soma feita depois.
  const emCentavos = (n) => Math.round(n * 100) / 100
  const ordenar = (lista) =>
    lista
      .map((g) => ({ ...g, faturamento: emCentavos(g.faturamento) }))
      .sort((a, b) => b.faturamento - a.faturamento || b.linhas - a.linhas)

  const porNivel = { A: 0, B: 0, C: 0 }
  const servicos = new Map()
  let faturamentoTotal = 0
  let inicio = null
  let fim = null

  for (const registro of dados) {
    if (porNivel[registro.nivel] !== undefined) porNivel[registro.nivel] += 1
    faturamentoTotal += registro.faturamento ?? 0

    for (const servico of registro.servicos ?? []) {
      servicos.set(servico, (servicos.get(servico) ?? 0) + 1)
    }

    const data = registro.contratacao
    if (data instanceof Date) {
      if (!inicio || data < inicio) inicio = data
      if (!fim || data > fim) fim = data
    }
  }

  return {
    linhas: dados.length + erros.length,
    validas: dados.length,
    comErro: erros.length,
    porNivel,
    porSegmento: ordenar(agrupar(dados, (r) => r.segmento)),
    porConsultor: ordenar(agrupar(dados, (r) => r.consultor)),
    porCidade: ordenar(agrupar(dados, (r) => r.cidade)),
    porServico: [...servicos.entries()]
      .map(([servico, clientes]) => ({ servico, clientes }))
      .sort((a, b) => b.clientes - a.clientes),
    faturamentoTotal: emCentavos(faturamentoTotal),
    periodo: inicio && fim ? { inicio, fim } : null,
    avisos,
    colunasIgnoradas,
  }
}
