<script setup>
import { computed, onMounted, ref } from 'vue'
import CartaoGrafico from '../components/graficos/CartaoGrafico.vue'
import CartaoIndicador from '../components/graficos/CartaoIndicador.vue'
import GraficoArea from '../components/graficos/GraficoArea.vue'
import GraficoBarras from '../components/graficos/GraficoBarras.vue'
import { formatarNumero } from '../components/graficos/escala'
import { CORES_NIVEL, compactar, formatarData, mesParaData } from '../dados/painel'
import { api, mensagemDeErro } from '../services/api'

// carregando → pronto | vazio (nenhuma planilha processada) | erro
const estado = ref('carregando')
const erro = ref('')
const analise = ref(null) // GET /api/analises/ultima
const ind = ref(null) // GET /api/indicadores: o indicadores.json do Python
const historico = ref([]) // GET /api/analises

async function carregar() {
  estado.value = 'carregando'
  erro.value = ''
  try {
    const [ultima, indicadores, envios] = await Promise.all([
      api.get('/analises/ultima'),
      api.get('/indicadores'),
      api.get('/analises'),
    ])
    analise.value = ultima.data
    ind.value = indicadores.data
    historico.value = envios.data
    estado.value = 'pronto'
  } catch (falha) {
    // 404 aqui não é erro: só quer dizer que ninguém enviou planilha ainda.
    if (falha.response?.status === 404) {
      estado.value = 'vazio'
    } else {
      erro.value = mensagemDeErro(falha)
      estado.value = 'erro'
    }
  }
}

onMounted(carregar)

const reais = (valor) => `R$ ${compactar(valor)}`
const porcento = (valor) => `${formatarNumero(valor, 1)}%`
const dataHora = (iso) =>
  new Date(iso).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })

const indicadores = computed(() => {
  const geral = ind.value.faturamento.geral
  const nivelA = ind.value.faturamento.participacao_por_nivel.find((p) => p.nivel === 'A')
  return [
    { chave: 'clientes', rotulo: 'Clientes na carteira', valor: formatarNumero(ind.value.qualidade.linhas_validas) },
    { chave: 'total', rotulo: 'Faturamento anual somado', valor: reais(geral.soma) },
    { chave: 'mediana', rotulo: 'Faturamento do cliente típico (mediana)', valor: reais(geral.mediana) },
    { chave: 'nivelA', rotulo: 'Receita vinda do nível A', valor: porcento(nivelA?.percentual ?? 0) },
  ]
})

const porSegmento = computed(() =>
  ind.value.frequencias.segmento.map((f) => ({ rotulo: f.categoria, clientes: f.quantidade, percentual: f.percentual })),
)

const porNivel = computed(() => {
  const receita = Object.fromEntries(ind.value.faturamento.participacao_por_nivel.map((p) => [p.nivel, p.percentual]))
  return ind.value.frequencias.nivel.map((f) => ({
    rotulo: f.categoria,
    clientes: f.quantidade,
    percentual: f.percentual,
    receita: receita[f.categoria] ?? 0,
  }))
})

const porMes = computed(() =>
  ind.value.contratacoes.por_mes.map((m) => ({ data: mesParaData(m.mes), clientes: m.clientes, contratos: m.contratos })),
)

// Só os textos da análise; os resumos numéricos por segmento/faixa já estão nos gráficos.
const insights = computed(() => analise.value.insights.filter((i) => i.origem === 'ANALISE'))

const testes = computed(() => Object.values(ind.value.testes).filter((t) => t.aplicavel !== false))

const ROTULO_CATEGORIA = {
  SEGMENTO: 'Segmento',
  FATURAMENTO: 'Faturamento',
  SERVICO: 'Serviços',
  CONTRATACOES: 'Contratações',
  CONSULTOR: 'Consultores',
  ESTATISTICA: 'Estatística',
  QUALIDADE: 'Qualidade dos dados',
}
</script>

<template>
  <h1 class="text-2xl font-bold text-ink-900 dark:text-mist-100">Dashboard</h1>
  <p class="mt-2 text-sm text-ink-600 dark:text-mist-300" v-if="estado === 'pronto'">
    Carteira da última planilha processada: <strong class="font-semibold">{{ analise.arquivo }}</strong>,
    em {{ dataHora(analise.geradoEm) }}.
  </p>
  <p class="mt-2 text-sm text-ink-600 dark:text-mist-300" v-else>
    Indicadores e insights da carteira de clientes.
  </p>

  <p class="mt-8 text-sm text-ink-600 dark:text-mist-300" v-if="estado === 'carregando'" role="status">
    Carregando indicadores…
  </p>

  <section
    class="mt-8 rounded border border-ink-200 bg-white p-6 text-sm dark:border-night-700 dark:bg-night-900"
    v-else-if="estado === 'vazio'"
  >
    <h2 class="font-semibold text-ink-900 dark:text-mist-100">Nenhuma planilha processada ainda</h2>
    <p class="mt-2 text-ink-600 dark:text-mist-300">
      Os gráficos e insights aparecem aqui depois do primeiro envio.
    </p>
    <RouterLink
      class="mt-4 inline-flex rounded bg-brand-800 px-4 py-2 font-semibold text-white transition-colors hover:bg-brand-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
      :to="{ name: 'upload' }"
    >
      Enviar planilha
    </RouterLink>
  </section>

  <section
    class="mt-8 rounded border border-erro-600 bg-erro-600/5 p-6 text-sm dark:border-erro-400 dark:bg-erro-400/10"
    v-else-if="estado === 'erro'"
    role="alert"
  >
    <h2 class="font-semibold text-ink-900 dark:text-mist-100">Não foi possível carregar o painel</h2>
    <p class="mt-2 text-ink-900 dark:text-mist-100">{{ erro }}</p>
    <button
      class="mt-4 rounded border border-ink-200 px-4 py-2 font-semibold text-brand-800 transition-colors hover:border-brand-500 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:border-night-700 dark:text-brand-300"
      type="button"
      @click="carregar"
    >
      Tentar de novo
    </button>
  </section>

  <template v-else>
    <div class="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <CartaoIndicador
        v-for="indicador in indicadores"
        :key="indicador.chave"
        :rotulo="indicador.rotulo"
        :valor="indicador.valor"
      />
    </div>

    <div class="mt-4 grid gap-4 lg:grid-cols-2">
      <CartaoGrafico titulo="Clientes por segmento" descricao="Quantos clientes cada segmento tem na carteira.">
        <GraficoBarras :itens="porSegmento" campo="clientes" unidade="clientes" />

        <template #tabela>
          <table class="w-full text-left text-sm">
            <thead>
              <tr class="border-b border-ink-200 dark:border-night-700">
                <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Segmento</th>
                <th class="py-2 pr-4 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Clientes</th>
                <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">% da carteira</th>
              </tr>
            </thead>
            <tbody>
              <tr class="border-b border-ink-200 last:border-0 dark:border-night-700" v-for="item in porSegmento" :key="item.rotulo">
                <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
                <td class="py-2 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ formatarNumero(item.clientes) }}</td>
                <td class="py-2 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ porcento(item.percentual) }}</td>
              </tr>
            </tbody>
          </table>
        </template>
      </CartaoGrafico>

      <CartaoGrafico
        titulo="Clientes por nível A/B/C"
        descricao="A faixa é ordenada: o tom escurece conforme o nível sobe."
      >
        <GraficoBarras :itens="porNivel" campo="clientes" unidade="clientes" :cores="CORES_NIVEL" />

        <template #tabela>
          <table class="w-full text-left text-sm">
            <thead>
              <tr class="border-b border-ink-200 dark:border-night-700">
                <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Nível</th>
                <th class="py-2 pr-4 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Clientes</th>
                <th class="py-2 pr-4 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">% dos clientes</th>
                <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">% da receita</th>
              </tr>
            </thead>
            <tbody>
              <tr class="border-b border-ink-200 last:border-0 dark:border-night-700" v-for="item in porNivel" :key="item.rotulo">
                <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
                <td class="py-2 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ formatarNumero(item.clientes) }}</td>
                <td class="py-2 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ porcento(item.percentual) }}</td>
                <td class="py-2 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ porcento(item.receita) }}</td>
              </tr>
            </tbody>
          </table>
        </template>
      </CartaoGrafico>
    </div>

    <div class="mt-4">
      <CartaoGrafico
        titulo="Evolução das contratações"
        descricao="Novos clientes por mês, pela data de contratação. Mês sem venda aparece como zero."
      >
        <GraficoArea
          v-if="porMes.length > 1"
          :pontos="porMes"
          campo="clientes"
          unidade="clientes"
          formato-data="mes"
          descricao="Novos clientes por mês."
        />
        <p class="text-sm text-ink-600 dark:text-mist-300" v-else>
          A planilha cobre um mês só: a evolução precisa de pelo menos dois.
        </p>

        <template #tabela>
          <div class="max-h-72 overflow-y-auto">
            <table class="w-full text-left text-sm">
              <thead class="sticky top-0 bg-white dark:bg-night-900">
                <tr class="border-b border-ink-200 dark:border-night-700">
                  <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Mês</th>
                  <th class="py-2 pr-4 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Novos clientes</th>
                  <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Serviços contratados</th>
                </tr>
              </thead>
              <tbody>
                <tr class="border-b border-ink-200 last:border-0 dark:border-night-700" v-for="ponto in porMes" :key="ponto.data.toISOString()">
                  <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ formatarData(ponto.data, 'mes') }}</td>
                  <td class="py-2 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ formatarNumero(ponto.clientes) }}</td>
                  <td class="py-2 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ formatarNumero(ponto.contratos) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </CartaoGrafico>
    </div>

    <section class="mt-4 min-w-0 rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
      <h2 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Insights</h2>
      <p class="mt-1 text-xs text-ink-600 dark:text-mist-300">
        Gerados a partir dos números da planilha; mudam a cada envio.
      </p>
      <ul class="mt-4 grid gap-3 md:grid-cols-2">
        <li class="rounded border border-ink-200 p-4 dark:border-night-700" v-for="insight in insights" :key="insight.id">
          <p class="text-xs font-medium uppercase tracking-wide text-brand-800 dark:text-brand-300">
            {{ ROTULO_CATEGORIA[insight.categoria] ?? insight.categoria }}
          </p>
          <h3 class="mt-1 text-sm font-semibold text-ink-900 dark:text-mist-100">{{ insight.titulo }}</h3>
          <p class="mt-1 text-sm leading-relaxed text-ink-600 dark:text-mist-300">{{ insight.texto }}</p>
        </li>
      </ul>
    </section>

    <section class="mt-4 min-w-0 rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
      <h2 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Testes estatísticos</h2>
      <p class="mt-1 text-xs text-ink-600 dark:text-mist-300">Nível de significância de 5%.</p>
      <dl class="mt-4 grid gap-3">
        <div v-for="teste in testes" :key="teste.teste + (teste.variavel ?? '')">
          <dt class="text-sm font-medium text-ink-900 dark:text-mist-100">
            {{ teste.teste }}<span class="font-normal text-ink-600 dark:text-mist-300" v-if="teste.variavel"> · {{ teste.variavel }}</span>
          </dt>
          <dd class="text-sm text-ink-600 dark:text-mist-300">{{ teste.conclusao }}</dd>
        </div>
      </dl>
    </section>

    <section class="mt-4 min-w-0 rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
      <h2 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Últimos envios</h2>
      <p class="mt-1 text-xs text-ink-600 dark:text-mist-300">Planilhas processadas e o resultado da validação.</p>

      <div class="mt-4 overflow-x-auto">
        <table class="w-full min-w-136 text-left text-sm">
          <thead>
            <tr class="border-b border-ink-200 dark:border-night-700">
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Arquivo</th>
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Processado em</th>
              <th class="py-2 pr-4 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Gravadas</th>
              <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Recusadas</th>
            </tr>
          </thead>
          <tbody>
            <tr class="border-b border-ink-200 last:border-0 dark:border-night-700" v-for="envio in historico" :key="envio.id">
              <td class="py-2.5 pr-4 text-ink-900 dark:text-mist-100">{{ envio.arquivo }}</td>
              <td class="py-2.5 pr-4 text-ink-600 dark:text-mist-300">{{ dataHora(envio.geradoEm) }}</td>
              <td class="py-2.5 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ formatarNumero(envio.linhasValidas) }}</td>
              <td class="py-2.5 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ formatarNumero(envio.linhasRejeitadas) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </template>
</template>
