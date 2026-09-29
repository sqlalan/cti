<script setup>
import { computed, ref } from 'vue'
import CartaoGrafico from '../components/graficos/CartaoGrafico.vue'
import GraficoArea from '../components/graficos/GraficoArea.vue'
import GraficoBarras from '../components/graficos/GraficoBarras.vue'
import { NIVEIS } from '../dados/esquemaPlanilha'
import { formatarData } from '../dados/painel'
import { useUploadStore } from '../stores/uploadStore'

// O relatório lê a mesma planilha que o Upload carregou. É o que o store existe
// para resolver: o dado atravessa a navegação sem passar por prop nem por URL.
const planilha = useUploadStore()

const filtroSegmento = ref('todos')
const filtroNivel = ref('todos')
const filtroConsultor = ref('todos')
const busca = ref('')

const opcoes = (chave) =>
  computed(() => [...new Set(planilha.dadosValidos.map((r) => r[chave]))].filter(Boolean).sort())

const segmentos = opcoes('segmento')
const consultores = opcoes('consultor')

const filtrados = computed(() => {
  const termo = busca.value.trim().toLowerCase()
  return planilha.dadosValidos.filter((registro) => {
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
const somar = (lista) => centavos(lista.reduce((total, r) => total + (r.faturamento ?? 0), 0))

const faturamento = computed(() => somar(filtrados.value))
const ticketMedio = computed(() =>
  filtrados.value.length ? centavos(faturamento.value / filtrados.value.length) : 0,
)
const participacaoA = computed(() => {
  if (!filtrados.value.length) return 0
  return (filtrados.value.filter((r) => r.nivel === 'A').length / filtrados.value.length) * 100
})

function agrupar(chave) {
  const grupos = new Map()
  for (const registro of filtrados.value) {
    const nome = registro[chave] || '(em branco)'
    const atual = grupos.get(nome) ?? { rotulo: nome, clientes: 0, faturamento: 0 }
    atual.clientes += 1
    atual.faturamento += registro.faturamento ?? 0
    grupos.set(nome, atual)
  }
  return [...grupos.values()]
    .map((g) => ({ ...g, faturamento: centavos(g.faturamento) }))
    .sort((a, b) => b.faturamento - a.faturamento)
}

const porSegmento = computed(() => agrupar('segmento'))
const porConsultor = computed(() => agrupar('consultor'))
const porCidade = computed(() => agrupar('cidade'))

// Nível é escala ordinal: a ordem A → B → C faz parte do sentido, então a
// listagem segue a ordem da faixa e não o tamanho da barra.
const porNivel = computed(() =>
  NIVEIS.map((nivel) => {
    const doNivel = filtrados.value.filter((r) => r.nivel === nivel)
    return { rotulo: nivel, clientes: doNivel.length, faturamento: somar(doNivel) }
  }),
)

const CORES_NIVEL = {
  A: 'bg-nivel-a-600 dark:bg-nivel-a-400',
  B: 'bg-nivel-b-600 dark:bg-nivel-b-400',
  C: 'bg-nivel-c-600 dark:bg-nivel-c-400',
}

// Contratações agrupadas por mês. Meses sem contrato entram com zero: pular o
// mês vazio encurtaria o eixo e faria a série parecer contínua onde não é.
const porMes = computed(() => {
  const comData = filtrados.value.filter((r) => r.contratacao instanceof Date)
  if (!comData.length) return []

  const chave = (data) => `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}`
  const contagem = new Map()
  for (const registro of comData) {
    const k = chave(registro.contratacao)
    const atual = contagem.get(k) ?? { clientes: 0, faturamento: 0 }
    atual.clientes += 1
    atual.faturamento += registro.faturamento ?? 0
    contagem.set(k, atual)
  }

  const datas = comData.map((r) => r.contratacao)
  const inicio = new Date(Math.min(...datas))
  const fim = new Date(Math.max(...datas))

  const pontos = []
  const cursor = new Date(inicio.getFullYear(), inicio.getMonth(), 1)
  const ultimo = new Date(fim.getFullYear(), fim.getMonth(), 1)
  while (cursor <= ultimo) {
    const k = chave(cursor)
    const achado = contagem.get(k) ?? { clientes: 0, faturamento: 0 }
    pontos.push({
      data: new Date(cursor),
      clientes: achado.clientes,
      faturamento: centavos(achado.faturamento),
    })
    cursor.setMonth(cursor.getMonth() + 1)
  }
  return pontos
})

const ordenados = computed(() => [...filtrados.value].sort((a, b) => b.faturamento - a.faturamento))

const inteiro = (valor) => valor.toLocaleString('pt-BR')

// Cor no quadro de validação: verde só na linha dos válidos, vermelho na de
// erro que realmente aconteceu, cinza no zero. Pintar o zero de vermelho
// alarmaria por uma verificação que passou, e pintar tudo de vermelho apagaria
// a diferença entre "3 duplicados" e "nenhum duplicado".
function quantidadeCor(item) {
  if (item.tipo === 'validos') return 'text-ok-600 dark:text-ok-400'
  if (!item.quantidade) return 'text-ink-400 dark:text-mist-500'
  return 'text-erro-600 dark:text-erro-400'
}
const dinheiro = (valor) =>
  typeof valor === 'number'
    ? valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
    : '—'
const dinheiroCurto = (valor) =>
  typeof valor === 'number'
    ? valor.toLocaleString('pt-BR', {
        style: 'currency',
        currency: 'BRL',
        notation: 'compact',
        maximumFractionDigits: 1,
      })
    : '—'
const dataCurta = (valor) => (valor instanceof Date ? valor.toLocaleDateString('pt-BR') : '—')
// Mesmo formato do eixo do gráfico, para a tabela não discordar dele.
const mesAno = (data) => formatarData(data, 'mes')

// ---------------------------------------------------------------------------
// Exportação
// ---------------------------------------------------------------------------

const COLUNAS_CSV = [
  ['codigo_cliente', (r) => r.codigo],
  ['nome_cliente', (r) => r.nome],
  ['consultor', (r) => r.consultor],
  ['segmento', (r) => r.segmento],
  ['nivel_cliente', (r) => r.nivel],
  ['faturamento_anual', (r) => String(r.faturamento ?? '')],
  ['servicos_contratados', (r) => (r.servicos ?? []).join(';')],
  ['data_contratacao', (r) => dataCurta(r.contratacao)],
  ['cidade', (r) => r.cidade],
  ['uf', (r) => r.uf],
  // Sai vazia quando a planilha não trouxe a coluna. Vale exportar mesmo assim:
  // omitir a coluna faria a exportação descartar em silêncio um dado que o
  // registro carrega, e é justamente o e-mail que quem recebe o arquivo usa
  // para contatar o cliente.
  ['email', (r) => r.email],
]

// Célula que começa com = + - @ é executada como fórmula ao abrir o CSV no
// Excel. Como o conteúdo veio de uma planilha enviada por terceiros, prefixar
// com apóstrofo neutraliza a injeção sem alterar o que a pessoa lê.
function protegerCelula(texto) {
  const valor = String(texto ?? '')
  const perigosa = /^[=+\-@\t\r]/.test(valor)
  const escapada = (perigosa ? `'${valor}` : valor).replace(/"/g, '""')
  return `"${escapada}"`
}

function exportarCsv() {
  const linhas = [
    COLUNAS_CSV.map(([titulo]) => protegerCelula(titulo)).join(';'),
    ...ordenados.value.map((registro) =>
      COLUNAS_CSV.map(([, obter]) => protegerCelula(obter(registro))).join(';'),
    ),
  ]

  // BOM na frente para o Excel abrir em UTF-8; sem ele os acentos quebram.
  const conteudo = new Blob(['﻿' + linhas.join('\r\n')], {
    type: 'text/csv;charset=utf-8;',
  })

  const url = URL.createObjectURL(conteudo)
  const ancora = document.createElement('a')
  ancora.href = url
  ancora.download = `relatorio-clientes-${new Date().toISOString().slice(0, 10)}.csv`

  // A âncora precisa estar no documento para o clique valer em alguns
  // navegadores, e a URL do blob só pode ser revogada depois que o download
  // começou — revogar na mesma volta do laço cancela o arquivo pela metade.
  document.body.appendChild(ancora)
  ancora.click()
  ancora.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
</script>

<template>
  <h1 class="text-2xl font-bold text-ink-900 dark:text-mist-100">Relatórios</h1>
  <p class="mt-2 text-sm text-ink-600 dark:text-mist-300">
    Análise da carteira a partir da planilha carregada.
  </p>

  <!-- =====================================================================
       Relatório da validação.

       Vem antes do painel de negócio e fora do ramo do temDados de propósito:
       é guardado por quantidadeLinhas, não por linhas VÁLIDAS. Uma planilha em
       que tudo falhou tem zero dado para analisar e, ainda assim, é justamente
       quando o relatório de erros mais importa — pendurá-lo no temDados o faria
       desaparecer no pior caso.
       ===================================================================== -->
  <section class="mt-8" v-if="planilha.quantidadeLinhas > 0">
    <h2 class="text-lg font-semibold text-ink-900 dark:text-mist-100">Validação da planilha</h2>

    <dl
      class="mt-4 grid grid-cols-2 gap-px overflow-hidden rounded border border-ink-200 bg-ink-200 sm:grid-cols-4 dark:border-night-700 dark:bg-night-700"
    >
      <div class="bg-white px-4 py-3 dark:bg-night-900">
        <dt class="text-xs text-ink-600 dark:text-mist-300">Arquivo analisado</dt>
        <dd
          class="mt-1 truncate text-sm font-semibold text-ink-900 dark:text-mist-100"
          :title="planilha.nomeArquivo"
        >
          {{ planilha.nomeArquivo || '—' }}
        </dd>
      </div>
      <div class="bg-white px-4 py-3 dark:bg-night-900">
        <dt class="text-xs text-ink-600 dark:text-mist-300">Total de registros</dt>
        <dd class="mt-1 text-sm font-semibold tabular-nums text-ink-900 dark:text-mist-100">
          {{ inteiro(planilha.quantidadeLinhas) }}
        </dd>
      </div>
      <div class="bg-white px-4 py-3 dark:bg-night-900">
        <dt class="text-xs text-ink-600 dark:text-mist-300">Registros válidos</dt>
        <dd class="mt-1 text-sm font-semibold tabular-nums text-ok-600 dark:text-ok-400">
          {{ inteiro(planilha.quantidadeValidas) }}
        </dd>
      </div>
      <div class="bg-white px-4 py-3 dark:bg-night-900">
        <dt class="text-xs text-ink-600 dark:text-mist-300">Registros com erro</dt>
        <dd
          class="mt-1 text-sm font-semibold tabular-nums"
          :class="planilha.quantidadeInvalidas
            ? 'text-erro-600 dark:text-erro-400'
            : 'text-ink-900 dark:text-mist-100'"
        >
          {{ inteiro(planilha.quantidadeInvalidas) }}
        </dd>
      </div>
    </dl>

    <div class="mt-4 grid gap-4 lg:grid-cols-2">
      <!-- O quadro "Validação realizada / Quantidade". -->
      <div class="rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
        <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Validação realizada</h3>
        <table class="mt-3 w-full text-left text-sm">
          <thead>
            <tr class="border-b border-ink-200 dark:border-night-700">
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">
                Verificação
              </th>
              <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                Quantidade
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              class="border-b border-ink-200 last:border-0 dark:border-night-700"
              v-for="item in planilha.quadroValidacao"
              :key="item.tipo"
            >
              <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
              <td
                class="py-2 text-right font-semibold tabular-nums"
                :class="quantidadeCor(item)"
              >
                {{ inteiro(item.quantidade) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Correções que o tratamento aplicou sozinho. Separado dos erros
           porque não reprovam linha nenhuma: misturar os dois quadros somaria
           problema resolvido com problema pendente. -->
      <div class="rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
        <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">
          Tratamentos aplicados
        </h3>
        <p class="mt-1 text-xs text-ink-600 dark:text-mist-300">
          A linha passou; o valor foi ajustado. Corrija na origem para a planilha
          não depender do tratamento. Célula vazia não entra aqui — quando a coluna
          é obrigatória, ela reprova a linha e é contada ao lado, em
          <span class="font-medium">Campos obrigatórios vazios</span>.
        </p>
        <table class="mt-3 w-full text-left text-sm">
          <thead>
            <tr class="border-b border-ink-200 dark:border-night-700">
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">
                Tratamento
              </th>
              <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                Quantidade
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              class="border-b border-ink-200 last:border-0 dark:border-night-700"
              v-for="item in planilha.avisosPorTipo"
              :key="item.tipo"
            >
              <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
              <td
                class="py-2 text-right font-semibold tabular-nums"
                :class="item.quantidade
                  ? 'text-alerta-600 dark:text-alerta-400'
                  : 'text-ink-400 dark:text-mist-500'"
              >
                {{ inteiro(item.quantidade) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Uma linha por problema: linha, campo e descrição, como o relatório
         pede. Um registro com dois defeitos aparece duas vezes aqui e uma vez
         só na contagem de "registros com erro". -->
    <div
      class="mt-4 min-w-0 rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900"
      v-if="planilha.totalOcorrencias"
    >
      <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">
        Problemas encontrados ({{ inteiro(planilha.totalOcorrencias) }})
      </h3>
      <p class="mt-1 text-xs text-ink-600 dark:text-mist-300">
        Em {{ inteiro(planilha.quantidadeInvalidas) }}
        {{ planilha.quantidadeInvalidas === 1 ? 'registro' : 'registros' }}.
      </p>

      <div class="mt-3 max-h-96 overflow-auto">
        <table class="w-full min-w-2xl text-left text-sm">
          <thead class="sticky top-0 bg-white dark:bg-night-900">
            <tr class="border-b border-ink-200 dark:border-night-700">
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Linha</th>
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Campo</th>
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Tipo</th>
              <th class="py-2 font-medium text-ink-600 dark:text-mist-300" scope="col">
                Descrição do erro
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              class="border-b border-ink-200 last:border-0 dark:border-night-700"
              v-for="(item, i) in planilha.ocorrencias"
              :key="`${item.linha}-${item.campo}-${i}`"
            >
              <td class="py-2 pr-4 tabular-nums text-ink-400 dark:text-mist-500">
                {{ item.linha }}
              </td>
              <td class="py-2 pr-4 whitespace-nowrap font-medium text-ink-900 dark:text-mist-100">
                {{ item.campo }}
              </td>
              <td class="py-2 pr-4 whitespace-nowrap text-ink-600 dark:text-mist-300">
                {{ item.rotulo }}
              </td>
              <td class="py-2 text-erro-600 dark:text-erro-400">{{ item.descricao }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <p
      class="mt-4 rounded border border-ok-600 bg-ok-600/10 px-4 py-3 text-sm text-ink-900 dark:border-ok-400 dark:bg-ok-400/15 dark:text-mist-100"
      role="status"
      v-else
    >
      Nenhum problema encontrado: as {{ inteiro(planilha.quantidadeLinhas) }} linhas passaram em
      todas as validações.
    </p>
  </section>

  <!-- Sem planilha não há painel. Em vez de mostrar cartões zerados, que
       parecem defeito, a tela diz o que falta e leva até lá. -->
  <section
    class="mt-8 flex flex-col items-center rounded border border-ink-200 bg-white px-6 py-14 text-center dark:border-night-700 dark:bg-night-900"
    v-if="!planilha.temDados"
  >
    <svg
      class="h-10 w-10 text-ink-400 dark:text-mist-500"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="1.75"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8Z" />
      <path d="M14 3v5h5" />
      <path d="M9 13h6" />
      <path d="M9 17h4" />
    </svg>
    <!-- Dois motivos diferentes para não haver painel, e dizer o motivo errado
         manda a pessoa procurar no lugar errado: sem arquivo ela precisa
         carregar um; com arquivo todo reprovado ela precisa corrigir a planilha
         — e a validação acima já lhe diz o quê. -->
    <template v-if="planilha.quantidadeLinhas > 0">
      <p class="mt-4 text-sm font-medium text-ink-900 dark:text-mist-100">
        Nenhuma linha válida para analisar
      </p>
      <p class="mt-1 max-w-sm text-sm text-ink-600 dark:text-mist-300">
        As {{ inteiro(planilha.quantidadeLinhas) }} linhas de {{ planilha.nomeArquivo }} foram
        reprovadas na validação. Corrija os problemas listados acima e carregue de novo.
      </p>
    </template>
    <template v-else>
      <p class="mt-4 text-sm font-medium text-ink-900 dark:text-mist-100">
        Nenhuma planilha carregada
      </p>
      <p class="mt-1 max-w-sm text-sm text-ink-600 dark:text-mist-300">
        Os relatórios são montados sobre os dados tratados no Upload. Envie uma planilha para
        começar.
      </p>
    </template>
    <RouterLink
      class="mt-6 rounded bg-brand-800 px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-brand-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
      :to="{ name: 'upload' }"
    >
      Ir para o Upload
    </RouterLink>
  </section>

  <template v-else>
    <!-- Uma linha de filtros, acima de tudo que ela governa: cartões, gráficos e
         tabela reagem à mesma fatia, então os números nunca se contradizem. -->
    <div class="mt-6 flex flex-wrap items-end gap-3">
      <label class="flex flex-col gap-1">
        <span class="text-xs font-medium text-ink-600 dark:text-mist-300">Buscar</span>
        <input
          class="w-52 rounded border border-ink-200 bg-white px-3 py-1.5 text-sm text-ink-900 placeholder:text-ink-400 focus:border-brand-500 focus:outline-2 focus:outline-offset-0 focus:outline-brand-500 dark:border-night-700 dark:bg-night-900 dark:text-mist-100 dark:placeholder:text-mist-500"
          v-model="busca"
          type="search"
          placeholder="Cliente, código ou cidade"
        />
      </label>

      <label class="flex flex-col gap-1">
        <span class="text-xs font-medium text-ink-600 dark:text-mist-300">Segmento</span>
        <select
          class="rounded border border-ink-200 bg-white px-3 py-1.5 text-sm text-ink-900 focus:border-brand-500 focus:outline-2 focus:outline-offset-0 focus:outline-brand-500 dark:border-night-700 dark:bg-night-900 dark:text-mist-100"
          v-model="filtroSegmento"
        >
          <option value="todos">Todos</option>
          <option v-for="item in segmentos" :key="item" :value="item">{{ item }}</option>
        </select>
      </label>

      <label class="flex flex-col gap-1">
        <span class="text-xs font-medium text-ink-600 dark:text-mist-300">Nível</span>
        <select
          class="rounded border border-ink-200 bg-white px-3 py-1.5 text-sm text-ink-900 focus:border-brand-500 focus:outline-2 focus:outline-offset-0 focus:outline-brand-500 dark:border-night-700 dark:bg-night-900 dark:text-mist-100"
          v-model="filtroNivel"
        >
          <option value="todos">Todos</option>
          <option v-for="nivel in NIVEIS" :key="nivel" :value="nivel">{{ nivel }}</option>
        </select>
      </label>

      <label class="flex flex-col gap-1">
        <span class="text-xs font-medium text-ink-600 dark:text-mist-300">Consultor</span>
        <select
          class="rounded border border-ink-200 bg-white px-3 py-1.5 text-sm text-ink-900 focus:border-brand-500 focus:outline-2 focus:outline-offset-0 focus:outline-brand-500 dark:border-night-700 dark:bg-night-900 dark:text-mist-100"
          v-model="filtroConsultor"
        >
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
        class="ml-auto rounded border border-ink-200 px-4 py-1.5 text-sm font-medium text-brand-800 transition-colors hover:border-brand-500/50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 disabled:cursor-not-allowed disabled:opacity-50 dark:border-night-700 dark:text-brand-300"
        type="button"
        :disabled="!filtrados.length"
        @click="exportarCsv"
      >
        Exportar CSV
      </button>
    </div>

    <p
      class="mt-3 text-xs text-ink-600 dark:text-mist-300"
      v-if="temFiltro"
      role="status"
    >
      Mostrando {{ inteiro(filtrados.length) }} de {{ inteiro(planilha.quantidadeValidas) }} clientes.
    </p>

    <div class="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <div class="rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
        <p class="text-sm text-ink-600 dark:text-mist-300">Clientes</p>
        <p class="mt-3 text-3xl font-semibold leading-none text-ink-900 dark:text-mist-100">
          {{ inteiro(filtrados.length) }}
        </p>
      </div>
      <div class="rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
        <p class="text-sm text-ink-600 dark:text-mist-300">Faturamento anual</p>
        <p
          class="mt-3 whitespace-nowrap text-3xl font-semibold leading-none text-ink-900 dark:text-mist-100"
        >
          {{ dinheiroCurto(faturamento) }}
        </p>
      </div>
      <div class="rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
        <p class="text-sm text-ink-600 dark:text-mist-300">Ticket médio</p>
        <p
          class="mt-3 whitespace-nowrap text-3xl font-semibold leading-none text-ink-900 dark:text-mist-100"
        >
          {{ dinheiroCurto(ticketMedio) }}
        </p>
      </div>
      <div class="rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
        <p class="text-sm text-ink-600 dark:text-mist-300">Carteira nível A</p>
        <p class="mt-3 text-3xl font-semibold leading-none text-ink-900 dark:text-mist-100">
          {{ participacaoA.toLocaleString('pt-BR', { maximumFractionDigits: 1 }) }}%
        </p>
      </div>
    </div>

    <div class="mt-4 grid gap-4">
      <CartaoGrafico
        titulo="Contratações por mês"
        descricao="Clientes fechados em cada mês do período carregado."
        v-if="porMes.length > 1"
      >
        <GraficoArea
          :pontos="porMes"
          campo="clientes"
          unidade="clientes"
          formato-data="mes"
          descricao="Clientes contratados por mês."
        />

        <template #tabela>
          <div class="max-h-72 overflow-y-auto">
            <table class="w-full text-left text-sm">
              <thead class="sticky top-0 bg-white dark:bg-night-900">
                <tr class="border-b border-ink-200 dark:border-night-700">
                  <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Mês</th>
                  <th class="py-2 pr-4 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                    Clientes
                  </th>
                  <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                    Faturamento
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr
                  class="border-b border-ink-200 last:border-0 dark:border-night-700"
                  v-for="ponto in porMes"
                  :key="ponto.data.toISOString()"
                >
                  <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ mesAno(ponto.data) }}</td>
                  <td class="py-2 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">
                    {{ inteiro(ponto.clientes) }}
                  </td>
                  <td class="py-2 text-right tabular-nums text-ink-900 dark:text-mist-100">
                    {{ dinheiro(ponto.faturamento) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </CartaoGrafico>

      <div class="grid gap-4 lg:grid-cols-2">
        <CartaoGrafico titulo="Faturamento por segmento" descricao="Soma anual de cada segmento.">
          <GraficoBarras :itens="porSegmento" campo="faturamento" formato="moeda" />

          <template #tabela>
            <table class="w-full text-left text-sm">
              <thead>
                <tr class="border-b border-ink-200 dark:border-night-700">
                  <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">
                    Segmento
                  </th>
                  <th class="py-2 pr-4 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                    Clientes
                  </th>
                  <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                    Faturamento
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr
                  class="border-b border-ink-200 last:border-0 dark:border-night-700"
                  v-for="item in porSegmento"
                  :key="item.rotulo"
                >
                  <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
                  <td class="py-2 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">
                    {{ inteiro(item.clientes) }}
                  </td>
                  <td class="py-2 text-right tabular-nums text-ink-900 dark:text-mist-100">
                    {{ dinheiro(item.faturamento) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </template>
        </CartaoGrafico>

        <CartaoGrafico titulo="Faturamento por consultor" descricao="Soma anual de cada carteira.">
          <GraficoBarras :itens="porConsultor" campo="faturamento" formato="moeda" />

          <template #tabela>
            <table class="w-full text-left text-sm">
              <thead>
                <tr class="border-b border-ink-200 dark:border-night-700">
                  <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">
                    Consultor
                  </th>
                  <th class="py-2 pr-4 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                    Clientes
                  </th>
                  <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                    Faturamento
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr
                  class="border-b border-ink-200 last:border-0 dark:border-night-700"
                  v-for="item in porConsultor"
                  :key="item.rotulo"
                >
                  <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
                  <td class="py-2 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">
                    {{ inteiro(item.clientes) }}
                  </td>
                  <td class="py-2 text-right tabular-nums text-ink-900 dark:text-mist-100">
                    {{ dinheiro(item.faturamento) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </template>
        </CartaoGrafico>

        <CartaoGrafico
          titulo="Faturamento por nível"
          descricao="A faixa é ordenada: o tom escurece conforme o porte do cliente."
        >
          <GraficoBarras
            :itens="porNivel"
            campo="faturamento"
            formato="moeda"
            :cores="CORES_NIVEL"
          />

          <template #tabela>
            <table class="w-full text-left text-sm">
              <thead>
                <tr class="border-b border-ink-200 dark:border-night-700">
                  <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Nível</th>
                  <th class="py-2 pr-4 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                    Clientes
                  </th>
                  <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                    Faturamento
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr
                  class="border-b border-ink-200 last:border-0 dark:border-night-700"
                  v-for="item in porNivel"
                  :key="item.rotulo"
                >
                  <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
                  <td class="py-2 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">
                    {{ inteiro(item.clientes) }}
                  </td>
                  <td class="py-2 text-right tabular-nums text-ink-900 dark:text-mist-100">
                    {{ dinheiro(item.faturamento) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </template>
        </CartaoGrafico>

        <CartaoGrafico titulo="Faturamento por cidade" descricao="Soma anual por praça atendida.">
          <GraficoBarras :itens="porCidade" campo="faturamento" formato="moeda" />

          <template #tabela>
            <table class="w-full text-left text-sm">
              <thead>
                <tr class="border-b border-ink-200 dark:border-night-700">
                  <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Cidade</th>
                  <th class="py-2 pr-4 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                    Clientes
                  </th>
                  <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                    Faturamento
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr
                  class="border-b border-ink-200 last:border-0 dark:border-night-700"
                  v-for="item in porCidade"
                  :key="item.rotulo"
                >
                  <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
                  <td class="py-2 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">
                    {{ inteiro(item.clientes) }}
                  </td>
                  <td class="py-2 text-right tabular-nums text-ink-900 dark:text-mist-100">
                    {{ dinheiro(item.faturamento) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </template>
        </CartaoGrafico>
      </div>

      <section class="min-w-0 rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
        <h2 class="text-sm font-semibold text-ink-900 dark:text-mist-100">
          Carteira ({{ inteiro(ordenados.length) }})
        </h2>
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
                <th class="py-2 pr-4 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                  Faturamento
                </th>
                <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">
                  Contratação
                </th>
                <th class="py-2 font-medium text-ink-600 dark:text-mist-300" scope="col">Cidade</th>
              </tr>
            </thead>
            <tbody>
              <tr
                class="border-b border-ink-200 last:border-0 dark:border-night-700"
                v-for="registro in ordenados"
                :key="registro.codigo"
              >
                <td class="py-2 pr-4 tabular-nums text-ink-600 dark:text-mist-300">
                  {{ registro.codigo }}
                </td>
                <td class="py-2 pr-4 whitespace-nowrap text-ink-900 dark:text-mist-100">
                  {{ registro.nome }}
                </td>
                <td class="py-2 pr-4 whitespace-nowrap text-ink-900 dark:text-mist-100">
                  {{ registro.consultor }}
                </td>
                <td class="py-2 pr-4 whitespace-nowrap text-ink-900 dark:text-mist-100">
                  {{ registro.segmento }}
                </td>
                <td class="py-2 pr-4">
                  <span class="flex items-center gap-2">
                    <span
                      class="h-2.5 w-2.5 shrink-0 rounded-sm"
                      :class="CORES_NIVEL[registro.nivel]"
                      aria-hidden="true"
                    ></span>
                    <span class="font-semibold text-ink-900 dark:text-mist-100">
                      {{ registro.nivel }}
                    </span>
                  </span>
                </td>
                <td class="py-2 pr-4 text-right whitespace-nowrap tabular-nums text-ink-900 dark:text-mist-100">
                  {{ dinheiro(registro.faturamento) }}
                </td>
                <td class="py-2 pr-4 whitespace-nowrap tabular-nums text-ink-600 dark:text-mist-300">
                  {{ dataCurta(registro.contratacao) }}
                </td>
                <td class="py-2 whitespace-nowrap text-ink-900 dark:text-mist-100">
                  {{ registro.cidade }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <p
          class="mt-4 text-sm text-ink-600 dark:text-mist-300"
          v-if="!ordenados.length"
          role="status"
        >
          Nenhum cliente atende aos filtros selecionados.
        </p>
      </section>
    </div>
  </template>
</template>
