// Esquema da planilha de clientes e regras de tratamento.
//
// Cabeçalho de planilha nunca vem padronizado: `apelidos` lista as variações, e
// a comparação usa a forma achatada (sem acento, caixa ou pontuação).

export const NIVEIS = ['A', 'B', 'C']

export const UFS = [
  'AC', 'AL', 'AM', 'AP', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MG', 'MS', 'MT',
  'PA', 'PB', 'PE', 'PI', 'PR', 'RJ', 'RN', 'RO', 'RR', 'RS', 'SC', 'SE', 'SP', 'TO',
]

// Dicionário de segmentos. Achatar resolve "Indústria" x "industria", mas não
// "IND." — sem sinônimo declarado ela viraria um segmento próprio e o
// agrupamento mentiria. Declare cada abreviação nova aqui.
export const SEGMENTOS = [
  { canonico: 'Indústria', sinonimos: ['industria', 'ind', 'industrial', 'fabrica'] },
  { canonico: 'Comércio', sinonimos: ['comercio', 'com', 'comercial', 'varejo'] },
  { canonico: 'Serviços', sinonimos: ['servicos', 'servico', 'serv'] },
  { canonico: 'Saúde', sinonimos: ['saude', 'hospitalar'] },
  { canonico: 'Tecnologia', sinonimos: ['tecnologia', 'tec', 'ti'] },
  { canonico: 'Educação', sinonimos: ['educacao', 'ensino', 'educacional'] },
]

// Célula multivalorada. Aceita vírgula e barra além do ponto e vírgula do
// dicionário; sem isso "Internet Dedicada, Firewall" virava um serviço só,
// inexistente. Nenhum nome do catálogo contém esses caracteres — se passar a
// conter, mude o separador junto. " e " fica de fora: partiria nome que a use.
export const SEPARADOR_SERVICOS = /[;,/]/

export const SERVICOS = [
  { canonico: 'Internet Dedicada', sinonimos: ['internet dedicada', 'internet'] },
  { canonico: 'Link Empresarial', sinonimos: ['link empresarial', 'link'] },
  { canonico: 'MPLS', sinonimos: ['mpls'] },
  { canonico: 'Firewall', sinonimos: ['firewall'] },
  { canonico: 'Backup', sinonimos: ['backup'] },
  { canonico: 'Cloud', sinonimos: ['cloud', 'nuvem'] },
]

// Catálogo de tipos de problema. O problema nasce com um tipo para o relatório
// poder contar por natureza, e o rótulo mora aqui para a tela não manter um
// segundo dicionário que envelhece em separado.

// Erro derruba a linha: ela não entra em dadosValidos.
export const TIPOS_ERRO = [
  { tipo: 'vazio', rotulo: 'Campos obrigatórios vazios' },
  { tipo: 'email', rotulo: 'E-mails inválidos' },
  { tipo: 'duplicado', rotulo: 'Registros duplicados' },
  { tipo: 'formato', rotulo: 'Dados fora do padrão' },
  { tipo: 'estrutura', rotulo: 'Problemas na estrutura do arquivo' },
]

// Aviso não derruba: a linha passa e o assunto é reportado. Os dois primeiros
// são correções já aplicadas, registradas para a pessoa poder corrigir a
// planilha na origem.
//
// "Espaços extras", e não "espaços em branco": em português a segunda forma se
// lê como célula vazia, e o contador marcando 0 numa planilha com três células
// vazias parecia falha da checagem. Célula vazia é contada em 'vazio'.
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
    // Cliente sem serviço entraria no faturamento total sem aparecer em
    // contagem nenhuma, e a soma das partes não fecharia com o todo.
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
    // Opcionais pela aba dicionario_dados da planilha. O guia do Pinia lista as
    // duas como obrigatórias; os documentos divergem e prevaleceu o dicionário.
    // Se a regra mudar, é só esta flag.
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
    // A planilha modelo não tem esta coluna, mas o enunciado pede validação de
    // e-mail. Opcional resolve a divergência: obrigatória reprovaria as 39
    // linhas por um campo que o arquivo não tem. No fim da lista para não abrir
    // coluna vazia no meio da prévia, que percorre o ESQUEMA na ordem.
    obrigatoria: false,
    tratamento: 'email',
  },
]

// ---------------------------------------------------------------------------
// Limpeza de texto
// ---------------------------------------------------------------------------

// Inclui o espaço não separável, invisível na tela e causa mais comum de
// "esse trim não funcionou". Entra sozinho quando a planilha veio da web.
function limpar(bruto) {
  if (bruto === null || bruto === undefined) return ''
  return String(bruto).replace(/[\s ]+/g, ' ').trim()
}

// ̀-ͯ é a faixa das marcas de acento que o NFD separa da letra.
function semAcento(texto) {
  return texto.normalize('NFD').replace(/[̀-ͯ]/g, '')
}

// Só para casar cabeçalho e consultar dicionário, nunca para exibir. Faz
// "codigo_cliente", "Código Cliente" e "CODIGO  CLIENTE" caírem no mesmo lugar.
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
// Preservar sigla curta só vale em texto de caixa mista — em "Consultoria TI" o
// TI é sigla de verdade. Texto inteiro em caixa-alta é digitação gritada, e sem
// essa distinção "ANA SOUZA" virava "ANA Souza", um consultor separado de
// "Ana Souza" no agrupamento.
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

// Primeira letra em maiúscula. As mensagens vêm de três origens e cada uma
// nascia como o autor escreveu: "Consultor em branco" ao lado de "código CTI035
// repetido" na mesma coluna. Normalizar na CRIAÇÃO, e não na exibição, vale
// também para mensagem que ainda não existe. Locale pt-BR por causa do acento.
function maiuscula(texto) {
  if (!texto) return texto
  return texto.charAt(0).toLocaleUpperCase('pt-BR') + texto.slice(1)
}

// Ponto único da normalização: antes cada `push` montava o objeto à mão, e
// bastava um novo esquecer a regra para a inconsistência voltar.
const problemaDe = (tipo, coluna, mensagem) => ({ tipo, coluna, mensagem: maiuscula(mensagem) })

const avisoDe = (linha, coluna, tipo, mensagem) => ({
  linha,
  coluna,
  tipo,
  motivo: maiuscula(mensagem),
})

// ---------------------------------------------------------------------------
// Tratamentos por coluna
//
// Devolvem { valor, erro?, aviso? }, com erro e aviso em { tipo, mensagem }:
// erro rejeita a linha, aviso só reporta. O tipo é decidido aqui, onde se sabe
// o que aconteceu — um mesmo tratamento produz tipos diferentes. A mensagem vai
// direto para a tela: escreva para quem vai corrigir a planilha.
// ---------------------------------------------------------------------------

// Nenhum tratamento reclama de célula vazia: quem decide é o `obrigatoria` do
// ESQUEMA, que já emite "<Rótulo> em branco". Com as duas origens, a mesma
// tabela misturava "Cliente em branco" e "segmento em branco".
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

  // Formato, não existência — só o envio prova que a caixa existe. Cobre o que
  // estraga cadastro: espaço no meio, arroba faltando ou dobrada, domínio sem
  // ponto. Mais simples que o RFC 5322 de propósito: o que ele libera a mais
  // não aparece em planilha comercial.
  email(bruto) {
    const valor = limpar(bruto).toLowerCase()
    if (!valor) return { valor: '' }
    if (!/^[^\s@]+@[^\s@.]+(\.[^\s@.]+)*\.[a-z]{2,}$/.test(valor)) {
      return { valor, erro: { tipo: 'email', mensagem: `e-mail "${valor}" não é válido` } }
    }
    return { valor }
  },

  // Devolve a forma canônica com acento e caixa ("Indústria") porque vai para a
  // tela; o achatamento serve só à consulta.
  segmento(bruto) {
    const limpo = limpar(bruto)
    if (!limpo) return { valor: '' }

    const canonico = consultarDicionario(SEGMENTOS, limpo)
    if (canonico) return { valor: canonico }

    // Desconhecido não derruba a linha: pode ser segmento novo. Passa adiante e
    // o relatório cobra o cadastro.
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

  // Aceita "R$ 1.850.000,00", 1850000 e 1850000.00: a mesma planilha traz os
  // três quando passou por mãos diferentes.
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

  // dd/mm/aaaa à mão porque new Date("15/01/2025") lê mês 15 e, pior, com
  // "03/02/2025" devolve 3 de fevereiro em vez de 2 de março, sem avisar.
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

  // Vazio não é erro aqui (ver `obrigatoria`), mas sigla preenchida tem de ser
  // uma das 27.
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
 * Só sobre célula de TEXTO: número e Date vêm tipados do SheetJS, sem limpeza.
 * Espaço sobrando e caixa errada são independentes — " industria " tem os dois —
 * e por isso os dois avisos podem sair juntos.
 */
function correcoesSilenciosas(bruto, valor, coluna) {
  if (typeof bruto !== 'string') return []

  const achados = []
  const aparado = limpar(bruto)

  // Rótulo antes dos dois-pontos para não depender do gênero da coluna:
  // "Cidade padronizado" saía errado, e concordar exigiria declarar o gênero de
  // cada rótulo no ESQUEMA.
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
 * Recebe a matriz crua (primeira linha = cabeçalho) e devolve
 * { dados, erros, avisos, colunasIgnoradas }.
 *
 * Linha com problema não derruba o arquivo: vai para `erros` com o número da
 * linha do Excel. Cada item traz `problemas` tipados — de onde saem a contagem
 * por tipo e o campo culpado — e `motivo`, a mesma lista em frase única.
 */
export function tratarLinhas(matriz) {
  // Estrutura vale para o arquivo inteiro, não para uma coluna: entra com '—'
  // para a tabela de ocorrências não precisar de caso especial.
  const erroEstrutura = (linha, mensagem) => {
    const problemas = [problemaDe('estrutura', '—', mensagem)]
    return {
      linha,
      codigo: '—',
      nome: '(sem nome)',
      problemas,
      motivo: problemas[0].mensagem,
    }
  }

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

    // Só chaves do ESQUEMA, nunca nomes do arquivo: fecha a porta para a
    // poluição de protótipo — "__proto__" no cabeçalho nunca vira chave aqui.
    const registro = {}
    const problemas = []

    for (const coluna of ESQUEMA) {
      const posicao = mapa[coluna.chave]
      const bruto = posicao === undefined ? '' : celulas[posicao]
      const { valor, erro, aviso } = TRATAMENTOS[coluna.tratamento](bruto)

      registro[coluna.chave] = valor

      // O problema guarda a coluna que o produziu: dentro da frase, o campo dava
      // para ler e não para agrupar, e o relatório o pede como informação própria.
      if (erro) problemas.push(problemaDe(erro.tipo, coluna.rotulo, erro.mensagem))
      if (aviso) avisos.push(avisoDe(linha, coluna.rotulo, aviso.tipo, aviso.mensagem))

      // Só reporta correção onde a coluna não tem erro: apontar espaço sobrando
      // numa célula vazia é ruído em cima do problema real.
      if (!erro) {
        for (const achado of correcoesSilenciosas(bruto, valor, coluna)) {
          avisos.push(avisoDe(linha, coluna.rotulo, achado.tipo, achado.mensagem))
        }
      }

      // Tratamento de texto nunca devolve erro por conta própria: sem esta
      // verificação um consultor em branco passaria, e a flag seria decorativa.
      const vazio = valor === '' || valor === null || (Array.isArray(valor) && !valor.length)
      if (!erro && coluna.obrigatoria && vazio) {
        problemas.push(problemaDe('vazio', coluna.rotulo, `${coluna.rotulo} em branco`))
      }
    }

    // Código repetido é erro de cadastro: duas linhas com o mesmo CTI viram dois
    // registros do mesmo cliente lá na frente.
    if (registro.codigo) {
      const anterior = vistos.get(registro.codigo)
      if (anterior) {
        problemas.push(
          problemaDe(
            'duplicado',
            'Código',
            `código ${registro.codigo} repetido (já apareceu na linha ${anterior})`,
          ),
        )
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
  // Somar float acumula resíduo: o total sairia 59215.310000000005. A tela
  // formata e esconde, mas qualquer comparação de soma feita depois quebra.
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
