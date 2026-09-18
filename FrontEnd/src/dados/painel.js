// Dados de exemplo do painel.
//
// Tudo aqui é sintético e existe só para o front-end ter forma enquanto a API
// não chega. Quando a integração vier, troque `gerarPainel` por uma chamada ao
// backend: o resto das telas consome o formato devolvido por esta função e não
// precisa mudar.
//
// O sorteio é determinístico (semente fixa) de propósito — com Math.random os
// gráficos mudariam a cada re-render e a cada troca de período, o que faz um
// painel parecer quebrado.

function semente(valor) {
  return function sortear() {
    valor |= 0
    valor = (valor + 0x6d2b79f5) | 0
    let t = Math.imul(valor ^ (valor >>> 15), 1 | valor)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

const DIA = 86400000

function diasAtras(quantidade) {
  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)
  return new Date(hoje.getTime() - quantidade * DIA)
}

// Volume diário com tendência de alta suave, queda no fim de semana e um pico
// isolado — o bastante para o gráfico ter o que contar.
function serieVolume(dias) {
  const sortear = semente(20260910)
  const pontos = []
  for (let i = dias - 1; i >= 0; i -= 1) {
    const data = diasAtras(i)
    const semana = data.getDay()
    const fimDeSemana = semana === 0 || semana === 6
    const tendencia = 1 + (dias - i) / (dias * 2.2)
    const base = 1450 * tendencia * (fimDeSemana ? 0.34 : 1)
    const ruido = 0.82 + sortear() * 0.36
    const pico = i === Math.floor(dias * 0.35) ? 1.62 : 1
    pontos.push({ data, registros: Math.round(base * ruido * pico) })
  }
  return pontos
}

function somar(lista, campo) {
  return lista.reduce((total, item) => total + item[campo], 0)
}

// Reduz a série diária a 12 pontos para a faísca do cartão, preservando o
// formato da curva em vez de cortar os últimos 12 dias.
function faisca(pontos) {
  const alvo = 12
  if (pontos.length <= alvo) return pontos.map((ponto) => ponto.registros)
  const passo = pontos.length / alvo
  return Array.from({ length: alvo }, (_, i) => {
    const inicio = Math.floor(i * passo)
    const fim = Math.max(inicio + 1, Math.floor((i + 1) * passo))
    const fatia = pontos.slice(inicio, fim)
    return Math.round(somar(fatia, 'registros') / fatia.length)
  })
}

function formatarInteiro(valor) {
  return valor.toLocaleString('pt-BR')
}

// Compacta valores grandes para caber no cartão: 12.482 vira "12,5 mil".
export function compactar(valor) {
  if (valor >= 1000000) return `${(valor / 1000000).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} mi`
  if (valor >= 10000) return `${(valor / 1000).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} mil`
  return formatarInteiro(valor)
}

export function formatarData(data, formato = 'curto') {
  if (formato === 'curto') {
    return data.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })
  }
  // Série mensal precisa de "jan/25": mostrar 01/01 num eixo de meses se lê
  // como primeiro de janeiro e sugere granularidade diária que não existe.
  if (formato === 'mes') {
    // "jan/25", e não "jan de 25": a forma longa mede quase o dobro e os dois
    // últimos rótulos do eixo acabavam se sobrepondo.
    const mes = data.toLocaleDateString('pt-BR', { month: 'short' }).replace('.', '')
    return `${mes}/${String(data.getFullYear()).slice(2)}`
  }
  return data.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' })
}

export const PERIODOS = [
  { dias: 7, rotulo: '7 dias' },
  { dias: 30, rotulo: '30 dias' },
  { dias: 90, rotulo: '90 dias' },
]

export function gerarPainel(dias = 30) {
  const volume = serieVolume(dias)
  const anterior = serieVolume(dias * 2).slice(0, dias)

  const registros = somar(volume, 'registros')
  const registrosAnteriores = somar(anterior, 'registros')

  const sortear = semente(dias * 977)
  const arquivos = Math.max(6, Math.round(dias * 2.4 + sortear() * dias))
  const arquivosAnteriores = Math.round(arquivos * (0.78 + sortear() * 0.3))

  // Qualidade dos registros. A ordem importa: é a mesma da pilha no gráfico e
  // da legenda, e foi validada nessa sequência (verde → âmbar → coral).
  const erros = Math.round(registros * (0.019 + sortear() * 0.011))
  const alertas = Math.round(registros * (0.052 + sortear() * 0.028))
  const validos = registros - alertas - erros

  const conformidade = (validos / registros) * 100
  const conformidadeAnterior = conformidade - (0.6 + sortear() * 1.9)

  const relatorios = Math.max(2, Math.round(arquivos * 0.42))
  const relatoriosAnteriores = Math.round(relatorios * (0.7 + sortear() * 0.45))

  return {
    dias,

    indicadores: [
      {
        chave: 'arquivos',
        rotulo: 'Arquivos processados',
        valor: compactar(arquivos),
        bruto: arquivos,
        delta: variacao(arquivos, arquivosAnteriores),
        subirEhBom: true,
        faisca: faisca(volume),
      },
      {
        chave: 'registros',
        rotulo: 'Registros analisados',
        valor: compactar(registros),
        bruto: registros,
        delta: variacao(registros, registrosAnteriores),
        subirEhBom: true,
        faisca: faisca(volume),
      },
      {
        chave: 'conformidade',
        rotulo: 'Taxa de conformidade',
        valor: `${conformidade.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%`,
        bruto: conformidade,
        delta: variacao(conformidade, conformidadeAnterior),
        subirEhBom: true,
        faisca: faisca(volume).map((v, i, lista) => Math.round(v * (0.97 + (i / lista.length) * 0.06))),
      },
      {
        chave: 'relatorios',
        rotulo: 'Relatórios gerados',
        valor: compactar(relatorios),
        bruto: relatorios,
        delta: variacao(relatorios, relatoriosAnteriores),
        subirEhBom: true,
        faisca: faisca(volume).map((v) => Math.round(v * 0.42)),
      },
    ],

    volume,

    // Categorias nominais: uma cor só para todas as barras. Colorir cada tipo de
    // um jeito gastaria o canal de identidade para repetir o que o comprimento
    // da barra já diz.
    tipos: [
      { rotulo: '.xlsx', arquivos: Math.round(arquivos * 0.58) },
      { rotulo: '.csv', arquivos: Math.round(arquivos * 0.29) },
      { rotulo: '.xls', arquivos: Math.round(arquivos * 0.13) },
    ],

    qualidade: [
      { chave: 'validos', rotulo: 'Válidos', registros: validos },
      { chave: 'alertas', rotulo: 'Com alerta', registros: alertas },
      { chave: 'erros', rotulo: 'Com erro', registros: erros },
    ],

    envios: enviosRecentes(dias),
  }
}

function variacao(atual, anterior) {
  if (!anterior) return null
  return ((atual - anterior) / anterior) * 100
}

const NOMES = [
  'faturamento-consolidado.xlsx',
  'base-clientes-agosto.csv',
  'estoque-filiais.xlsx',
  'metas-comerciais.xls',
  'inadimplencia-q3.csv',
  'cadastro-fornecedores.xlsx',
]

function enviosRecentes(dias) {
  const sortear = semente(dias * 31 + 7)
  return NOMES.map((nome, i) => {
    const registros = Math.round(400 + sortear() * 5200)
    const proporcao = sortear()
    const estado = proporcao > 0.82 ? 'erro' : proporcao > 0.55 ? 'alerta' : 'ok'
    return {
      nome,
      registros,
      data: diasAtras(i),
      estado,
      inconsistencias: estado === 'ok' ? 0 : Math.round(registros * (estado === 'erro' ? 0.07 : 0.02)),
    }
  })
}

export const ESTADOS = {
  ok: { rotulo: 'Processado', descricao: 'Sem inconsistências' },
  alerta: { rotulo: 'Com alerta', descricao: 'Inconsistências não bloqueantes' },
  erro: { rotulo: 'Com erro', descricao: 'Registros rejeitados' },
}
