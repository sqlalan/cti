<script setup>
import { computed, ref } from 'vue'
import { FORMATOS, TAMANHO_MAXIMO, useUploadStore } from '../stores/uploadStore'
import { ESQUEMA } from '../dados/esquemaPlanilha'

const planilha = useUploadStore()

const arrastando = ref(false)
const LIMITE_PREVIA = 15

// A extensão é a checagem que vale: navegador e sistema operacional divergem no
// tipo MIME de planilha (o mesmo .xls chega como application/vnd.ms-excel,
// application/excel ou string vazia). O MIME entra só no accept, como dica ao
// seletor de arquivos.
const ACEITOS = [
  ...FORMATOS,
  'text/csv',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
].join(',')

const previa = computed(() => planilha.dados.slice(0, LIMITE_PREVIA))
const ocultas = computed(() => Math.max(0, planilha.dados.length - LIMITE_PREVIA))

function selecionar(evento) {
  const [arquivo] = evento.target.files
  planilha.carregar(arquivo)
  // Zera o input para que escolher o mesmo arquivo de novo volte a disparar o
  // change — sem isto, trocar e reselecionar não faz nada.
  evento.target.value = ''
}

function soltar(evento) {
  arrastando.value = false
  const [arquivo] = evento.dataTransfer.files
  planilha.carregar(arquivo)
}

function formatarTamanho(bytes) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1).replace('.', ',')} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1).replace('.', ',')} MB`
}

const inteiro = (valor) => valor.toLocaleString('pt-BR')

const dinheiro = (valor) =>
  typeof valor === 'number'
    ? valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
    : '—'

const dataCurta = (valor) => (valor instanceof Date ? valor.toLocaleDateString('pt-BR') : '—')

// A prévia percorre o ESQUEMA em vez de listar coluna por coluna: acrescentar
// um campo lá aparece aqui sem tocar no template.
function celula(coluna, registro) {
  const valor = registro[coluna.chave]
  if (coluna.tratamento === 'moeda') return dinheiro(valor)
  if (coluna.tratamento === 'dataBR') return dataCurta(valor)
  if (coluna.tratamento === 'listaServicos') return valor?.length ? valor.join(', ') : '—'
  return valor === '' || valor === null || valor === undefined ? '—' : String(valor)
}

const alinhaDireita = (coluna) => coluna.tratamento === 'moeda'

// Nível é faixa ordinal (A é o cliente maior), não status. Duas consequências:
// a cor é uma rampa de um matiz só, e ela entra como marca ao lado do texto —
// nunca no texto. Verde/âmbar/vermelho diria que cliente C é defeito, e um
// passo claro da rampa não alcança contraste de leitura em letra pequena.
const NIVEL_PONTO = {
  A: 'bg-nivel-a-600 dark:bg-nivel-a-400',
  B: 'bg-nivel-b-600 dark:bg-nivel-b-400',
  C: 'bg-nivel-c-600 dark:bg-nivel-c-400',
}
</script>

<template>
  <h1 class="text-2xl font-bold text-ink-900 dark:text-mist-100">Upload</h1>
  <p class="mt-2 text-sm text-ink-600 dark:text-mist-300">
    Envie a planilha de clientes para conferir o tratamento antes de mandar para o servidor.
  </p>

  <label
    class="mt-8 flex cursor-pointer flex-col items-center justify-center rounded border-2 border-dashed px-6 py-12 text-center transition-colors"
    :class="arrastando
      ? 'border-brand-500 bg-brand-500/5'
      : 'border-ink-200 bg-white hover:border-brand-500/50 dark:border-night-700 dark:bg-night-900'"
    @dragover.prevent="arrastando = true"
    @dragleave.prevent="arrastando = false"
    @drop.prevent="soltar"
  >
    <svg
      class="h-10 w-10 text-brand-800 dark:text-brand-300"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="1.75"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
      <path d="M17 8l-5-5-5 5" />
      <path d="M12 3v13" />
    </svg>
    <p class="mt-4 text-sm font-medium text-ink-900 dark:text-mist-100">
      Arraste a planilha ou clique para selecionar
    </p>
    <p class="mt-1 text-xs text-ink-400 dark:text-mist-500">
      {{ FORMATOS.join(', ') }} — até {{ formatarTamanho(TAMANHO_MAXIMO) }}
    </p>
    <input class="sr-only" type="file" :accept="ACEITOS" @change="selecionar" />
  </label>

  <p
    class="mt-4 rounded border border-ink-200 bg-white px-4 py-3 text-sm text-ink-600 dark:border-night-700 dark:bg-night-900 dark:text-mist-300"
    v-if="planilha.estado === 'lendo'"
    role="status"
  >
    Lendo a planilha…
  </p>

  <p
    class="mt-4 flex items-start gap-2 rounded border border-warn-500 bg-warn-500/10 px-4 py-3 text-sm text-ink-900 dark:bg-warn-500/15 dark:text-mist-100"
    v-if="planilha.falha"
    role="alert"
  >
    <svg
      class="mt-0.5 h-4 w-4 shrink-0 text-warn-500"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      <path d="M10.3 4.3 2.8 17a2 2 0 0 0 1.7 3h15a2 2 0 0 0 1.7-3L13.7 4.3a2 2 0 0 0-3.4 0Z" />
      <path d="M12 9v4" />
      <path d="M12 17h.01" />
    </svg>
    <span>{{ planilha.falha }}</span>
  </p>

  <template v-if="planilha.resumo">
    <section class="mt-6 min-w-0 rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0">
          <h2 class="truncate text-sm font-semibold text-ink-900 dark:text-mist-100">
            {{ planilha.arquivo.name }}
          </h2>
          <p class="mt-1 text-xs text-ink-600 dark:text-mist-300">
            {{ formatarTamanho(planilha.arquivo.size) }} · aba
            <span class="font-medium">{{ planilha.aba }}</span>
            <span v-if="planilha.resumo.periodo">
              · contratações de {{ dataCurta(planilha.resumo.periodo.inicio) }} a
              {{ dataCurta(planilha.resumo.periodo.fim) }}
            </span>
          </p>
        </div>
        <button
          class="shrink-0 text-sm font-medium text-brand-800 hover:text-brand-500 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:text-brand-300"
          type="button"
          @click="planilha.limpar()"
        >
          Trocar arquivo
        </button>
      </div>

      <dl class="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div>
          <dt class="text-xs text-ink-600 dark:text-mist-300">Linhas lidas</dt>
          <dd class="mt-1 text-2xl font-semibold text-ink-900 dark:text-mist-100">
            {{ inteiro(planilha.resumo.linhas) }}
          </dd>
        </div>
        <div>
          <dt class="text-xs text-ink-600 dark:text-mist-300">Válidas</dt>
          <dd class="mt-1 text-2xl font-semibold text-ok-600 dark:text-ok-400">
            {{ inteiro(planilha.resumo.validas) }}
          </dd>
        </div>
        <div>
          <dt class="text-xs text-ink-600 dark:text-mist-300">Com erro</dt>
          <dd
            class="mt-1 text-2xl font-semibold"
            :class="planilha.resumo.comErro
              ? 'text-erro-600 dark:text-erro-400'
              : 'text-ink-900 dark:text-mist-100'"
          >
            {{ inteiro(planilha.resumo.comErro) }}
          </dd>
        </div>
        <div>
          <dt class="text-xs text-ink-600 dark:text-mist-300">Faturamento somado</dt>
          <dd class="mt-1 whitespace-nowrap text-2xl font-semibold text-ink-900 dark:text-mist-100">
            {{ dinheiro(planilha.resumo.faturamentoTotal) }}
          </dd>
        </div>
      </dl>

      <div class="mt-6 grid gap-6 lg:grid-cols-4 sm:grid-cols-2">
        <div>
          <h3 class="text-xs font-semibold text-ink-900 dark:text-mist-100">Por nível</h3>
          <ul class="mt-2 flex flex-col gap-1.5">
            <li
              class="flex items-baseline gap-2 text-sm"
              v-for="(quantidade, nivel) in planilha.resumo.porNivel"
              :key="nivel"
            >
              <span
                class="h-2.5 w-2.5 shrink-0 rounded-sm"
                :class="NIVEL_PONTO[nivel]"
                aria-hidden="true"
              ></span>
              <span class="w-4 font-semibold text-ink-900 dark:text-mist-100">{{ nivel }}</span>
              <span class="tabular-nums text-ink-600 dark:text-mist-300">{{ inteiro(quantidade) }}</span>
            </li>
          </ul>
        </div>

        <div>
          <h3 class="text-xs font-semibold text-ink-900 dark:text-mist-100">Por segmento</h3>
          <ul class="mt-2 flex flex-col gap-1.5">
            <li
              class="flex items-baseline justify-between gap-3 text-sm"
              v-for="item in planilha.resumo.porSegmento"
              :key="item.chave"
            >
              <span class="min-w-0 truncate text-ink-900 dark:text-mist-100">{{ item.chave }}</span>
              <span class="shrink-0 tabular-nums text-ink-600 dark:text-mist-300">{{ item.linhas }}</span>
            </li>
          </ul>
        </div>

        <div>
          <h3 class="text-xs font-semibold text-ink-900 dark:text-mist-100">Por consultor</h3>
          <ul class="mt-2 flex flex-col gap-1.5">
            <li
              class="flex items-baseline justify-between gap-3 text-sm"
              v-for="item in planilha.resumo.porConsultor"
              :key="item.chave"
            >
              <span class="min-w-0 truncate text-ink-900 dark:text-mist-100">{{ item.chave }}</span>
              <span class="shrink-0 tabular-nums text-ink-600 dark:text-mist-300">{{ item.linhas }}</span>
            </li>
          </ul>
        </div>

        <div>
          <h3 class="text-xs font-semibold text-ink-900 dark:text-mist-100">Serviços contratados</h3>
          <ul class="mt-2 flex flex-col gap-1.5">
            <li
              class="flex items-baseline justify-between gap-3 text-sm"
              v-for="item in planilha.resumo.porServico"
              :key="item.servico"
            >
              <span class="min-w-0 truncate text-ink-900 dark:text-mist-100">{{ item.servico }}</span>
              <span class="shrink-0 tabular-nums text-ink-600 dark:text-mist-300">{{ item.clientes }}</span>
            </li>
          </ul>
        </div>
      </div>

      <p
        class="mt-6 text-xs text-ink-400 dark:text-mist-500"
        v-if="planilha.resumo.colunasIgnoradas.length || planilha.abasIgnoradas.length"
      >
        <span v-if="planilha.resumo.colunasIgnoradas.length">
          Colunas fora do esquema, lidas mas não tratadas:
          {{ planilha.resumo.colunasIgnoradas.join(', ') }}.
        </span>
        <span v-if="planilha.abasIgnoradas.length">
          Abas ignoradas: {{ planilha.abasIgnoradas.join(', ') }}.
        </span>
      </p>
    </section>

    <!-- Avisos não bloqueiam a linha: são coisas que passaram, mas que alguém
         precisa olhar — segmento novo, serviço fora do catálogo. -->
    <section
      class="mt-4 min-w-0 rounded border border-warn-500 bg-warn-500/10 p-5 dark:bg-warn-500/15"
      v-if="planilha.resumo.avisos.length"
    >
      <h2 class="text-sm font-semibold text-ink-900 dark:text-mist-100">
        Avisos ({{ inteiro(planilha.resumo.avisos.length) }})
      </h2>
      <p class="mt-1 text-xs text-ink-600 dark:text-mist-300">
        Estas linhas seguem no envio. Vale conferir o cadastro do dicionário em
        <code class="text-xs">esquemaPlanilha.js</code>.
      </p>
      <ul class="mt-3 flex flex-col gap-1.5">
        <li
          class="text-sm text-ink-900 dark:text-mist-100"
          v-for="(aviso, i) in planilha.resumo.avisos"
          :key="`${aviso.linha}-${i}`"
        >
          <span class="tabular-nums text-ink-600 dark:text-mist-300">Linha {{ aviso.linha }}</span>
          — {{ aviso.motivo }}
        </li>
      </ul>
    </section>

    <section
      class="mt-4 min-w-0 rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900"
      v-if="planilha.erros.length"
    >
      <h2 class="text-sm font-semibold text-ink-900 dark:text-mist-100">
        Linhas com erro ({{ inteiro(planilha.erros.length) }})
      </h2>
      <p class="mt-1 text-xs text-ink-600 dark:text-mist-300">
        Estas linhas ficam de fora do envio. O número é o da linha na planilha.
      </p>

      <div class="mt-4 max-h-72 overflow-auto">
        <table class="w-full min-w-lg text-left text-sm">
          <thead class="sticky top-0 bg-white dark:bg-night-900">
            <tr class="border-b border-ink-200 dark:border-night-700">
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Linha</th>
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Código</th>
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Cliente</th>
              <th class="py-2 font-medium text-ink-600 dark:text-mist-300" scope="col">Motivo</th>
            </tr>
          </thead>
          <tbody>
            <tr
              class="border-b border-ink-200 last:border-0 dark:border-night-700"
              v-for="erro in planilha.erros"
              :key="`${erro.linha}-${erro.motivo}`"
            >
              <td class="py-2 pr-4 tabular-nums text-ink-600 dark:text-mist-300">{{ erro.linha }}</td>
              <td class="py-2 pr-4 tabular-nums text-ink-900 dark:text-mist-100">{{ erro.codigo }}</td>
              <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ erro.nome }}</td>
              <td class="py-2 text-erro-600 dark:text-erro-400">{{ erro.motivo }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section
      class="mt-4 min-w-0 rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900"
      v-if="planilha.temDados"
    >
      <h2 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Prévia dos dados tratados</h2>
      <p class="mt-1 text-xs text-ink-600 dark:text-mist-300">
        Espaços removidos, segmento e consultor padronizados, nível validado, valor e data convertidos.
      </p>

      <div class="mt-4 overflow-x-auto">
        <table class="w-full min-w-4xl text-left text-sm">
          <thead>
            <tr class="border-b border-ink-200 dark:border-night-700">
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Linha</th>
              <th
                class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300"
                :class="alinhaDireita(coluna) ? 'text-right' : ''"
                v-for="coluna in ESQUEMA"
                :key="coluna.chave"
                scope="col"
              >
                {{ coluna.rotulo }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              class="border-b border-ink-200 last:border-0 dark:border-night-700"
              v-for="registro in previa"
              :key="registro.linha"
            >
              <td class="py-2 pr-4 tabular-nums text-ink-400 dark:text-mist-500">{{ registro.linha }}</td>
              <td
                class="py-2 pr-4 whitespace-nowrap text-ink-900 dark:text-mist-100"
                :class="alinhaDireita(coluna) ? 'text-right tabular-nums' : ''"
                v-for="coluna in ESQUEMA"
                :key="coluna.chave"
              >
                <span class="flex items-center gap-2" v-if="coluna.chave === 'nivel'">
                  <span
                    class="h-2.5 w-2.5 shrink-0 rounded-sm"
                    :class="NIVEL_PONTO[registro.nivel]"
                    aria-hidden="true"
                  ></span>
                  <span class="font-semibold">{{ registro.nivel }}</span>
                </span>
                <template v-else>{{ celula(coluna, registro) }}</template>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p class="mt-3 text-xs text-ink-400 dark:text-mist-500" v-if="ocultas">
        Mostrando as primeiras {{ LIMITE_PREVIA }} linhas — outras {{ inteiro(ocultas) }} seguem no envio.
      </p>
    </section>

    <!-- Sem nenhuma linha válida a prévia acima não renderiza, e uma seção que
         simplesmente some não diz à pessoa que ela sumiu — parece tela quebrada.
         Este bloco ocupa o lugar dela e aponta para a tabela de erros. -->
    <section
      class="mt-4 min-w-0 rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900"
      v-else
    >
      <h2 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Prévia dos dados tratados</h2>
      <p class="mt-1 text-sm text-ink-600 dark:text-mist-300">
        Nenhuma linha passou no tratamento, então não há o que pré-visualizar. Os
        {{ inteiro(planilha.erros.length) }} motivos estão na tabela acima — corrija a planilha e
        carregue de novo.
      </p>
    </section>

    <div class="mt-6 flex flex-wrap items-center gap-3">
      <button
        class="rounded bg-brand-800 px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-brand-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 disabled:cursor-not-allowed disabled:opacity-50"
        type="button"
        :disabled="!planilha.podeEnviar"
        @click="planilha.enviar()"
      >
        {{ planilha.estado === 'enviando' ? 'Enviando…' : 'Enviar para o servidor' }}
      </button>

      <p
        class="flex items-center gap-1.5 text-sm font-medium text-ok-600 dark:text-ok-400"
        v-if="planilha.estado === 'enviado'"
        role="status"
      >
        <svg
          class="h-4 w-4 shrink-0"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <circle cx="12" cy="12" r="9" />
          <path d="m8.5 12 2.5 2.5 4.5-5" />
        </svg>
        Enviado
      </p>
    </div>
  </template>

  <p
    class="mt-8 rounded border border-warn-500 bg-warn-500/10 px-4 py-3 text-sm text-ink-900 dark:bg-warn-500/15 dark:text-mist-100"
    role="status"
  >
    O envio ainda não sai da máquina: o ponto de integração está marcado em
    <code class="text-xs">enviar()</code>, em <code class="text-xs">src/stores/uploadStore.js</code>.
  </p>
</template>
