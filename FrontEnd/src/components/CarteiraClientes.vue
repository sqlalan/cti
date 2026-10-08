<script setup>
import { computed, ref } from 'vue'
import CartaoGrafico from './graficos/CartaoGrafico.vue'
import GraficoBarras from './graficos/GraficoBarras.vue'
import { NIVEIS } from '../dados/esquemaPlanilha'
import { baixarCsv, hoje } from '../dados/exportar'
import { CORES_NIVEL } from '../dados/painel'

// A carteira gravada no banco (GET /api/clientes), recebida por props. Os
// filtros são estado local: só esta seção reage a eles.
const props = defineProps({
  clientes: { type: Array, required: true },
})

const FAIXA = { MICRO: 'Micro', PEQUENA: 'Pequena', MEDIA: 'Média', GRANDE: 'Grande' }

// faturamentoAnual chega como número do BigDecimal; Number() cobre o caso de
// vir como texto.
const carteira = computed(() =>
  props.clientes.map((c) => ({
    codigo: c.codigo,
    nome: c.nome,
    consultor: c.consultor,
    segmento: c.segmento,
    nivel: c.nivel,
    faturamento: Number(c.faturamentoAnual ?? 0),
    faixa: FAIXA[c.faixaFaturamento] ?? c.faixaFaturamento ?? '',
    cidade: c.cidade ?? '',
    uf: c.uf ?? '',
  })),
)

const filtroSegmento = ref('todos')
const filtroNivel = ref('todos')
const filtroConsultor = ref('todos')
const busca = ref('')

const opcoes = (chave) =>
  computed(() => [...new Set(carteira.value.map((r) => r[chave]))].filter(Boolean).sort())

const segmentos = opcoes('segmento')
const consultores = opcoes('consultor')

const filtrados = computed(() => {
  const termo = busca.value.trim().toLowerCase()
  return carteira.value.filter((registro) => {
    if (filtroSegmento.value !== 'todos' && registro.segmento !== filtroSegmento.value) return false
    if (filtroNivel.value !== 'todos' && registro.nivel !== filtroNivel.value) return false
    if (filtroConsultor.value !== 'todos' && registro.consultor !== filtroConsultor.value) return false
    if (!termo) return true
    return (
      registro.nome.toLowerCase().includes(termo) ||
      registro.codigo.toLowerCase().includes(termo) ||
      registro.cidade.toLowerCase().includes(termo)
    )
  })
})

const temFiltro = computed(
  () =>
    filtroSegmento.value !== 'todos' ||
    filtroNivel.value !== 'todos' ||
    filtroConsultor.value !== 'todos' ||
    busca.value.trim() !== '',
)

function limparFiltros() {
  filtroSegmento.value = 'todos'
  filtroNivel.value = 'todos'
  filtroConsultor.value = 'todos'
  busca.value = ''
}

// Somar float acumula resíduo; arredonda antes de qualquer comparação.
const centavos = (n) => Math.round(n * 100) / 100
const somar = (lista) => centavos(lista.reduce((total, r) => total + r.faturamento, 0))

const faturamento = computed(() => somar(filtrados.value))
const ticketMedio = computed(() =>
  filtrados.value.length ? centavos(faturamento.value / filtrados.value.length) : 0,
)

function agrupar(chave) {
  const grupos = new Map()
  for (const registro of filtrados.value) {
    const nome = registro[chave] || 'Não informada'
    const atual = grupos.get(nome) ?? { rotulo: nome, clientes: 0, faturamento: 0 }
    atual.clientes += 1
    atual.faturamento += registro.faturamento
    grupos.set(nome, atual)
  }
  return [...grupos.values()]
    .map((g) => ({ ...g, faturamento: centavos(g.faturamento) }))
    .sort((a, b) => b.faturamento - a.faturamento)
}

// Nível é escala ordinal: a listagem segue A → B → C, não o tamanho da barra.
const porNivel = computed(() =>
  NIVEIS.map((nivel) => {
    const doNivel = filtrados.value.filter((r) => r.nivel === nivel)
    return { rotulo: nivel, clientes: doNivel.length, faturamento: somar(doNivel) }
  }),
)

// Os quatro recortes têm a mesma forma: um template só desenha todos.
const graficos = computed(() => [
  { titulo: 'Faturamento por segmento', descricao: 'Soma anual de cada segmento.', coluna: 'Segmento', itens: agrupar('segmento') },
  { titulo: 'Faturamento por consultor', descricao: 'Soma anual de cada carteira.', coluna: 'Consultor', itens: agrupar('consultor') },
  {
    titulo: 'Faturamento por nível',
    descricao: 'A faixa é ordenada: o tom escurece conforme o porte do cliente.',
    coluna: 'Nível',
    itens: porNivel.value,
    cores: CORES_NIVEL,
  },
  { titulo: 'Faturamento por cidade', descricao: 'Soma anual por praça atendida.', coluna: 'Cidade', itens: agrupar('cidade') },
])

const ordenados = computed(() => [...filtrados.value].sort((a, b) => b.faturamento - a.faturamento))

const inteiro = (valor) => valor.toLocaleString('pt-BR')
const dinheiro = (valor) => valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })

// ---------------------------------------------------------------------------
// Exportação
// ---------------------------------------------------------------------------

const COLUNAS_CSV = [
  ['codigo_cliente', (r) => r.codigo],
  ['nome_cliente', (r) => r.nome],
  ['consultor', (r) => r.consultor],
  ['segmento', (r) => r.segmento],
  ['nivel_cliente', (r) => r.nivel],
  ['faturamento_anual', (r) => String(r.faturamento)],
  ['porte', (r) => r.faixa],
  ['cidade', (r) => r.cidade],
  ['uf', (r) => r.uf],
]

function exportarCsv() {
  baixarCsv(`carteira-clientes-${hoje()}.csv`, { colunas: COLUNAS_CSV, linhas: ordenados.value })
}

const classeCampo =
  'rounded border border-ink-200 bg-white px-3 py-1.5 text-sm text-ink-900 focus:border-brand-500 focus:outline-2 focus:outline-offset-0 focus:outline-brand-500 dark:border-night-700 dark:bg-night-900 dark:text-mist-100'
</script>

<template>
  <section class="mt-8">
    <h2 class="text-lg font-semibold text-ink-900 dark:text-mist-100">Faturamento da carteira</h2>
    <p class="mt-1 text-sm text-ink-600 dark:text-mist-300">
      Clientes gravados no banco. Os filtros valem para os gráficos e a tabela desta seção.
    </p>

    <!-- Uma linha de filtros, acima de tudo que ela governa: gráficos e tabela
         reagem à mesma fatia, então os números nunca se contradizem. -->
    <div class="mt-4 flex flex-wrap items-end gap-3">
      <label class="flex flex-col gap-1">
        <span class="text-xs font-medium text-ink-600 dark:text-mist-300">Buscar</span>
        <input
          class="w-52 placeholder:text-ink-400 dark:placeholder:text-mist-500"
          :class="classeCampo"
          v-model="busca"
          type="search"
          placeholder="Cliente, código ou cidade"
        />
      </label>

      <label class="flex flex-col gap-1">
        <span class="text-xs font-medium text-ink-600 dark:text-mist-300">Segmento</span>
        <select :class="classeCampo" v-model="filtroSegmento">
          <option value="todos">Todos</option>
          <option v-for="item in segmentos" :key="item" :value="item">{{ item }}</option>
        </select>
      </label>

      <label class="flex flex-col gap-1">
        <span class="text-xs font-medium text-ink-600 dark:text-mist-300">Nível</span>
        <select :class="classeCampo" v-model="filtroNivel">
          <option value="todos">Todos</option>
          <option v-for="nivel in NIVEIS" :key="nivel" :value="nivel">{{ nivel }}</option>
        </select>
      </label>

      <label class="flex flex-col gap-1">
        <span class="text-xs font-medium text-ink-600 dark:text-mist-300">Consultor</span>
        <select :class="classeCampo" v-model="filtroConsultor">
          <option value="todos">Todos</option>
          <option v-for="item in consultores" :key="item" :value="item">{{ item }}</option>
        </select>
      </label>

      <button
        class="rounded px-3 py-1.5 text-sm font-medium text-ink-600 transition-colors hover:text-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:text-mist-300 dark:hover:text-brand-300"
        v-if="temFiltro"
        type="button"
        @click="limparFiltros"
      >
        Limpar filtros
      </button>

      <button
        class="ml-auto rounded border print:hidden border-ink-200 px-4 py-1.5 text-sm font-medium text-brand-800 transition-colors hover:border-brand-500/50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 disabled:cursor-not-allowed disabled:opacity-50 dark:border-night-700 dark:text-brand-300"
        type="button"
        :disabled="!filtrados.length"
        @click="exportarCsv"
      >
        Exportar CSV
      </button>
    </div>

    <p class="mt-3 text-sm text-ink-600 dark:text-mist-300" role="status">
      {{ inteiro(filtrados.length) }} {{ filtrados.length === 1 ? 'cliente' : 'clientes' }}
      <template v-if="temFiltro">de {{ inteiro(carteira.length) }}</template>
      · faturamento {{ dinheiro(faturamento) }} · ticket médio {{ dinheiro(ticketMedio) }}
    </p>

    <div class="mt-4 grid gap-4 lg:grid-cols-2">
      <CartaoGrafico v-for="grafico in graficos" :key="grafico.titulo" :titulo="grafico.titulo" :descricao="grafico.descricao">
        <GraficoBarras :itens="grafico.itens" campo="faturamento" formato="moeda" :cores="grafico.cores" />

        <template #tabela>
          <table class="w-full text-left text-sm">
            <thead>
              <tr class="border-b border-ink-200 dark:border-night-700">
                <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">{{ grafico.coluna }}</th>
                <th class="py-2 pr-4 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Clientes</th>
                <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Faturamento</th>
              </tr>
            </thead>
            <tbody>
              <tr class="border-b border-ink-200 last:border-0 dark:border-night-700" v-for="item in grafico.itens" :key="item.rotulo">
                <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
                <td class="py-2 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ inteiro(item.clientes) }}</td>
                <td class="py-2 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ dinheiro(item.faturamento) }}</td>
              </tr>
            </tbody>
          </table>
        </template>
      </CartaoGrafico>
    </div>

    <section class="mt-4 min-w-0 rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
      <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">
        Carteira ({{ inteiro(ordenados.length) }})
      </h3>
      <p class="mt-1 text-xs text-ink-600 dark:text-mist-300">
        Ordenada por faturamento anual, do maior para o menor.
      </p>

      <div class="mt-4 max-h-[32rem] overflow-auto">
        <table class="w-full min-w-3xl text-left text-sm">
          <thead class="sticky top-0 bg-white dark:bg-night-900">
            <tr class="border-b border-ink-200 dark:border-night-700">
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Código</th>
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Cliente</th>
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Consultor</th>
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Segmento</th>
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Nível</th>
              <th class="py-2 pr-4 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Faturamento</th>
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Porte</th>
              <th class="py-2 font-medium text-ink-600 dark:text-mist-300" scope="col">Cidade</th>
            </tr>
          </thead>
          <tbody>
            <tr class="border-b border-ink-200 last:border-0 dark:border-night-700" v-for="registro in ordenados" :key="registro.codigo">
              <td class="py-2 pr-4 tabular-nums text-ink-600 dark:text-mist-300">{{ registro.codigo }}</td>
              <td class="py-2 pr-4 whitespace-nowrap text-ink-900 dark:text-mist-100">{{ registro.nome }}</td>
              <td class="py-2 pr-4 whitespace-nowrap text-ink-900 dark:text-mist-100">{{ registro.consultor }}</td>
              <td class="py-2 pr-4 whitespace-nowrap text-ink-900 dark:text-mist-100">{{ registro.segmento }}</td>
              <td class="py-2 pr-4">
                <span class="flex items-center gap-2">
                  <span class="h-2.5 w-2.5 shrink-0 rounded-sm" :class="CORES_NIVEL[registro.nivel]" aria-hidden="true"></span>
                  <span class="font-semibold text-ink-900 dark:text-mist-100">{{ registro.nivel }}</span>
                </span>
              </td>
              <td class="py-2 pr-4 text-right whitespace-nowrap tabular-nums text-ink-900 dark:text-mist-100">
                {{ dinheiro(registro.faturamento) }}
              </td>
              <td class="py-2 pr-4 whitespace-nowrap text-ink-600 dark:text-mist-300">{{ registro.faixa }}</td>
              <td class="py-2 whitespace-nowrap text-ink-900 dark:text-mist-100">
                {{ registro.cidade }}<template v-if="registro.uf">/{{ registro.uf }}</template>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p class="mt-4 text-sm text-ink-600 dark:text-mist-300" v-if="!ordenados.length" role="status">
        Nenhum cliente atende aos filtros selecionados.
      </p>
    </section>
  </section>
</template>
