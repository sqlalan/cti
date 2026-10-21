<script setup>
// DashboardView.vue — gráficos com os dados que o Python calculou e o banco Azure guardou.
import { computed, onBeforeUnmount, onMounted, reactive } from 'vue' // reactive: objeto reativo só desta tela
import { dashboardService, mensagemDeErro } from '../services/api' // chamada ao backend (sem store: só esta tela usa)
import { useUploadStore } from '../stores/uploadStore' // para abrir o lote recém-enviado
import BaseChart from '../components/BaseChart.vue' // desenha gráficos
import KpiCard from '../components/KpiCard.vue' // cartão de número
import StatusBadge from '../components/StatusBadge.vue' // etiqueta de status

const moeda = (n) => (n == null ? '—' : Number(n).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })) // 1500 -> R$ 1.500,00
const numero = (n) => (n == null ? '—' : Number(n).toLocaleString('pt-BR', { maximumFractionDigits: 2 })) // 1234.5 -> 1.234,5

const dash = reactive({ // dados só desta tela (por isso não precisa de Pinia)
  lote: null, // o lote mostrado (status, totais)
  estatisticas: null, // JSON do Python (null enquanto o ETL não terminou)
  erro: '', // mensagem de erro
  carregando: false, // true durante a busca
  async carregar(loteId) { // GET /api/dashboard
    this.carregando = true; this.erro = '' // começa
    try {
      const resp = await dashboardService.obter(loteId) // pede ao Spring
      this.lote = resp.lote; this.estatisticas = resp.estatisticas // guarda o lote e o JSON (ou null)
    } catch (e) {
      this.lote = null; this.estatisticas = null; this.erro = mensagemDeErro(e) // 404 = ainda não há lote processado
    } finally {
      this.carregando = false
    }
  },
})
const upload = useUploadStore() // acesso ao upload
const d = computed(() => dash.estatisticas) // atalho: d.value.resumo, d.value.porSegmento...
let relogio = null // guarda o timer de atualização

// Cada gráfico lê uma lista que o Python gravou no JSON e separa em "nomes" e "números".
const seg = computed(() => d.value?.porSegmento ?? []) // clientes por segmento
const niv = computed(() => d.value?.porNivel ?? []) // clientes por nível
const con = computed(() => d.value?.porConsultor ?? []) // faturamento por consultor
const ser = computed(() => d.value?.porServico ?? []) // serviços mais contratados
const mes = computed(() => d.value?.porMes ?? []) // contratações por mês

async function buscar() { // pede os dados ao Spring
  await dash.carregar(upload.lote?.id) // se acabou de enviar, usa esse lote; senão o último processado
  const esperando = ['AGUARDANDO_ETL', 'PROCESSANDO'].includes(dash.lote?.status) // o Python ainda não terminou (ERRO não espera)
  if (esperando && !relogio) relogio = setInterval(buscar, 5000) // pergunta de novo a cada 5 s
  if (!esperando && relogio) { clearInterval(relogio); relogio = null } // terminou: para o timer
}

onMounted(buscar) // busca ao abrir a tela
onBeforeUnmount(() => relogio && clearInterval(relogio)) // limpa o timer ao sair
</script>

<template>
  <section>
    <h1 class="mb-1 text-2xl font-bold text-ink-900 dark:text-mist-100">Gráficos da carteira</h1>
    <p v-if="dash.lote" class="mb-4 flex flex-wrap items-center gap-2 text-sm text-ink-600 dark:text-mist-300">
      Lote {{ dash.lote.id }} · {{ dash.lote.arquivoNome }} <StatusBadge :status="dash.lote.status" />
    </p>

    <p v-if="dash.erro" class="rounded border border-warn-500 bg-warn-500/10 p-3 text-sm text-ink-900 dark:bg-warn-500/15 dark:text-mist-100">{{ dash.erro }} <RouterLink :to="{ name: 'upload' }" class="font-semibold text-brand-800 underline dark:text-brand-300">Enviar planilha</RouterLink></p>
    <p v-else-if="dash.carregando && !d" class="text-sm text-ink-600 dark:text-mist-300">Carregando...</p>
    <p v-else-if="dash.lote?.status === 'ERRO'" class="rounded border border-erro-600 bg-erro-600/5 p-3 text-sm text-ink-900 dark:border-erro-400 dark:bg-erro-400/10 dark:text-mist-100" role="alert">O processamento deste lote falhou: {{ dash.lote.mensagem }} <RouterLink :to="{ name: 'upload' }" class="font-semibold text-brand-800 underline dark:text-brand-300">Enviar de novo</RouterLink></p>
    <p v-else-if="dash.lote && !d" class="rounded border border-brand-500/40 bg-brand-500/10 p-3 text-sm text-ink-900 dark:text-mist-100" role="status">O Python está processando a planilha. Esta tela atualiza sozinha.</p>

    <template v-if="d">
      <div class="grid grid-cols-2 md:grid-cols-5 gap-3 mb-6"> <!-- cartões de resumo -->
        <KpiCard titulo="Clientes" :valor="String(d.resumo.totalClientes)" />
        <KpiCard titulo="Faturamento total" :valor="moeda(d.resumo.faturamentoTotal)" />
        <KpiCard titulo="Média" :valor="moeda(d.resumo.media)" destaque /> <!-- média calculada pelo Python -->
        <KpiCard titulo="Mediana" :valor="moeda(d.resumo.mediana)" />
        <KpiCard titulo="Desvio padrão" :valor="moeda(d.resumo.desvioPadrao)" />
      </div>

      <div class="grid gap-4 lg:grid-cols-2"> <!-- 1 coluna no celular, 2 no computador -->
        <div class="min-w-0 rounded border border-ink-200 bg-white p-4 shadow-cartao dark:border-night-700 dark:bg-night-900">
          <h2 class="mb-2 font-semibold text-ink-900 dark:text-mist-100">Clientes por segmento</h2>
          <BaseChart tipo="bar" titulo="Clientes" :rotulos="seg.map((x) => x.segmento)" :valores="seg.map((x) => x.clientes)" />
        </div>
        <div class="min-w-0 rounded border border-ink-200 bg-white p-4 shadow-cartao dark:border-night-700 dark:bg-night-900">
          <h2 class="mb-2 font-semibold text-ink-900 dark:text-mist-100">Clientes por nível</h2>
          <BaseChart tipo="doughnut" :rotulos="niv.map((x) => 'Nível ' + x.nivel)" :valores="niv.map((x) => x.clientes)" />
        </div>
        <div class="min-w-0 rounded border border-ink-200 bg-white p-4 shadow-cartao dark:border-night-700 dark:bg-night-900">
          <h2 class="mb-2 font-semibold text-ink-900 dark:text-mist-100">Faturamento por consultor</h2>
          <BaseChart tipo="bar" horizontal titulo="Faturamento" :rotulos="con.map((x) => x.consultor)" :valores="con.map((x) => x.faturamentoTotal)" />
        </div>
        <div class="min-w-0 rounded border border-ink-200 bg-white p-4 shadow-cartao dark:border-night-700 dark:bg-night-900">
          <h2 class="mb-2 font-semibold text-ink-900 dark:text-mist-100">Serviços mais contratados</h2>
          <BaseChart tipo="bar" horizontal titulo="Contratos" :rotulos="ser.map((x) => x.servico)" :valores="ser.map((x) => x.contratos)" />
        </div>
        <div class="min-w-0 rounded border border-ink-200 bg-white p-4 shadow-cartao dark:border-night-700 dark:bg-night-900 lg:col-span-2">
          <h2 class="mb-2 font-semibold text-ink-900 dark:text-mist-100">Contratações por mês</h2>
          <BaseChart tipo="line" titulo="Clientes" :rotulos="mes.map((x) => x.mes)" :valores="mes.map((x) => x.clientes)" />
        </div>
      </div>

      <div v-if="d.insights?.length" class="mt-6 rounded border border-brand-500/40 bg-brand-500/10 p-4"> <!-- frases geradas pelo Python -->
        <h2 class="mb-2 font-semibold text-ink-900 dark:text-mist-100">O que os dados mostram</h2>
        <ul class="list-disc space-y-1 pl-5 text-sm text-ink-900 dark:text-mist-100"><li v-for="i in d.insights" :key="i">{{ i }}</li></ul>
      </div>
      <p class="mt-4 text-xs text-ink-600 dark:text-mist-300">Linhas recebidas: {{ numero(d.qualidade?.linhasRecebidas) }} · válidas: {{ numero(d.qualidade?.linhasValidas) }}</p>
    </template>
  </section>
</template>
