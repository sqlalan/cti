<script setup>
import { computed, ref } from 'vue'
import CartaoGrafico from '../components/graficos/CartaoGrafico.vue'
import CartaoIndicador from '../components/graficos/CartaoIndicador.vue'
import GraficoArea from '../components/graficos/GraficoArea.vue'
import GraficoBarras from '../components/graficos/GraficoBarras.vue'
import GraficoPilha from '../components/graficos/GraficoPilha.vue'
import { ESTADOS, PERIODOS, formatarData, gerarPainel } from '../dados/painel'
import { formatarNumero } from '../components/graficos/escala'

// Um filtro só, no topo, governando tudo o que vem abaixo. Período por gráfico
// faria os números da mesma tela discordarem entre si.
const periodo = ref(30)
const painel = computed(() => gerarPainel(periodo.value))
const rotuloPeriodo = computed(() => PERIODOS.find((item) => item.dias === periodo.value).rotulo)

const APARENCIA_ESTADO = {
  ok: 'text-ok-600 dark:text-ok-400',
  alerta: 'text-alerta-600 dark:text-alerta-400',
  erro: 'text-erro-600 dark:text-erro-400',
}

const PONTO_ESTADO = {
  ok: 'bg-ok-600 dark:bg-ok-400',
  alerta: 'bg-alerta-600 dark:bg-alerta-400',
  erro: 'bg-erro-600 dark:bg-erro-400',
}
</script>

<template>
  <h1 class="text-2xl font-bold text-ink-900 dark:text-mist-100">Dashboard</h1>
  <p class="mt-2 text-sm text-ink-600 dark:text-mist-300">
    Acompanhe o processamento das planilhas enviadas.
  </p>

  <div class="mt-6 flex flex-wrap items-center gap-3">
    <span class="text-xs font-medium text-ink-600 dark:text-mist-300">Período</span>
    <div
      class="flex rounded border border-ink-200 p-0.5 dark:border-night-700"
      role="group"
      aria-label="Período exibido"
    >
      <button
        class="rounded px-3 py-1.5 text-xs font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
        :class="periodo === item.dias
          ? 'bg-brand-800 text-white'
          : 'text-ink-600 hover:text-brand-800 dark:text-mist-300 dark:hover:text-brand-300'"
        v-for="item in PERIODOS"
        :key="item.dias"
        type="button"
        :aria-pressed="periodo === item.dias"
        @click="periodo = item.dias"
      >
        {{ item.rotulo }}
      </button>
    </div>
  </div>

  <div class="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
    <CartaoIndicador
      v-for="indicador in painel.indicadores"
      :key="indicador.chave"
      :rotulo="indicador.rotulo"
      :valor="indicador.valor"
      :delta="indicador.delta"
      :subir-eh-bom="indicador.subirEhBom"
      :faisca="indicador.faisca"
      :periodo="rotuloPeriodo"
    />
  </div>

  <div class="mt-4 grid gap-4">
    <CartaoGrafico
      titulo="Registros processados por dia"
      :descricao="`Volume diário nos últimos ${rotuloPeriodo}.`"
    >
      <GraficoArea :pontos="painel.volume" />

      <template #tabela>
        <div class="max-h-72 overflow-y-auto">
          <table class="w-full text-left text-sm">
            <thead class="sticky top-0 bg-white dark:bg-night-900">
              <tr class="border-b border-ink-200 dark:border-night-700">
                <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Data</th>
                <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                  Registros
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                class="border-b border-ink-200 last:border-0 dark:border-night-700"
                v-for="ponto in painel.volume"
                :key="ponto.data.toISOString()"
              >
                <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">
                  {{ formatarData(ponto.data, 'longo') }}
                </td>
                <td class="py-2 text-right tabular-nums text-ink-900 dark:text-mist-100">
                  {{ formatarNumero(ponto.registros) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </CartaoGrafico>

    <div class="grid gap-4 lg:grid-cols-2">
      <CartaoGrafico titulo="Arquivos por formato" descricao="Distribuição das planilhas recebidas.">
        <GraficoBarras :itens="painel.tipos" campo="arquivos" unidade="arquivos" />

        <template #tabela>
          <table class="w-full text-left text-sm">
            <thead>
              <tr class="border-b border-ink-200 dark:border-night-700">
                <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Formato</th>
                <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                  Arquivos
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                class="border-b border-ink-200 last:border-0 dark:border-night-700"
                v-for="tipo in painel.tipos"
                :key="tipo.rotulo"
              >
                <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ tipo.rotulo }}</td>
                <td class="py-2 text-right tabular-nums text-ink-900 dark:text-mist-100">
                  {{ formatarNumero(tipo.arquivos) }}
                </td>
              </tr>
            </tbody>
          </table>
        </template>
      </CartaoGrafico>

      <CartaoGrafico titulo="Qualidade dos registros" descricao="Classificação após a validação do lote.">
        <GraficoPilha :itens="painel.qualidade" />

        <template #tabela>
          <table class="w-full text-left text-sm">
            <thead>
              <tr class="border-b border-ink-200 dark:border-night-700">
                <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">
                  Classificação
                </th>
                <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                  Registros
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                class="border-b border-ink-200 last:border-0 dark:border-night-700"
                v-for="item in painel.qualidade"
                :key="item.chave"
              >
                <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
                <td class="py-2 text-right tabular-nums text-ink-900 dark:text-mist-100">
                  {{ formatarNumero(item.registros) }}
                </td>
              </tr>
            </tbody>
          </table>
        </template>
      </CartaoGrafico>
    </div>

    <section class="min-w-0 rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
      <h2 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Últimos envios</h2>
      <p class="mt-1 text-xs text-ink-600 dark:text-mist-300">
        Arquivos recebidos e o resultado da validação de cada um.
      </p>

      <div class="mt-4 overflow-x-auto">
        <table class="w-full min-w-136 text-left text-sm">
          <thead>
            <tr class="border-b border-ink-200 dark:border-night-700">
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Arquivo</th>
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Recebido</th>
              <th class="py-2 pr-4 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                Registros
              </th>
              <th class="py-2 font-medium text-ink-600 dark:text-mist-300" scope="col">Situação</th>
            </tr>
          </thead>
          <tbody>
            <tr
              class="border-b border-ink-200 last:border-0 dark:border-night-700"
              v-for="envio in painel.envios"
              :key="envio.nome"
            >
              <td class="py-2.5 pr-4 text-ink-900 dark:text-mist-100">{{ envio.nome }}</td>
              <td class="py-2.5 pr-4 text-ink-600 dark:text-mist-300">
                {{ formatarData(envio.data, 'longo') }}
              </td>
              <td class="py-2.5 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">
                {{ formatarNumero(envio.registros) }}
              </td>
              <td class="py-2.5">
                <span class="flex items-center gap-1.5">
                  <span
                    class="h-2 w-2 shrink-0 rounded-full"
                    :class="PONTO_ESTADO[envio.estado]"
                    aria-hidden="true"
                  ></span>
                  <span class="font-medium" :class="APARENCIA_ESTADO[envio.estado]">
                    {{ ESTADOS[envio.estado].rotulo }}
                  </span>
                  <span class="text-xs text-ink-400 dark:text-mist-500" v-if="envio.inconsistencias">
                    ({{ formatarNumero(envio.inconsistencias) }} inconsistências)
                  </span>
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>

  <p
    class="mt-6 rounded border border-warn-500 bg-warn-500/10 px-4 py-3 text-sm text-ink-900 dark:bg-warn-500/15 dark:text-mist-100"
    role="status"
  >
    Números de exemplo, gerados no próprio navegador por
    <code class="text-xs">src/dados/painel.js</code>. Troque
    <code class="text-xs">gerarPainel</code> pela chamada à API para ligar o painel ao backend.
  </p>
</template>
