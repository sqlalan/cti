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

// Serviços contratados. A célula é multivalorada, separada por ponto e vírgula.
export const SERVICOS = [
  { canonico: 'Internet Dedicada', sinonimos: ['internet dedicada', 'internet'] },
  { canonico: 'Link Empresarial', sinonimos: ['link empresarial', 'link'] },
  { canonico: 'MPLS', sinonimos: ['mpls'] },
  { canonico: 'Firewall', sinonimos: ['firewall'] },
  { canonico: 'Backup', sinonimos: ['backup'] },
  { canonico: 'Cloud', sinonimos: ['cloud', 'nuvem'] },
]

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
    obrigatoria: false,
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
    obrigatoria: true,
    tratamento: 'nomeProprio',
  },
  {
    chave: 'uf',
    rotulo: 'UF',
    apelidos: ['uf', 'estado', 'sigla'],
    obrigatoria: true,
    tratamento: 'uf',
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
// Cada um devolve { valor, erro?, aviso? }.
//   erro  → a linha inteira é rejeitada e vai para a lista de erros;
//   aviso → a linha passa, mas o assunto é reportado no resumo.
// As frases vão direto para a tela: escreva para quem vai corrigir a planilha.
// ---------------------------------------------------------------------------

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

  // Devolve a forma canônica COM acento e caixa correta ("Indústria"), porque
  // ela vai para a tela. O achatamento é usado só na consulta, então agrupar
  // continua certo sem precisar exibir tudo em caixa-alta.
  segmento(bruto) {
    const limpo = limpar(bruto)
    if (!limpo) return { valor: '', erro: 'segmento em branco' }

    const canonico = consultarDicionario(SEGMENTOS, limpo)
    if (canonico) return { valor: canonico }

    // Desconhecido não derruba a linha: o negócio pode ter criado um segmento
    // novo. Passa adiante em forma apresentável e o resumo cobra o cadastro.
    return { valor: nomeProprio(limpo), aviso: `segmento "${limpo}" não está no dicionário` }
  },

  nivelABC(bruto) {
    const valor = limpar(bruto).toUpperCase()
    if (!valor) return { valor: '', erro: 'nível em branco' }
    if (!NIVEIS.includes(valor)) return { valor, erro: `nível "${valor}" fora de A, B ou C` }
    return { valor }
  },

  // Aceita "R$ 1.850.000,00" e também 1850000 ou 1850000.00, porque a mesma
  // planilha costuma trazer os dois quando passou por mãos diferentes.
  moeda(bruto) {
    // Vindo do xlsx a célula já é número; só o CSV entrega texto para converter.
    if (typeof bruto === 'number') {
      if (!Number.isFinite(bruto)) return { valor: null, erro: 'faturamento inválido' }
      if (bruto < 0) return { valor: bruto, erro: 'faturamento negativo' }
      return { valor: bruto }
    }

    const texto = limpar(bruto)
    if (!texto) return { valor: null, erro: 'faturamento em branco' }

    const semMoeda = texto.replace(/R\$/gi, '').replace(/[\s ]/g, '')
    const brasileiro = /^-?\d{1,3}(\.\d{3})*(,\d+)?$/.test(semMoeda) || /^-?\d+,\d+$/.test(semMoeda)
    const normalizado = brasileiro ? semMoeda.replace(/\./g, '').replace(',', '.') : semMoeda

    const numero = Number(normalizado)
    if (!Number.isFinite(numero)) return { valor: null, erro: `faturamento "${texto}" não é numérico` }
    if (numero < 0) return { valor: numero, erro: 'faturamento negativo' }
    return { valor: numero }
  },

  listaServicos(bruto) {
    const texto = limpar(bruto)
    if (!texto) return { valor: [] }

    const desconhecidos = []
    const itens = []

    for (const pedaco of texto.split(';')) {
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
      return { valor: itens, aviso: `serviço não catalogado: ${desconhecidos.join(', ')}` }
    }
    return { valor: itens }
  },

  // dd/mm/aaaa. new Date("15/01/2025") não serve: o motor interpreta como
  // mês 15 e devolve Invalid Date — ou, pior, com "03/02/2025" devolve
  // 3 de fevereiro em vez de 2 de março, silenciosamente.
  dataBR(bruto) {
    const texto = limpar(bruto)
    if (!texto) return { valor: null, erro: 'data de contratação em branco' }

    // O SheetJS pode entregar a célula já como Date quando a planilha tem o
    // formato de data aplicado; nesse caso não há o que interpretar.
    if (bruto instanceof Date && !Number.isNaN(bruto.getTime())) return { valor: bruto }

    const partes = texto.match(/^(\d{1,2})[/-](\d{1,2})[/-](\d{4})$/)
    if (!partes) return { valor: null, erro: `data "${texto}" fora do formato dd/mm/aaaa` }

    const [, dia, mes, ano] = partes.map(Number)
    const data = new Date(ano, mes - 1, dia)

    // Rejeita 31/02: o Date "corrige" para 03/03 sem avisar.
    if (data.getDate() !== dia || data.getMonth() !== mes - 1 || data.getFullYear() !== ano) {
      return { valor: null, erro: `data "${texto}" não existe no calendário` }
    }
    return { valor: data }
  },

  uf(bruto) {
    const valor = semAcento(limpar(bruto)).toUpperCase()
    if (!valor) return { valor: '', erro: 'UF em branco' }
    if (!UFS.includes(valor)) return { valor, erro: `UF "${valor}" não é uma sigla válida` }
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
 * Recebe a matriz crua da planilha (primeira linha = cabeçalho) e devolve
 * { dados, erros, avisos, colunasIgnoradas }.
 *
 * Uma linha com problema NÃO derruba o arquivo: ela vai para `erros` com o
 * número da linha como aparece no Excel, e as demais seguem para `dados`.
 */
export function tratarLinhas(matriz) {
  if (!matriz.length) {
    return {
      dados: [],
      erros: [{ linha: 0, codigo: '—', motivo: 'A planilha está vazia' }],
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
        {
          linha: 1,
          codigo: '—',
          motivo: `Cabeçalho sem a(s) coluna(s) obrigatória(s): ${faltando.join(', ')}`,
        },
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

      if (erro) problemas.push(erro)
      if (aviso) avisos.push({ linha, coluna: coluna.rotulo, motivo: aviso })

      // O `obrigatoria` do esquema precisa valer para TODA coluna. Tratamento de
      // texto nunca devolve erro por conta própria, então sem esta verificação
      // um consultor em branco passaria batido — a coluna estaria marcada como
      // obrigatória só no papel.
      const vazio = valor === '' || valor === null || (Array.isArray(valor) && !valor.length)
      if (!erro && coluna.obrigatoria && vazio) {
        problemas.push(`${coluna.rotulo} em branco`)
      }
    }

    // Código repetido é erro de cadastro, não de digitação: duas linhas com o
    // mesmo CTI vão virar dois registros do mesmo cliente lá na frente.
    if (registro.codigo) {
      const anterior = vistos.get(registro.codigo)
      if (anterior) {
        problemas.push(`código ${registro.codigo} repetido (já apareceu na linha ${anterior})`)
      } else {
        vistos.set(registro.codigo, linha)
      }
    }

    if (problemas.length) {
      erros.push({
        linha,
        codigo: registro.codigo || '—',
        nome: registro.nome || '(sem nome)',
        motivo: problemas.join('; '),
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
