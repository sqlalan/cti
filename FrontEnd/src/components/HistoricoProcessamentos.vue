<script setup>
import { computed, onMounted, ref } from 'vue'
import CartaoGrafico from './graficos/CartaoGrafico.vue'
import GraficoArea from './graficos/GraficoArea.vue'
import StatusBadge from './StatusBadge.vue'
import { formatarNumero } from './graficos/escala'
import { baixarCsv, baixarXlsx } from '../dados/exportar'
import { dashboardService, mensagemDeErro, uploadService } from '../services/api'
import { explicarLote } from '../dados/lote'
import { useUploadStore } from '../stores/uploadStore'

// Todos os lotes enviados (upload_lote) e, para os PROCESSADOS, o JSON que o
// Python gravou em estatistica_lote. Dá para abrir um lote antigo e comparar
// com o processado anterior.

const LIMITE_EVOLUCAO = 12

// carregando → pronto | vazio (nenhum lote) | erro
const estado = ref('carregando')
const erro = ref('')
const historico = ref([]) // GET /api/uploads, do mais recente para o mais antigo

const selecionadoId = ref(null)
const estadoDetalhe = ref('carregando')
const detalhe = ref(null) // { lote, est, anterior }

const evolucao = ref([])

// O resultado de um lote processado não muda: cada um é buscado uma vez só,
// mesmo aparecendo no detalhe, na comparação e na evolução.
const cacheEstatisticas = new Map()
function estatisticasDe(id) {
  if (!cacheEstatisticas.has(id)) {
    const pedido = dashboardService.obter(id).then((r) => r.estatisticas)
    pedido.catch(() => cacheEstatisticas.delete(id))
    cacheEstatisticas.set(id, pedido)
  }
  return cacheEstatisticas.get(id)
}

const processados = computed(() => historico.value.filter((l) => l.status === 'PROCESSADO'))

async function carregar() {
  estado.value = 'carregando'
  erro.value = ''
  try {
    historico.value = await uploadService.listar()
    if (!historico.value.length) {
      estado.value = 'vazio'
      return
    }
    estado.value = 'pronto'
    selecionar(historico.value[0].id)
    carregarEvolucao()
  } catch (falha) {
    erro.value = mensagemDeErro(falha)
    estado.value = 'erro'
  }
}

onMounted(carregar)

const posicaoSelecionada = computed(() => historico.value.findIndex((l) => l.id === selecionadoId.value))

// O processado mais recente antes do selecionado: só lote com resultado compara.
const loteAnterior = computed(() =>
  historico.value.slice(posicaoSelecionada.value + 1).find((l) => l.status === 'PROCESSADO') ?? null,
)

async function selecionar(id) {
  selecionadoId.value = id
  estadoDetalhe.value = 'carregando'
  const lote = historico.value.find((l) => l.id === id)
  const anterior = lote.status === 'PROCESSADO' ? loteAnterior.value : null
  try {
    const [est, estAnterior] = await Promise.all([
      lote.status === 'PROCESSADO' ? estatisticasDe(id) : null,
      anterior ? estatisticasDe(anterior.id) : null,
    ])
    // Clique em outra linha no meio do caminho: esta resposta já não vale.
    if (selecionadoId.value !== id) return
    detalhe.value = { lote, est, anterior: anterior && { ...anterior, est: estAnterior } }
    estadoDetalhe.value = 'pronto'
  } catch (falha) {
    if (selecionadoId.value !== id) return
    erro.value = mensagemDeErro(falha)
    estadoDetalhe.value = 'erro'
  }
}

// A evolução é um extra: se falhar, o resto do relatório continua de pé.
async function carregarEvolucao() {
  const lotes = processados.value.slice(0, LIMITE_EVOLUCAO).reverse()
  if (lotes.length < 2) return
  try {
    const todos = await Promise.all(lotes.map((l) => estatisticasDe(l.id)))
    evolucao.value = lotes.map((lote, i) => ({
      id: lote.id,
      arquivo: lote.arquivoNome,
      data: new Date(lote.enviadoEm),
      clientes: todos[i].resumo.totalClientes,
      faturamento: todos[i].resumo.faturamentoTotal ?? 0,
      aproveitamento: aproveitamento(todos[i]),
    }))
  } catch {
    evolucao.value = []
  }
}

// ---------------------------------------------------------------------------
// Lote selecionado
// ---------------------------------------------------------------------------

const aproveitamento = (est) => {
  const q = est.qualidade
  return q.linhasRecebidas ? (q.linhasValidas / q.linhasRecebidas) * 100 : 0
}

const quadroQualidade = computed(() => {
  const q = detalhe.value.est.qualidade
  return [
    { tipo: 'recebidas', rotulo: 'Linhas recebidas', quantidade: q.linhasRecebidas },
    { tipo: 'invalidas', rotulo: 'Linhas inválidas (recusadas na validação)', quantidade: q.linhasInvalidas },
    { tipo: 'validos', rotulo: 'Clientes gravados', quantidade: q.linhasValidas },
  ]
})

const padronizacoes = computed(() => {
  const q = detalhe.value.est.qualidade
  return [
    { tipo: 'segmento', rotulo: 'Segmentos padronizados', quantidade: q.correcoesSegmento },
    { tipo: 'consultor', rotulo: 'Nomes de consultor ajustados', quantidade: q.correcoesConsultor },
    { tipo: 'nivel', rotulo: 'Níveis ajustados', quantidade: q.correcoesNivel },
  ]
})

const houvePadronizacao = computed(() => padronizacoes.value.some((p) => p.quantidade > 0))

// Só as formas que de fato mudaram ("IND." → "Indústria").
const segmentosCorrigidos = computed(() =>
  (detalhe.value.est.qualidade.segmentosOriginais ?? []).filter((s) => s.original !== s.padronizado),
)

const insights = computed(() => detalhe.value.est.insights ?? [])

function quantidadeCor(item) {
  if (item.tipo === 'validos') return 'text-ok-600 dark:text-ok-400'
  if (item.tipo === 'recebidas' || !item.quantidade) return 'text-ink-900 dark:text-mist-100'
  return 'text-erro-600 dark:text-erro-400'
}

// ---------------------------------------------------------------------------
// Comparação com o lote processado anterior
// ---------------------------------------------------------------------------

const receitaA = (est) => est.porNivel.find((n) => n.nivel === 'A')?.percentualFaturamento ?? 0

// tipo: numero | moeda | pontos (percentual, a diferença sai em pontos percentuais).
// menorMelhor: para a cor da variação — mais linhas recusadas é piora.
const METRICAS = [
  { rotulo: 'Linhas recebidas', tipo: 'numero', valor: (e) => e.qualidade.linhasRecebidas, neutra: true },
  { rotulo: 'Clientes gravados', tipo: 'numero', valor: (e) => e.resumo.totalClientes },
  { rotulo: 'Linhas inválidas', tipo: 'numero', valor: (e) => e.qualidade.linhasInvalidas, menorMelhor: true },
  { rotulo: 'Aproveitamento', tipo: 'pontos', valor: aproveitamento },
  { rotulo: 'Faturamento total', tipo: 'moeda', valor: (e) => e.resumo.faturamentoTotal ?? 0 },
  { rotulo: 'Média de faturamento', tipo: 'moeda', valor: (e) => e.resumo.media ?? 0 },
  { rotulo: 'Mediana de faturamento', tipo: 'moeda', valor: (e) => e.resumo.mediana ?? 0 },
  { rotulo: 'Receita vinda do nível A', tipo: 'pontos', valor: receitaA, neutra: true },
]

const comparacao = computed(() => {
  const { est, anterior } = detalhe.value
  if (!anterior) return []
  return METRICAS.map((m) => ({ ...m, antes: m.valor(anterior.est), agora: m.valor(est) }))
})

// Segmentos dos dois lotes juntos: um segmento que sumiu ou apareceu também
// é informação, e entra com zero do lado em que falta.
const comparacaoSegmentos = computed(() => {
  const { est, anterior } = detalhe.value
  if (!anterior) return []
  const soma = (lista) => new Map(lista.map((s) => [s.segmento, s.faturamentoTotal ?? 0]))
  const antes = soma(anterior.est.porSegmento)
  const agora = soma(est.porSegmento)
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
// Exportação do lote selecionado
// ---------------------------------------------------------------------------

// A planilha validada no navegador nesta sessão, se houver, entra no XLSX.
const planilha = useUploadStore()

const COLUNAS_SEGMENTO = [
  ['Segmento', (s) => s.segmento],
  ['Clientes', (s) => s.clientes],
  ['% dos clientes', (s) => s.percentual],
  ['Faturamento total', (s) => s.faturamentoTotal],
  ['% do faturamento', (s) => s.percentualFaturamento],
  ['Média', (s) => s.media],
  ['Mediana', (s) => s.mediana],
]
const COLUNAS_GRUPO = (titulo, campo) => [
  [titulo, (g) => g[campo]],
  ['Clientes', (g) => g.clientes],
  ['Faturamento total', (g) => g.faturamentoTotal],
  ['Média', (g) => g.media],
]
const COLUNAS_QUANTIDADE = (titulo) => [
  [titulo, (i) => i.rotulo],
  ['Quantidade', (i) => i.quantidade],
]
const COLUNAS_COMPARACAO = (titulo) => [
  [titulo, (i) => i.rotulo],
  ['Anterior', (i) => arredondar(i.antes)],
  ['Este lote', (i) => arredondar(i.agora)],
  ['Variação', (i) => variacao(i)],
]
const COLUNAS_PROBLEMAS = [
  ['Linha', (o) => o.linha],
  ['Código', (o) => o.codigo],
  ['Campo', (o) => o.campo],
  ['Tipo', (o) => o.rotulo],
  ['Descrição do erro', (o) => o.descricao],
]
const arredondar = (valor) => Math.round(valor * 100) / 100

const nomeBase = computed(() => {
  const { lote } = detalhe.value
  const arquivo = lote.arquivoNome.replace(/\.[^.]+$/, '').replace(/[^\w-]+/g, '-')
  return `relatorio-${arquivo}-lote-${lote.id}`
})

function exportarCsv() {
  baixarCsv(`${nomeBase.value}-segmentos.csv`, { colunas: COLUNAS_SEGMENTO, linhas: detalhe.value.est.porSegmento })
}

function exportarXlsx() {
  const { lote, est, anterior } = detalhe.value
  const r = est.resumo
  const resumo = [
    ['Lote', lote.id],
    ['Arquivo', lote.arquivoNome],
    ['Enviado em', dataHora(lote.enviadoEm)],
    ['Enviado por', lote.enviadoPor],
    ['Linhas recebidas', est.qualidade.linhasRecebidas],
    ['Clientes gravados', r.totalClientes],
    ['Linhas inválidas', est.qualidade.linhasInvalidas],
    ['Aproveitamento (%)', arredondar(aproveitamento(est))],
    ['Faturamento total (R$)', arredondar(r.faturamentoTotal ?? 0)],
    ['Média (R$)', arredondar(r.media ?? 0)],
    ['Mediana (R$)', arredondar(r.mediana ?? 0)],
    ['Desvio padrão (R$)', arredondar(r.desvioPadrao ?? 0)],
    ['Segmento mais frequente', r.segmentoMaisFrequente],
    ['Nível mais frequente', r.nivelMaisFrequente],
    ['Serviço mais contratado', r.servicoMaisContratado],
  ]
  if (anterior) resumo.push(['Comparado com', `Lote ${anterior.id} · ${anterior.arquivoNome} (${dataHora(anterior.enviadoEm)})`])

  const abas = [
    { nome: 'Resumo', colunas: [['Indicador', (l) => l[0]], ['Valor', (l) => l[1]]], linhas: resumo },
    { nome: 'Qualidade', colunas: COLUNAS_QUANTIDADE('Verificação'), linhas: quadroQualidade.value },
    { nome: 'Padronizações', colunas: COLUNAS_QUANTIDADE('Padronização'), linhas: padronizacoes.value },
    { nome: 'Por segmento', colunas: COLUNAS_SEGMENTO, linhas: est.porSegmento },
    { nome: 'Por nível', colunas: COLUNAS_GRUPO('Nível', 'nivel'), linhas: est.porNivel },
    { nome: 'Por consultor', colunas: COLUNAS_GRUPO('Consultor', 'consultor'), linhas: est.porConsultor },
    {
      nome: 'Por serviço',
      colunas: [['Serviço', (s) => s.servico], ['Clientes', (s) => s.contratos], ['% dos clientes', (s) => s.percentual]],
      linhas: est.porServico,
    },
    {
      nome: 'Por mês',
      colunas: [['Mês', (m) => m.mes], ['Contratações', (m) => m.clientes], ['Acumulado', (m) => m.acumulado]],
      linhas: est.porMes,
    },
    { nome: 'Insights', colunas: [['Frase', (f) => f]], linhas: insights.value },
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
      ['Lote', (l) => l.id],
      ['Arquivo', (l) => l.arquivoNome],
      ['Enviado em', (l) => dataHora(l.enviadoEm)],
      ['Status', (l) => l.status],
      ['Recebidas', (l) => l.totalLinhas],
      ['Válidas', (l) => l.linhasValidas],
      ['Inválidas', (l) => l.linhasInvalidas],
    ],
    linhas: historico.value,
  })

  // A validação do navegador é possivelmente de outra planilha (a carregada
  // agora no Upload), então vai em abas próprias, com o nome do arquivo.
  if (planilha.quantidadeLinhas > 0) {
    abas.push(
      {
        nome: 'Navegador - Validação',
        colunas: COLUNAS_QUANTIDADE(`Verificação (${planilha.nomeArquivo})`),
        linhas: planilha.quadroValidacao,
      },
      { nome: 'Navegador - Tratamentos', colunas: COLUNAS_QUANTIDADE('Tratamento'), linhas: planilha.avisosPorTipo },
      { nome: 'Navegador - Problemas', colunas: COLUNAS_PROBLEMAS, linhas: planilha.ocorrencias },
    )
  }

  baixarXlsx(`${nomeBase.value}.xlsx`, abas)
}

// O PDF sai da impressão do navegador ("Salvar como PDF"): a tela já está
// preparada para o papel (sem menu, sem botões, sempre no tema claro).
const exportarPdf = () => window.print()

// ---------------------------------------------------------------------------

const inteiro = (valor) => (valor ?? 0).toLocaleString('pt-BR')
const porcento = (valor) => `${formatarNumero(valor, 1)}%`
const dinheiro = (valor) => (valor ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
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
    Carregando os lotes enviados…
  </p>

  <section class="mt-10" v-else-if="estado === 'vazio'">
    <h2 class="text-lg font-semibold text-ink-900 dark:text-mist-100">Lotes enviados</h2>
    <p class="mt-4 rounded border border-ink-200 bg-white px-4 py-3 text-sm text-ink-600 dark:border-night-700 dark:bg-night-900 dark:text-mist-300">
      Nenhuma planilha foi enviada ainda.
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
         Histórico: cada linha abre o relatório daquele lote.
         =================================================================== -->
    <section class="mt-10 min-w-0">
      <h2 class="text-lg font-semibold text-ink-900 dark:text-mist-100">Lotes enviados</h2>
      <p class="mt-1 text-sm text-ink-600 dark:text-mist-300">
        Cada envio vira um lote. Clique em um lote para ver os indicadores calculados e a comparação com o
        lote processado anterior.
      </p>

      <div class="mt-4 max-h-80 overflow-auto print:max-h-none print:overflow-visible" :class="classeCartao">
        <table class="w-full min-w-xl text-left text-sm">
          <thead class="sticky top-0 bg-white dark:bg-night-900">
            <tr class="border-b border-ink-200 dark:border-night-700">
              <th :class="classeTh" scope="col">Lote</th>
              <th :class="classeTh" scope="col">Arquivo</th>
              <th :class="classeTh" scope="col">Enviado em</th>
              <th :class="classeTh" scope="col">Status</th>
              <th :class="[classeTh, 'text-right']" scope="col">Recebidas</th>
              <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Válidas</th>
            </tr>
          </thead>
          <tbody>
            <tr
              :class="[classeLinha, lote.id === selecionadoId ? 'bg-brand-500/10' : '']"
              v-for="lote in historico"
              :key="lote.id"
              :aria-current="lote.id === selecionadoId ? 'true' : undefined"
            >
              <td class="py-2 pr-4 tabular-nums text-ink-600 dark:text-mist-300">{{ lote.id }}</td>
              <td class="py-2 pr-4">
                <button
                  class="text-left font-medium text-brand-800 underline-offset-2 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:text-brand-300"
                  type="button"
                  @click="selecionar(lote.id)"
                >
                  {{ lote.arquivoNome }}
                </button>
              </td>
              <td class="py-2 pr-4 text-ink-600 dark:text-mist-300">{{ dataHora(lote.enviadoEm) }}</td>
              <td class="py-2 pr-4"><StatusBadge :status="lote.status" /></td>
              <td class="py-2 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ inteiro(lote.totalLinhas) }}</td>
              <td class="py-2 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ inteiro(lote.linhasValidas) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- ===================================================================
         Relatório do lote selecionado (por padrão, o mais recente).
         =================================================================== -->
    <section class="mt-10">
      <p class="text-sm text-ink-600 dark:text-mist-300" v-if="estadoDetalhe === 'carregando'" role="status">
        Carregando o lote…
      </p>
      <p
        class="rounded border border-erro-600 bg-erro-600/5 p-4 text-sm text-ink-900 dark:border-erro-400 dark:bg-erro-400/10 dark:text-mist-100"
        v-else-if="estadoDetalhe === 'erro'"
        role="alert"
      >
        Não foi possível abrir este lote: {{ erro }}
      </p>

      <template v-else>
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div class="min-w-0">
            <h2 class="text-lg font-semibold text-ink-900 dark:text-mist-100">
              {{ posicaoSelecionada === 0 ? 'Último lote enviado' : 'Lote selecionado' }} · {{ detalhe.lote.id }}
            </h2>
            <p class="mt-1 max-w-2xl text-sm text-ink-600 dark:text-mist-300">
              Só as linhas válidas são processadas; os indicadores abaixo foram calculados a partir delas.
            </p>
          </div>

          <!-- Exportam o lote aberto aqui, seja o mais recente ou um antigo. -->
          <div class="flex flex-wrap gap-2 print:hidden" role="group" aria-label="Exportar este relatório" v-if="detalhe.est">
            <button :class="classeBotao" type="button" title="Indicadores por segmento" @click="exportarCsv">
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
            <dd class="mt-1 truncate text-sm font-semibold text-ink-900 dark:text-mist-100" :title="detalhe.lote.arquivoNome">
              {{ detalhe.lote.arquivoNome }}
            </dd>
          </div>
          <div class="bg-white px-4 py-3 dark:bg-night-900">
            <dt class="text-xs text-ink-600 dark:text-mist-300">Enviado em</dt>
            <dd class="mt-1 text-sm font-semibold tabular-nums text-ink-900 dark:text-mist-100">{{ dataHora(detalhe.lote.enviadoEm) }}</dd>
          </div>
          <div class="bg-white px-4 py-3 dark:bg-night-900">
            <dt class="text-xs text-ink-600 dark:text-mist-300">Enviado por</dt>
            <dd class="mt-1 truncate text-sm font-semibold text-ink-900 dark:text-mist-100" :title="detalhe.lote.enviadoPor">
              {{ detalhe.lote.enviadoPor }}
            </dd>
          </div>
          <div class="bg-white px-4 py-3 dark:bg-night-900">
            <dt class="text-xs text-ink-600 dark:text-mist-300">Status</dt>
            <dd class="mt-1"><StatusBadge :status="detalhe.lote.status" /></dd>
          </div>
          <div class="bg-white px-4 py-3 dark:bg-night-900">
            <dt class="text-xs text-ink-600 dark:text-mist-300">Linhas recebidas</dt>
            <dd class="mt-1 text-sm font-semibold tabular-nums text-ink-900 dark:text-mist-100">{{ inteiro(detalhe.lote.totalLinhas) }}</dd>
          </div>
          <div class="bg-white px-4 py-3 dark:bg-night-900">
            <dt class="text-xs text-ink-600 dark:text-mist-300">Válidas</dt>
            <dd class="mt-1 text-sm font-semibold tabular-nums text-ok-600 dark:text-ok-400">{{ inteiro(detalhe.lote.linhasValidas) }}</dd>
          </div>
          <div class="bg-white px-4 py-3 dark:bg-night-900">
            <dt class="text-xs text-ink-600 dark:text-mist-300">Inválidas</dt>
            <dd
              class="mt-1 text-sm font-semibold tabular-nums"
              :class="detalhe.lote.linhasInvalidas ? 'text-erro-600 dark:text-erro-400' : 'text-ink-900 dark:text-mist-100'"
            >
              {{ inteiro(detalhe.lote.linhasInvalidas) }}
            </dd>
          </div>
          <div class="bg-white px-4 py-3 dark:bg-night-900">
            <dt class="text-xs text-ink-600 dark:text-mist-300">Aproveitamento</dt>
            <dd class="mt-1 text-sm font-semibold tabular-nums text-ink-900 dark:text-mist-100">
              {{ porcento(detalhe.lote.totalLinhas ? (detalhe.lote.linhasValidas / detalhe.lote.totalLinhas) * 100 : 0) }}
            </dd>
          </div>
        </dl>

        <!-- Lote sem resultado: a mensagem do upload_lote diz o porquê. -->
        <p
          class="mt-4 rounded border px-4 py-3 text-sm text-ink-900 dark:text-mist-100"
          :class="detalhe.lote.status === 'ERRO' || detalhe.lote.status === 'REJEITADO'
            ? 'border-erro-600 bg-erro-600/5 dark:border-erro-400 dark:bg-erro-400/10'
            : 'border-brand-500/40 bg-brand-500/10'"
          v-if="!detalhe.est"
          role="status"
        >
          {{ explicarLote(detalhe.lote) }}
        </p>

        <template v-else>
          <div class="mt-4 grid gap-4 lg:grid-cols-2">
            <div :class="classeCartao">
              <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Qualidade do lote</h3>
              <table class="mt-3 w-full text-left text-sm">
                <thead>
                  <tr class="border-b border-ink-200 dark:border-night-700">
                    <th :class="classeTh" scope="col">Verificação</th>
                    <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Quantidade</th>
                  </tr>
                </thead>
                <tbody>
                  <tr :class="classeLinha" v-for="item in quadroQualidade" :key="item.tipo">
                    <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
                    <td class="py-2 text-right font-semibold tabular-nums" :class="quantidadeCor(item)">{{ inteiro(item.quantidade) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div :class="classeCartao">
              <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Padronizações no processamento</h3>
              <!-- Zero é o normal: o Pinia e o Spring já entregam as linhas padronizadas. -->
              <p class="mt-3 text-sm text-ink-600 dark:text-mist-300" v-if="!houvePadronizacao">
                Nenhuma correção foi necessária: as linhas já chegaram padronizadas pela validação da planilha.
                O processamento confere de novo e só corrige o que escapar.
              </p>
              <table class="mt-3 w-full text-left text-sm" v-else>
                <thead>
                  <tr class="border-b border-ink-200 dark:border-night-700">
                    <th :class="classeTh" scope="col">Padronização</th>
                    <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Quantidade</th>
                  </tr>
                </thead>
                <tbody>
                  <tr :class="classeLinha" v-for="item in padronizacoes" :key="item.tipo">
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
              <p class="mt-3 text-xs text-ink-600 dark:text-mist-300" v-if="segmentosCorrigidos.length">
                <template v-for="(s, i) in segmentosCorrigidos" :key="`${s.original}-${s.padronizado}`">
                  {{ i ? ' · ' : '' }}“{{ s.original }}” → {{ s.padronizado }} ({{ inteiro(s.ocorrencias) }})
                </template>
              </p>
            </div>
          </div>

          <div class="mt-4" :class="classeCartao" v-if="insights.length">
            <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Insights gerados neste lote</h3>
            <ul class="mt-3 list-disc space-y-1 pl-5 text-sm leading-relaxed text-ink-900 dark:text-mist-100">
              <li v-for="frase in insights" :key="frase">{{ frase }}</li>
            </ul>
          </div>

          <!-- Comparação com o lote processado imediatamente anterior ao selecionado. -->
          <div class="mt-10" v-if="detalhe.anterior">
            <h2 class="text-lg font-semibold text-ink-900 dark:text-mist-100">Comparação com o lote anterior</h2>
            <p class="mt-1 text-sm text-ink-600 dark:text-mist-300">
              Lote {{ detalhe.anterior.id }} · {{ detalhe.anterior.arquivoNome }}, enviado em {{ dataHora(detalhe.anterior.enviadoEm) }}.
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
                      <th :class="[classeTh, 'text-right']" scope="col">Este lote</th>
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
                      <th :class="[classeTh, 'text-right']" scope="col">Este lote</th>
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
            Não há lote processado antes deste, então não há o que comparar.
          </p>
        </template>
      </template>
    </section>

    <!-- ===================================================================
         Evolução: os últimos lotes processados, do mais antigo ao mais novo.
         =================================================================== -->
    <section class="mt-10" v-if="evolucao.length > 1">
      <h2 class="text-lg font-semibold text-ink-900 dark:text-mist-100">Evolução entre lotes</h2>
      <p class="mt-1 text-sm text-ink-600 dark:text-mist-300">
        Os últimos {{ evolucao.length }} lotes processados, do mais antigo para o mais recente.
      </p>

      <div class="mt-4 grid gap-4 lg:grid-cols-2">
        <CartaoGrafico titulo="Faturamento total por lote" descricao="Soma do faturamento anual dos clientes de cada lote.">
          <GraficoArea :pontos="evolucao" campo="faturamento" formato="moeda" unidade="" descricao="Faturamento total por lote." />
          <template #tabela>
            <table class="w-full text-left text-sm">
              <thead>
                <tr class="border-b border-ink-200 dark:border-night-700">
                  <th :class="classeTh" scope="col">Lote</th>
                  <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Faturamento</th>
                </tr>
              </thead>
              <tbody>
                <tr :class="classeLinha" v-for="ponto in evolucao" :key="ponto.id">
                  <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ ponto.id }} · {{ ponto.arquivo }} · {{ dataHora(ponto.data) }}</td>
                  <td class="py-2 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ dinheiro(ponto.faturamento) }}</td>
                </tr>
              </tbody>
            </table>
          </template>
        </CartaoGrafico>

        <CartaoGrafico titulo="Clientes gravados por lote" descricao="Clientes gravados em cada lote.">
          <GraficoArea :pontos="evolucao" campo="clientes" unidade="clientes" descricao="Clientes gravados por lote." />
          <template #tabela>
            <table class="w-full text-left text-sm">
              <thead>
                <tr class="border-b border-ink-200 dark:border-night-700">
                  <th :class="classeTh" scope="col">Lote</th>
                  <th :class="[classeTh, 'text-right']" scope="col">Clientes</th>
                  <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Aproveitamento</th>
                </tr>
              </thead>
              <tbody>
                <tr :class="classeLinha" v-for="ponto in evolucao" :key="ponto.id">
                  <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ ponto.id }} · {{ ponto.arquivo }} · {{ dataHora(ponto.data) }}</td>
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
