<script setup>
import { computed, onMounted, ref } from 'vue'
import CartaoGrafico from './graficos/CartaoGrafico.vue'
import GraficoArea from './graficos/GraficoArea.vue'
import { formatarNumero } from './graficos/escala'
import { TIPOS_AVISO, TIPOS_ERRO, rotularTipo } from '../dados/esquemaPlanilha'
import { baixarCsv, baixarXlsx } from '../dados/exportar'
import { api, mensagemDeErro } from '../services/api'
import { useUploadStore } from '../stores/uploadStore'

// Tudo o que o servidor guardou de cada planilha processada: o banco mantém
// os indicadores e os problemas de TODOS os envios, então dá para abrir um
// envio antigo e comparar um envio com o anterior.

const LIMITE_EVOLUCAO = 12

// carregando → pronto | vazio (nenhuma planilha processada) | erro
const estado = ref('carregando')
const erro = ref('')
const historico = ref([]) // GET /api/analises, do mais recente para o mais antigo

const selecionadoId = ref(null)
const estadoDetalhe = ref('carregando')
const detalhe = ref(null) // { analise, ind, problemas, anterior }

const evolucao = ref([])

// Indicadores de um envio não mudam depois de gravados: cada um é buscado uma
// vez só, mesmo aparecendo no detalhe, na comparação e na evolução.
const cacheIndicadores = new Map()
function indicadoresDe(id) {
  if (!cacheIndicadores.has(id)) {
    const pedido = api.get(`/analises/${id}/indicadores`).then((r) => r.data)
    pedido.catch(() => cacheIndicadores.delete(id))
    cacheIndicadores.set(id, pedido)
  }
  return cacheIndicadores.get(id)
}

async function carregar() {
  estado.value = 'carregando'
  erro.value = ''
  try {
    const { data } = await api.get('/analises')
    historico.value = data
    if (!data.length) {
      estado.value = 'vazio'
      return
    }
    estado.value = 'pronto'
    selecionar(data[0].id)
    carregarEvolucao()
  } catch (falha) {
    erro.value = mensagemDeErro(falha)
    estado.value = 'erro'
  }
}

onMounted(carregar)

const posicaoSelecionada = computed(() => historico.value.findIndex((e) => e.id === selecionadoId.value))
const envioAnterior = computed(() => historico.value[posicaoSelecionada.value + 1] ?? null)

async function selecionar(id) {
  selecionadoId.value = id
  estadoDetalhe.value = 'carregando'
  const anterior = envioAnterior.value
  try {
    const [analise, ind, problemas, indAnterior] = await Promise.all([
      api.get(`/analises/${id}`).then((r) => r.data),
      indicadoresDe(id),
      api.get(`/analises/${id}/problemas`).then((r) => r.data),
      anterior ? indicadoresDe(anterior.id) : null,
    ])
    // Clique em outra linha no meio do caminho: esta resposta já não vale.
    if (selecionadoId.value !== id) return
    detalhe.value = { analise, ind, problemas, anterior: anterior && { ...anterior, ind: indAnterior } }
    estadoDetalhe.value = 'pronto'
  } catch (falha) {
    if (selecionadoId.value !== id) return
    erro.value = mensagemDeErro(falha)
    estadoDetalhe.value = 'erro'
  }
}

// A evolução é um extra: se falhar, o resto do relatório continua de pé.
async function carregarEvolucao() {
  const envios = historico.value.slice(0, LIMITE_EVOLUCAO).reverse()
  if (envios.length < 2) return
  try {
    const todos = await Promise.all(envios.map((e) => indicadoresDe(e.id)))
    evolucao.value = envios.map((envio, i) => ({
      id: envio.id,
      arquivo: envio.arquivo,
      data: new Date(envio.geradoEm),
      clientes: todos[i].qualidade.linhas_validas,
      faturamento: todos[i].faturamento.geral.soma ?? 0,
      aproveitamento: todos[i].qualidade.percentual_aproveitado,
    }))
  } catch {
    evolucao.value = []
  }
}

// ---------------------------------------------------------------------------
// Envio selecionado
// ---------------------------------------------------------------------------

const quadroValidacao = computed(() => {
  const q = detalhe.value.ind.qualidade
  return [
    ...TIPOS_ERRO.map(({ tipo, rotulo }) => ({ tipo, rotulo, quantidade: q.erros_por_tipo?.[tipo] ?? 0 })),
    { tipo: 'validos', rotulo: 'Registros válidos', quantidade: q.linhas_validas },
  ]
})

const tratamentos = computed(() => {
  const q = detalhe.value.ind.qualidade
  return TIPOS_AVISO.map(({ tipo, rotulo }) => ({ tipo, rotulo, quantidade: q.avisos_por_tipo?.[tipo] ?? 0 }))
})

// Uma linha por problema, como no relatório do navegador.
const ocorrencias = computed(() =>
  (detalhe.value.problemas.erros ?? []).flatMap((linha) =>
    linha.problemas.map((p) => ({
      linha: linha.linha,
      codigo: linha.codigo,
      campo: p.coluna,
      tipo: rotularTipo(p.tipo),
      descricao: p.mensagem,
    })),
  ),
)

const insights = computed(() => detalhe.value.analise.insights.filter((i) => i.origem === 'ANALISE'))

// Cor no quadro: verde só nos válidos, vermelho no erro que aconteceu, cinza
// no zero — pintar o zero de vermelho alarmaria por uma verificação que passou.
function quantidadeCor(item) {
  if (item.tipo === 'validos') return 'text-ok-600 dark:text-ok-400'
  if (!item.quantidade) return 'text-ink-400 dark:text-mist-500'
  return 'text-erro-600 dark:text-erro-400'
}

// ---------------------------------------------------------------------------
// Comparação com o envio anterior
// ---------------------------------------------------------------------------

const receitaA = (ind) => ind.faturamento.participacao_por_nivel.find((p) => p.nivel === 'A')?.percentual ?? 0

// tipo: numero | moeda | pontos (percentual, a diferença sai em pontos percentuais).
// menorMelhor: para a cor da variação — mais linhas recusadas é piora.
const METRICAS = [
  { rotulo: 'Linhas lidas', tipo: 'numero', valor: (i) => i.qualidade.linhas_lidas, neutra: true },
  { rotulo: 'Clientes gravados', tipo: 'numero', valor: (i) => i.qualidade.linhas_validas },
  { rotulo: 'Linhas recusadas', tipo: 'numero', valor: (i) => i.qualidade.linhas_rejeitadas, menorMelhor: true },
  { rotulo: 'Aproveitamento', tipo: 'pontos', valor: (i) => i.qualidade.percentual_aproveitado },
  { rotulo: 'Faturamento total', tipo: 'moeda', valor: (i) => i.faturamento.geral.soma ?? 0 },
  { rotulo: 'Ticket médio', tipo: 'moeda', valor: (i) => i.faturamento.geral.media ?? 0 },
  { rotulo: 'Faturamento mediano', tipo: 'moeda', valor: (i) => i.faturamento.geral.mediana ?? 0 },
  { rotulo: 'Receita vinda do nível A', tipo: 'pontos', valor: receitaA, neutra: true },
]

const comparacao = computed(() => {
  const { ind, anterior } = detalhe.value
  if (!anterior) return []
  return METRICAS.map((m) => ({ ...m, antes: m.valor(anterior.ind), agora: m.valor(ind) }))
})

// Segmentos dos dois envios juntos: um segmento que sumiu ou apareceu também
// é informação, e entra com zero do lado em que falta.
const comparacaoSegmentos = computed(() => {
  const { ind, anterior } = detalhe.value
  if (!anterior) return []
  const soma = (lista) => new Map(lista.map((s) => [s.segmento, s.soma ?? 0]))
  const antes = soma(anterior.ind.faturamento.por_segmento)
  const agora = soma(ind.faturamento.por_segmento)
  return [...new Set([...antes.keys(), ...agora.keys()])]
    .map((segmento) => ({ rotulo: segmento, tipo: 'moeda', antes: antes.get(segmento) ?? 0, agora: agora.get(segmento) ?? 0 }))
    .sort((a, b) => b.agora - a.agora)
})

function formatar(tipo, valor) {
  if (tipo === 'moeda') return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
  if (tipo === 'pontos') return `${formatarNumero(valor, 1)}%`
  return formatarNumero(valor)
}

function variacao(item) {
  const diferenca = item.agora - item.antes
  if (Math.abs(diferenca) < 0.005) return 'sem variação'
  const sinal = diferenca > 0 ? '+' : '−'
  if (item.tipo === 'pontos') return `${sinal}${formatarNumero(Math.abs(diferenca), 1)} p.p.`
  const absoluto = formatar(item.tipo, Math.abs(diferenca))
  if (!item.antes) return `${sinal}${absoluto}`
  return `${sinal}${absoluto} (${sinal}${formatarNumero(Math.abs((diferenca / item.antes) * 100), 1)}%)`
}

function variacaoCor(item) {
  const diferenca = item.agora - item.antes
  if (item.neutra || Math.abs(diferenca) < 0.005) return 'text-ink-600 dark:text-mist-300'
  const melhorou = item.menorMelhor ? diferenca < 0 : diferenca > 0
  return melhorou ? 'text-ok-600 dark:text-ok-400' : 'text-erro-600 dark:text-erro-400'
}

// ---------------------------------------------------------------------------
// Exportação do envio selecionado
// ---------------------------------------------------------------------------

// A planilha validada no navegador nesta sessão, se houver, entra no XLSX.
const planilha = useUploadStore()

const COLUNAS_PROBLEMAS = [
  ['Linha', (o) => o.linha],
  ['Código', (o) => o.codigo],
  ['Campo', (o) => o.campo],
  ['Tipo', (o) => o.tipo],
  ['Descrição do erro', (o) => o.descricao],
]
const COLUNAS_QUANTIDADE = (titulo) => [
  [titulo, (i) => i.rotulo],
  ['Quantidade', (i) => i.quantidade],
]
const COLUNAS_COMPARACAO = (titulo) => [
  [titulo, (i) => i.rotulo],
  ['Anterior', (i) => arredondar(i.antes)],
  ['Este envio', (i) => arredondar(i.agora)],
  ['Variação', (i) => variacao(i)],
]
const arredondar = (valor) => Math.round(valor * 100) / 100

const nomeBase = computed(() => {
  const { analise } = detalhe.value
  const arquivo = analise.arquivo.replace(/\.[^.]+$/, '').replace(/[^\w-]+/g, '-')
  return `relatorio-${arquivo}-envio-${analise.id}`
})

function exportarCsv() {
  baixarCsv(`${nomeBase.value}-problemas.csv`, { colunas: COLUNAS_PROBLEMAS, linhas: ocorrencias.value })
}

function exportarXlsx() {
  const { analise, ind, anterior } = detalhe.value
  const geral = ind.faturamento.geral
  const resumo = [
    ['Arquivo', analise.arquivo],
    ['Processado em', dataHora(analise.geradoEm)],
    ['Linhas lidas', analise.linhasLidas],
    ['Linhas gravadas', analise.linhasValidas],
    ['Linhas recusadas', analise.linhasRejeitadas],
    ['Aproveitamento (%)', arredondar(ind.qualidade.percentual_aproveitado)],
    ['Clientes novos', analise.clientesCriados],
    ['Clientes atualizados', analise.clientesAtualizados],
    ['Faturamento total (R$)', arredondar(geral.soma ?? 0)],
    ['Ticket médio (R$)', arredondar(geral.media ?? 0)],
    ['Faturamento mediano (R$)', arredondar(geral.mediana ?? 0)],
  ]
  if (anterior) resumo.push(['Comparado com', `${anterior.arquivo} (${dataHora(anterior.geradoEm)})`])

  const abas = [
    { nome: 'Resumo', colunas: [['Indicador', (l) => l[0]], ['Valor', (l) => l[1]]], linhas: resumo },
    { nome: 'Validação', colunas: COLUNAS_QUANTIDADE('Verificação'), linhas: quadroValidacao.value },
    { nome: 'Tratamentos', colunas: COLUNAS_QUANTIDADE('Tratamento'), linhas: tratamentos.value },
    { nome: 'Problemas', colunas: COLUNAS_PROBLEMAS, linhas: ocorrencias.value },
    { nome: 'Insights', colunas: [['Título', (i) => i.titulo], ['Descrição', (i) => i.descricao]], linhas: insights.value },
  ]
  if (anterior) {
    abas.push(
      { nome: 'Comparação', colunas: COLUNAS_COMPARACAO('Indicador'), linhas: comparacao.value },
      { nome: 'Comparação por segmento', colunas: COLUNAS_COMPARACAO('Segmento'), linhas: comparacaoSegmentos.value },
    )
  }
  abas.push({
    nome: 'Histórico',
    colunas: [
      ['Arquivo', (e) => e.arquivo],
      ['Processado em', (e) => dataHora(e.geradoEm)],
      ['Lidas', (e) => e.linhasLidas],
      ['Gravadas', (e) => e.linhasValidas],
      ['Recusadas', (e) => e.linhasRejeitadas],
    ],
    linhas: historico.value,
  })

  // A validação do navegador é de outra planilha possivelmente (a carregada
  // agora no Upload), então vai em abas próprias, com o nome do arquivo.
  if (planilha.quantidadeLinhas > 0) {
    abas.push(
      {
        nome: 'Navegador - Validação',
        colunas: COLUNAS_QUANTIDADE(`Verificação (${planilha.nomeArquivo})`),
        linhas: planilha.quadroValidacao,
      },
      { nome: 'Navegador - Tratamentos', colunas: COLUNAS_QUANTIDADE('Tratamento'), linhas: planilha.avisosPorTipo },
      {
        nome: 'Navegador - Problemas',
        colunas: COLUNAS_PROBLEMAS,
        linhas: planilha.ocorrencias.map((o) => ({ ...o, tipo: o.rotulo })),
      },
    )
  }

  baixarXlsx(`${nomeBase.value}.xlsx`, abas)
}

// O PDF sai da impressão do navegador ("Salvar como PDF"): a tela já está
// preparada para o papel (sem menu, sem botões, sempre no tema claro).
const exportarPdf = () => window.print()

// ---------------------------------------------------------------------------

const inteiro = (valor) => valor.toLocaleString('pt-BR')
const porcento = (valor) => `${formatarNumero(valor, 1)}%`
const dinheiro = (valor) => valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
const dataHora = (iso) =>
  new Date(iso).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })

const classeCartao = 'rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900'
const classeTh = 'py-2 pr-4 font-medium text-ink-600 dark:text-mist-300'
const classeLinha = 'border-b border-ink-200 last:border-0 dark:border-night-700'
const classeBotao =
  'rounded border border-ink-200 px-4 py-1.5 text-sm font-medium text-brand-800 transition-colors hover:border-brand-500/50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 disabled:cursor-not-allowed disabled:opacity-50 dark:border-night-700 dark:text-brand-300'
</script>

<template>
  <p class="mt-10 text-sm text-ink-600 dark:text-mist-300" v-if="estado === 'carregando'" role="status">
    Carregando os processamentos…
  </p>

  <section class="mt-10" v-else-if="estado === 'vazio'">
    <h2 class="text-lg font-semibold text-ink-900 dark:text-mist-100">Processamentos no servidor</h2>
    <p class="mt-4 rounded border border-ink-200 bg-white px-4 py-3 text-sm text-ink-600 dark:border-night-700 dark:bg-night-900 dark:text-mist-300">
      Nenhuma planilha foi processada ainda.
    </p>
  </section>

  <div
    class="mt-10 rounded border border-erro-600 bg-erro-600/5 p-4 text-sm dark:border-erro-400 dark:bg-erro-400/10"
    v-else-if="estado === 'erro'"
    role="alert"
  >
    <p class="text-ink-900 dark:text-mist-100">Não foi possível consultar o servidor: {{ erro }}</p>
    <button
      class="mt-3 rounded border border-ink-200 px-4 py-2 font-semibold text-brand-800 transition-colors hover:border-brand-500 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:border-night-700 dark:text-brand-300"
      type="button"
      @click="carregar"
    >
      Tentar de novo
    </button>
  </div>

  <template v-else>
    <!-- ===================================================================
         Histórico: cada linha abre o relatório daquele envio.
         =================================================================== -->
    <section class="mt-10 min-w-0">
      <h2 class="text-lg font-semibold text-ink-900 dark:text-mist-100">Histórico de processamentos</h2>
      <p class="mt-1 text-sm text-ink-600 dark:text-mist-300">
        Todos os envios ficam guardados. Clique em um envio para ver o relatório dele e a comparação com o
        envio anterior.
      </p>

      <div class="mt-4 max-h-80 overflow-auto print:max-h-none print:overflow-visible" :class="classeCartao">
        <table class="w-full min-w-136 text-left text-sm">
          <thead class="sticky top-0 bg-white dark:bg-night-900">
            <tr class="border-b border-ink-200 dark:border-night-700">
              <th :class="classeTh" scope="col">Arquivo</th>
              <th :class="classeTh" scope="col">Processado em</th>
              <th :class="[classeTh, 'text-right']" scope="col">Lidas</th>
              <th :class="[classeTh, 'text-right']" scope="col">Gravadas</th>
              <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Recusadas</th>
            </tr>
          </thead>
          <tbody>
            <tr
              :class="[classeLinha, envio.id === selecionadoId ? 'bg-brand-500/10' : '']"
              v-for="envio in historico"
              :key="envio.id"
              :aria-current="envio.id === selecionadoId ? 'true' : undefined"
            >
              <td class="py-2 pr-4">
                <button
                  class="text-left font-medium text-brand-800 underline-offset-2 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:text-brand-300"
                  type="button"
                  @click="selecionar(envio.id)"
                >
                  {{ envio.arquivo }}
                </button>
              </td>
              <td class="py-2 pr-4 text-ink-600 dark:text-mist-300">{{ dataHora(envio.geradoEm) }}</td>
              <td class="py-2 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ inteiro(envio.linhasLidas) }}</td>
              <td class="py-2 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ inteiro(envio.linhasValidas) }}</td>
              <td class="py-2 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ inteiro(envio.linhasRejeitadas) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- ===================================================================
         Relatório do envio selecionado (por padrão, o mais recente).
         =================================================================== -->
    <section class="mt-10">
      <p class="text-sm text-ink-600 dark:text-mist-300" v-if="estadoDetalhe === 'carregando'" role="status">
        Carregando o envio…
      </p>
      <p
        class="rounded border border-erro-600 bg-erro-600/5 p-4 text-sm text-ink-900 dark:border-erro-400 dark:bg-erro-400/10 dark:text-mist-100"
        v-else-if="estadoDetalhe === 'erro'"
        role="alert"
      >
        Não foi possível abrir este envio: {{ erro }}
      </p>

      <template v-else>
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div class="min-w-0">
            <h2 class="text-lg font-semibold text-ink-900 dark:text-mist-100">
              {{ posicaoSelecionada === 0 ? 'Último processamento no servidor' : 'Processamento selecionado' }}
            </h2>
            <p class="mt-1 max-w-2xl text-sm text-ink-600 dark:text-mist-300">
              O servidor trata a planilha de novo em Python antes de gravar, então estes números podem diferir da
              prévia do navegador.
            </p>
          </div>

          <!-- Exportam o envio aberto aqui, seja o mais recente ou um antigo. -->
          <div class="flex flex-wrap gap-2 print:hidden" role="group" aria-label="Exportar este relatório">
            <button
              :class="classeBotao"
              type="button"
              :disabled="!ocorrencias.length"
              :title="ocorrencias.length ? 'Lista de problemas (linha, campo e descrição)' : 'Este envio não teve linhas recusadas'"
              @click="exportarCsv"
            >
              Exportar CSV
            </button>
            <button :class="classeBotao" type="button" title="Relatório completo, uma aba por tabela" @click="exportarXlsx">
              Exportar XLSX
            </button>
            <button :class="classeBotao" type="button" title="Abre a impressão: escolha &quot;Salvar como PDF&quot;" @click="exportarPdf">
              Exportar PDF
            </button>
          </div>
        </div>

        <dl class="mt-4 grid grid-cols-2 gap-px overflow-hidden rounded border border-ink-200 bg-ink-200 sm:grid-cols-4 dark:border-night-700 dark:bg-night-700">
          <div class="bg-white px-4 py-3 dark:bg-night-900">
            <dt class="text-xs text-ink-600 dark:text-mist-300">Arquivo</dt>
            <dd class="mt-1 truncate text-sm font-semibold text-ink-900 dark:text-mist-100" :title="detalhe.analise.arquivo">
              {{ detalhe.analise.arquivo }}
            </dd>
          </div>
          <div class="bg-white px-4 py-3 dark:bg-night-900">
            <dt class="text-xs text-ink-600 dark:text-mist-300">Processado em</dt>
            <dd class="mt-1 text-sm font-semibold tabular-nums text-ink-900 dark:text-mist-100">{{ dataHora(detalhe.analise.geradoEm) }}</dd>
          </div>
          <div class="bg-white px-4 py-3 dark:bg-night-900">
            <dt class="text-xs text-ink-600 dark:text-mist-300">Linhas lidas</dt>
            <dd class="mt-1 text-sm font-semibold tabular-nums text-ink-900 dark:text-mist-100">{{ inteiro(detalhe.analise.linhasLidas) }}</dd>
          </div>
          <div class="bg-white px-4 py-3 dark:bg-night-900">
            <dt class="text-xs text-ink-600 dark:text-mist-300">Aproveitamento</dt>
            <dd class="mt-1 text-sm font-semibold tabular-nums text-ink-900 dark:text-mist-100">
              {{ porcento(detalhe.ind.qualidade.percentual_aproveitado) }}
            </dd>
          </div>
          <div class="bg-white px-4 py-3 dark:bg-night-900">
            <dt class="text-xs text-ink-600 dark:text-mist-300">Linhas gravadas</dt>
            <dd class="mt-1 text-sm font-semibold tabular-nums text-ok-600 dark:text-ok-400">{{ inteiro(detalhe.analise.linhasValidas) }}</dd>
          </div>
          <div class="bg-white px-4 py-3 dark:bg-night-900">
            <dt class="text-xs text-ink-600 dark:text-mist-300">Linhas recusadas</dt>
            <dd
              class="mt-1 text-sm font-semibold tabular-nums"
              :class="detalhe.analise.linhasRejeitadas ? 'text-erro-600 dark:text-erro-400' : 'text-ink-900 dark:text-mist-100'"
            >
              {{ inteiro(detalhe.analise.linhasRejeitadas) }}
            </dd>
          </div>
          <div class="bg-white px-4 py-3 dark:bg-night-900">
            <dt class="text-xs text-ink-600 dark:text-mist-300">Clientes novos</dt>
            <dd class="mt-1 text-sm font-semibold tabular-nums text-ink-900 dark:text-mist-100">{{ inteiro(detalhe.analise.clientesCriados) }}</dd>
          </div>
          <div class="bg-white px-4 py-3 dark:bg-night-900">
            <dt class="text-xs text-ink-600 dark:text-mist-300">Clientes atualizados</dt>
            <dd class="mt-1 text-sm font-semibold tabular-nums text-ink-900 dark:text-mist-100">{{ inteiro(detalhe.analise.clientesAtualizados) }}</dd>
          </div>
        </dl>

        <div class="mt-4 grid gap-4 lg:grid-cols-2">
          <div :class="classeCartao">
            <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Validação no servidor</h3>
            <table class="mt-3 w-full text-left text-sm">
              <thead>
                <tr class="border-b border-ink-200 dark:border-night-700">
                  <th :class="classeTh" scope="col">Verificação</th>
                  <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Quantidade</th>
                </tr>
              </thead>
              <tbody>
                <tr :class="classeLinha" v-for="item in quadroValidacao" :key="item.tipo">
                  <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
                  <td class="py-2 text-right font-semibold tabular-nums" :class="quantidadeCor(item)">{{ inteiro(item.quantidade) }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div :class="classeCartao">
            <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Tratamentos aplicados no servidor</h3>
            <table class="mt-3 w-full text-left text-sm">
              <thead>
                <tr class="border-b border-ink-200 dark:border-night-700">
                  <th :class="classeTh" scope="col">Tratamento</th>
                  <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Quantidade</th>
                </tr>
              </thead>
              <tbody>
                <tr :class="classeLinha" v-for="item in tratamentos" :key="item.tipo">
                  <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
                  <td
                    class="py-2 text-right font-semibold tabular-nums"
                    :class="item.quantidade ? 'text-alerta-600 dark:text-alerta-400' : 'text-ink-400 dark:text-mist-500'"
                  >
                    {{ inteiro(item.quantidade) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="mt-4 min-w-0" :class="classeCartao" v-if="ocorrencias.length">
          <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">
            Problemas que recusaram linhas ({{ inteiro(ocorrencias.length) }})
          </h3>
          <div class="mt-3 max-h-80 overflow-auto print:max-h-none print:overflow-visible">
            <table class="w-full min-w-2xl text-left text-sm">
              <thead class="sticky top-0 bg-white dark:bg-night-900">
                <tr class="border-b border-ink-200 dark:border-night-700">
                  <th :class="classeTh" scope="col">Linha</th>
                  <th :class="classeTh" scope="col">Código</th>
                  <th :class="classeTh" scope="col">Campo</th>
                  <th :class="classeTh" scope="col">Tipo</th>
                  <th class="py-2 font-medium text-ink-600 dark:text-mist-300" scope="col">Descrição do erro</th>
                </tr>
              </thead>
              <tbody>
                <tr :class="classeLinha" v-for="(item, i) in ocorrencias" :key="`${item.linha}-${item.campo}-${i}`">
                  <td class="py-2 pr-4 tabular-nums text-ink-400 dark:text-mist-500">{{ item.linha }}</td>
                  <td class="py-2 pr-4 whitespace-nowrap text-ink-600 dark:text-mist-300">{{ item.codigo }}</td>
                  <td class="py-2 pr-4 whitespace-nowrap font-medium text-ink-900 dark:text-mist-100">{{ item.campo }}</td>
                  <td class="py-2 pr-4 whitespace-nowrap text-ink-600 dark:text-mist-300">{{ item.tipo }}</td>
                  <td class="py-2 text-erro-600 dark:text-erro-400">{{ item.descricao }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="mt-4" :class="classeCartao" v-if="insights.length">
          <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Insights gerados neste envio</h3>
          <ul class="mt-3 grid gap-3 md:grid-cols-2">
            <li class="rounded border border-ink-200 p-3 dark:border-night-700" v-for="insight in insights" :key="insight.id">
              <p class="text-sm font-semibold text-ink-900 dark:text-mist-100">{{ insight.titulo }}</p>
              <p class="mt-1 text-sm leading-relaxed text-ink-600 dark:text-mist-300">{{ insight.descricao }}</p>
            </li>
          </ul>
        </div>

        <!-- Comparação com o envio imediatamente anterior ao selecionado. -->
        <div class="mt-10" v-if="detalhe.anterior">
          <h2 class="text-lg font-semibold text-ink-900 dark:text-mist-100">Comparação com o envio anterior</h2>
          <p class="mt-1 text-sm text-ink-600 dark:text-mist-300">
            {{ detalhe.anterior.arquivo }}, processado em {{ dataHora(detalhe.anterior.geradoEm) }}.
            Verde é melhora e vermelho é piora; cinza não tem leitura de melhor ou pior.
          </p>

          <div class="mt-4 grid gap-4 lg:grid-cols-2">
            <div class="min-w-0 overflow-x-auto" :class="classeCartao">
              <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Indicadores gerais</h3>
              <table class="mt-3 w-full min-w-md text-left text-sm">
                <thead>
                  <tr class="border-b border-ink-200 dark:border-night-700">
                    <th :class="classeTh" scope="col">Indicador</th>
                    <th :class="[classeTh, 'text-right']" scope="col">Anterior</th>
                    <th :class="[classeTh, 'text-right']" scope="col">Este envio</th>
                    <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Variação</th>
                  </tr>
                </thead>
                <tbody>
                  <tr :class="classeLinha" v-for="item in comparacao" :key="item.rotulo">
                    <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
                    <td class="py-2 pr-4 text-right whitespace-nowrap tabular-nums text-ink-600 dark:text-mist-300">{{ formatar(item.tipo, item.antes) }}</td>
                    <td class="py-2 pr-4 text-right whitespace-nowrap tabular-nums text-ink-900 dark:text-mist-100">{{ formatar(item.tipo, item.agora) }}</td>
                    <td class="py-2 text-right whitespace-nowrap tabular-nums font-medium" :class="variacaoCor(item)">{{ variacao(item) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="min-w-0 overflow-x-auto" :class="classeCartao">
              <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Faturamento por segmento</h3>
              <table class="mt-3 w-full min-w-md text-left text-sm">
                <thead>
                  <tr class="border-b border-ink-200 dark:border-night-700">
                    <th :class="classeTh" scope="col">Segmento</th>
                    <th :class="[classeTh, 'text-right']" scope="col">Anterior</th>
                    <th :class="[classeTh, 'text-right']" scope="col">Este envio</th>
                    <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Variação</th>
                  </tr>
                </thead>
                <tbody>
                  <tr :class="classeLinha" v-for="item in comparacaoSegmentos" :key="item.rotulo">
                    <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
                    <td class="py-2 pr-4 text-right whitespace-nowrap tabular-nums text-ink-600 dark:text-mist-300">{{ formatar(item.tipo, item.antes) }}</td>
                    <td class="py-2 pr-4 text-right whitespace-nowrap tabular-nums text-ink-900 dark:text-mist-100">{{ formatar(item.tipo, item.agora) }}</td>
                    <td class="py-2 text-right whitespace-nowrap tabular-nums font-medium" :class="variacaoCor(item)">{{ variacao(item) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
        <p class="mt-6 text-sm text-ink-600 dark:text-mist-300" v-else>
          Este é o primeiro envio guardado, então não há envio anterior para comparar.
        </p>
      </template>
    </section>

    <!-- ===================================================================
         Evolução: os últimos envios lado a lado, do mais antigo ao mais novo.
         =================================================================== -->
    <section class="mt-10" v-if="evolucao.length > 1">
      <h2 class="text-lg font-semibold text-ink-900 dark:text-mist-100">Evolução entre envios</h2>
      <p class="mt-1 text-sm text-ink-600 dark:text-mist-300">
        Os últimos {{ evolucao.length }} envios, do mais antigo para o mais recente.
      </p>

      <div class="mt-4 grid gap-4 lg:grid-cols-2">
        <CartaoGrafico titulo="Faturamento total por envio" descricao="Soma do faturamento anual dos clientes gravados em cada envio.">
          <GraficoArea :pontos="evolucao" campo="faturamento" formato="moeda" unidade="" descricao="Faturamento total por envio." />
          <template #tabela>
            <table class="w-full text-left text-sm">
              <thead>
                <tr class="border-b border-ink-200 dark:border-night-700">
                  <th :class="classeTh" scope="col">Envio</th>
                  <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Faturamento</th>
                </tr>
              </thead>
              <tbody>
                <tr :class="classeLinha" v-for="ponto in evolucao" :key="ponto.id">
                  <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ ponto.arquivo }} · {{ dataHora(ponto.data) }}</td>
                  <td class="py-2 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ dinheiro(ponto.faturamento) }}</td>
                </tr>
              </tbody>
            </table>
          </template>
        </CartaoGrafico>

        <CartaoGrafico titulo="Clientes gravados por envio" descricao="Linhas aproveitadas pelo servidor em cada envio.">
          <GraficoArea :pontos="evolucao" campo="clientes" unidade="clientes" descricao="Clientes gravados por envio." />
          <template #tabela>
            <table class="w-full text-left text-sm">
              <thead>
                <tr class="border-b border-ink-200 dark:border-night-700">
                  <th :class="classeTh" scope="col">Envio</th>
                  <th :class="[classeTh, 'text-right']" scope="col">Clientes</th>
                  <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Aproveitamento</th>
                </tr>
              </thead>
              <tbody>
                <tr :class="classeLinha" v-for="ponto in evolucao" :key="ponto.id">
                  <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ ponto.arquivo }} · {{ dataHora(ponto.data) }}</td>
                  <td class="py-2 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ inteiro(ponto.clientes) }}</td>
                  <td class="py-2 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ porcento(ponto.aproveitamento) }}</td>
                </tr>
              </tbody>
            </table>
          </template>
        </CartaoGrafico>
      </div>
    </section>
  </template>
</template>
