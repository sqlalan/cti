<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { FORMATOS, TAMANHO_MAXIMO, formatarTamanho, useUploadStore } from '../stores/uploadStore'
import { ESQUEMA, NIVEIS } from '../dados/esquemaPlanilha'
import EtapasEnvio from '../components/EtapasEnvio.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { explicarLote } from '../dados/lote'

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

const previa = computed(() => planilha.dadosValidos.slice(0, LIMITE_PREVIA))
const ocultas = computed(() => Math.max(0, planilha.quantidadeValidas - LIMITE_PREVIA))

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

// Grade das duas faixas do resumo — os totais em cima e os recortes embaixo.
// Mora num lugar só porque as colunas precisam cair uma sobre a outra, e isso
// depende de a quantidade de colunas, o breakpoint E o espaço entre elas serem
// idênticos: com 16px em cima e 40px embaixo, a diferença se repartia entre as
// colunas e cada uma deslizava 6px a mais que a anterior. O breakpoint é xl
// porque "R$ 72.360.000,00" em text-2xl não cabe em ¼ de uma tela lg.
const COLUNAS_RESUMO = 'grid gap-x-10 sm:grid-cols-2 xl:grid-cols-4'

// Os quatro recortes do resumo, na mesma forma: um template só desenha todos,
// e eles não voltam a divergir de layout como o de nível chegou a divergir.
//
// Todos contam clientes, e a lista segue a ordem do número que ela mostra. O
// resumo entrega segmento e consultor ordenados por FATURAMENTO — ordem certa
// para o relatório, que desenha faturamento, mas aqui ao lado de uma contagem
// ela fazia a coluna subir e descer (7, 4, 7, 3…) e parecer número solto.
// Nível é a exceção: A, B, C é ordem de faixa, e reordenar por quantidade
// apagaria justamente o que a rampa de cor mostra.
const grupos = computed(() => {
  const resumo = planilha.resumo
  if (!resumo) return []

  const porQuantidade = (itens) =>
    [...itens].sort(
      (a, b) => b.quantidade - a.quantidade || a.rotulo.localeCompare(b.rotulo, 'pt-BR'),
    )

  return [
    {
      titulo: 'Por nível',
      itens: NIVEIS.map((nivel) => ({
        rotulo: nivel,
        quantidade: resumo.porNivel[nivel] ?? 0,
        ponto: NIVEL_PONTO[nivel],
      })),
    },
    {
      titulo: 'Por segmento',
      itens: porQuantidade(
        resumo.porSegmento.map((item) => ({ rotulo: item.chave, quantidade: item.linhas })),
      ),
    },
    {
      titulo: 'Por consultor',
      itens: porQuantidade(
        resumo.porConsultor.map((item) => ({ rotulo: item.chave, quantidade: item.linhas })),
      ),
    },
    {
      titulo: 'Serviços contratados',
      itens: porQuantidade(
        resumo.porServico.map((item) => ({ rotulo: item.servico, quantidade: item.clientes })),
      ),
    },
  ]
})

// Nem o envio nem o Python informam quanto falta. O relógio mostra que o
// trabalho segue andando.
const agora = ref(Date.now())
let relogio = null
const trabalhando = computed(() => planilha.estado === 'enviando' || planilha.etapaEtl === 'processando')
watch(
  trabalhando,
  (ativo) => {
    clearInterval(relogio)
    if (ativo) {
      agora.value = Date.now()
      relogio = setInterval(() => (agora.value = Date.now()), 1000)
    }
  },
  { immediate: true },
)
onBeforeUnmount(() => clearInterval(relogio))

const tempo = (ms) => {
  const segundos = Math.max(0, Math.floor(ms / 1000))
  return `${Math.floor(segundos / 60)}:${String(segundos % 60).padStart(2, '0')}`
}

const plural = (quantidade, singular, varias) => `${inteiro(quantidade)} ${quantidade === 1 ? singular : varias}`

// Falha com dados na tela só pode ter vindo do envio (a de leitura limpa tudo).
const falhouEnvio = computed(() => planilha.estado === 'falha' && planilha.temDados)

const etapas = computed(() => {
  const servidor = planilha.validacaoServidor
  const estado = planilha.estado

  const leitura = planilha.temDados
    ? { situacao: 'feito', detalhe: plural(planilha.quantidadeValidas, 'linha aproveitada', 'linhas aproveitadas') }
    : { situacao: 'erro', detalhe: 'Nenhuma linha aproveitada' }

  let validacao = { situacao: 'pendente', detalhe: '' }
  if (servidor?.status === 'validando') validacao = { situacao: 'andamento', detalhe: 'Conferindo os dados…' }
  else if (servidor?.status === 'aprovado') validacao = { situacao: 'feito', detalhe: plural(servidor.clientes.length, 'linha aprovada', 'linhas aprovadas') }
  else if (servidor?.status === 'recusado') validacao = { situacao: 'erro', detalhe: 'Recusada — motivo abaixo' }

  let envio = { situacao: 'pendente', detalhe: servidor?.status === 'aprovado' ? 'Aguardando o seu comando' : '' }
  if (estado === 'enviando') {
    envio = { situacao: 'andamento', detalhe: `Enviando as linhas validadas… ${tempo(agora.value - planilha.inicioEnvio)}` }
  } else if (estado === 'enviado') {
    envio = { situacao: 'feito', detalhe: `Lote ${planilha.lote?.id} · ${plural(planilha.lote?.linhasValidas ?? 0, 'linha gravada', 'linhas gravadas')}` }
  } else if (falhouEnvio.value) {
    envio = { situacao: 'erro', detalhe: 'Não concluído — motivo abaixo' }
  }

  // O envio só responde depois do processamento: enquanto envia, os dados já estão sendo processados.
  let python = { situacao: 'pendente', detalhe: '' }
  if (estado === 'enviando' || planilha.etapaEtl === 'processando') {
    python = { situacao: 'andamento', detalhe: `Calculando os indicadores… ${tempo(agora.value - planilha.inicioEnvio)}` }
  } else if (planilha.etapaEtl === 'concluido') {
    python = { situacao: 'feito', detalhe: 'Indicadores prontos em Gráficos' }
  } else if (planilha.etapaEtl === 'erro') {
    python = { situacao: 'erro', detalhe: 'Falhou — motivo abaixo' }
  }

  return [
    { titulo: 'Leitura', ...leitura },
    { titulo: 'Validação no servidor', ...validacao },
    { titulo: 'Envio do lote', ...envio },
    { titulo: 'Processamento', ...python },
  ]
})

const ocupado = computed(
  () =>
    planilha.validacaoServidor?.status === 'validando' ||
    planilha.estado === 'enviando' ||
    planilha.etapaEtl === 'processando',
)

// Com planilha grande o resultado nasce fora da tela; sem rolar até ele, a
// pessoa não percebe que terminou.
const resultado = ref(null)

async function processar() {
  if (!(await planilha.enviar())) return
  await nextTick()
  const reduzir = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  resultado.value?.scrollIntoView({ behavior: reduzir ? 'auto' : 'smooth', block: 'center' })
  resultado.value?.focus({ preventScroll: true })
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

  <!-- Estes blocos não são mais região viva: quem anuncia agora é a pilha de
       notificações, e duas regiões dizendo a mesma frase fazem o leitor de
       tela repetir tudo. Aqui eles seguem como estado visível na página —
       o aviso passa, a situação continua. -->
  <p
    class="mt-4 rounded border border-ink-200 bg-white px-4 py-3 text-sm text-ink-600 dark:border-night-700 dark:bg-night-900 dark:text-mist-300"
    v-if="planilha.estado === 'lendo'"
  >
    Lendo a planilha…
  </p>

  <p
    class="mt-4 flex items-start gap-2 rounded border border-warn-500 bg-warn-500/10 px-4 py-3 text-sm text-ink-900 dark:bg-warn-500/15 dark:text-mist-100"
    v-if="planilha.falha && !falhouEnvio"
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
            {{ planilha.nomeArquivo }}
          </h2>
          <p class="mt-1 text-xs text-ink-600 dark:text-mist-300">
            {{ formatarTamanho(planilha.tamanhoArquivo) }} · aba
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

      <dl class="mt-5 gap-y-4" :class="COLUNAS_RESUMO">
        <div>
          <dt class="text-xs text-ink-600 dark:text-mist-300">Linhas lidas</dt>
          <dd class="mt-1 text-2xl font-semibold text-ink-900 dark:text-mist-100">
            {{ inteiro(planilha.quantidadeLinhas) }}
          </dd>
        </div>
        <div>
          <dt class="text-xs text-ink-600 dark:text-mist-300">Válidas</dt>
          <dd class="mt-1 text-2xl font-semibold text-ok-600 dark:text-ok-400">
            {{ inteiro(planilha.quantidadeValidas) }}
          </dd>
        </div>
        <div>
          <dt class="text-xs text-ink-600 dark:text-mist-300">Com erro</dt>
          <dd
            class="mt-1 text-2xl font-semibold"
            :class="planilha.quantidadeInvalidas
              ? 'text-erro-600 dark:text-erro-400'
              : 'text-ink-900 dark:text-mist-100'"
          >
            {{ inteiro(planilha.quantidadeInvalidas) }}
          </dd>
        </div>
        <div>
          <dt class="text-xs text-ink-600 dark:text-mist-300">Faturamento somado</dt>
          <dd class="mt-1 whitespace-nowrap text-2xl font-semibold text-ink-900 dark:text-mist-100">
            {{ dinheiro(planilha.totalFaturamento) }}
          </dd>
        </div>
      </dl>

      <!-- Três coisas amarram cada número à sua lista, e não à vizinha:
           a lista tem largura limitada, então o número fica perto do nome e
           longe da próxima coluna; o fio sob o título termina onde a lista
           termina, fechando o bloco; e o "clientes" em cima da coluna diz o
           que o número conta. Antes, cada lista ocupava ¼ da tela e o número
           ia para a borda — colado no nome da coluna seguinte. -->
      <div class="mt-6 gap-y-6" :class="COLUNAS_RESUMO">
        <!-- 192px: o piso é o cabeçalho mais longo ("Serviços contratados" +
             "clientes" medem 166px) — abaixo disso ele quebra de linha. Os 26px
             de folga absorvem fonte que renderize maior em outro sistema. -->
        <div class="w-full max-w-48" v-for="grupo in grupos" :key="grupo.titulo">
          <div class="flex items-baseline justify-between gap-3 border-b border-ink-200 pb-1.5 dark:border-night-700">
            <h3 class="text-xs font-semibold text-ink-900 dark:text-mist-100">{{ grupo.titulo }}</h3>
            <span class="text-xs text-ink-400 dark:text-mist-500">clientes</span>
          </div>
          <ul class="mt-2 flex flex-col gap-1.5">
            <li
              class="flex items-baseline justify-between gap-3 text-sm"
              v-for="item in grupo.itens"
              :key="item.rotulo"
            >
              <span class="flex min-w-0 items-center gap-2">
                <span
                  class="h-2.5 w-2.5 shrink-0 rounded-sm"
                  :class="item.ponto"
                  v-if="item.ponto"
                  aria-hidden="true"
                ></span>
                <span
                  class="truncate text-ink-900 dark:text-mist-100"
                  :class="{ 'font-semibold': item.ponto }"
                >
                  {{ item.rotulo }}
                </span>
              </span>
              <span class="shrink-0 font-medium tabular-nums text-ink-900 dark:text-mist-100">
                {{ inteiro(item.quantidade) }}
              </span>
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

    <!-- Pinia valida na tela, o servidor valida de novo e, no envio,
         chama o Python, que grava o lote. O botão só libera quando o
         servidor aprovou. Fica logo abaixo do resumo: com os avisos, os erros e
         a prévia antes dele, era preciso rolar a página inteira para enviar. -->
    <EtapasEnvio class="mt-6" :etapas="etapas" />

    <div class="mt-4 flex flex-wrap items-center gap-3">
      <button
        class="inline-flex items-center gap-2 rounded bg-brand-800 px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-brand-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 disabled:cursor-not-allowed disabled:opacity-50"
        type="button"
        :disabled="!planilha.podeEnviar"
        :aria-busy="ocupado"
        :title="planilha.podeEnviar || ocupado || planilha.estado === 'enviado' ? '' : 'Disponível depois que o servidor aprovar as linhas'"
        @click="processar"
      >
        <svg
          class="h-4 w-4 shrink-0 animate-spin motion-reduce:animate-none"
          v-if="ocupado"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          aria-hidden="true"
        >
          <circle class="opacity-25" cx="12" cy="12" r="9" />
          <path d="M21 12a9 9 0 0 0-9-9" stroke-linecap="round" />
        </svg>
        <template v-if="planilha.validacaoServidor?.status === 'validando'">Validando no servidor…</template>
        <template v-else-if="planilha.estado === 'enviando'">Enviando…</template>
        <template v-else-if="planilha.etapaEtl === 'processando'">Processando…</template>
        <template v-else-if="planilha.etapaEtl === 'erro'">Enviar de novo</template>
        <template v-else-if="planilha.estado === 'enviado'">Processada ✓</template>
        <template v-else>Enviar para análise</template>
      </button>
    </div>

    <p
      class="mt-3 rounded border border-warn-500 bg-warn-500/10 px-4 py-3 text-sm text-ink-900 dark:bg-warn-500/15 dark:text-mist-100"
      v-if="falhouEnvio"
      role="alert"
    >
      {{ planilha.falha.replace(/\.?$/, '.') }} A planilha continua carregada — é só clicar em enviar de novo.
    </p>
    <p
      class="mt-3 rounded border border-warn-500 bg-warn-500/10 px-4 py-3 text-sm text-ink-900 dark:bg-warn-500/15 dark:text-mist-100"
      v-else-if="planilha.validacaoServidor?.status === 'recusado'"
      role="alert"
    >
      O servidor recusou a planilha: {{ planilha.validacaoServidor.mensagem }}
      <button
        class="ml-1 font-semibold text-brand-800 underline hover:text-brand-500 dark:text-brand-300"
        type="button"
        v-if="planilha.validacaoServidor.semResposta"
        @click="planilha.validarNoServidor()"
      >
        Tentar de novo
      </button>
    </p>

    <!-- Depois do envio: o Python já criou e processou o lote. O cartão mostra
         "concluído" ou "falhou"; se o lote ainda estiver processando, a store
         pergunta o status a cada 2 segundos. -->
    <section
      class="mt-4 rounded border p-5 focus:outline-none"
      :class="{
        'border-brand-500/50 bg-brand-500/5 dark:bg-brand-500/10': planilha.etapaEtl === 'processando',
        'border-ok-600/40 bg-ok-600/5 dark:border-ok-400/40 dark:bg-ok-400/10': planilha.etapaEtl === 'concluido',
        'border-erro-600/50 bg-erro-600/5 dark:border-erro-400/50 dark:bg-erro-400/10': planilha.etapaEtl === 'erro',
      }"
      v-if="planilha.estado === 'enviado' && planilha.lote"
      ref="resultado"
      tabindex="-1"
      role="status"
      aria-live="polite"
      :aria-busy="planilha.etapaEtl === 'processando'"
    >
      <div class="flex items-center gap-4">
        <!-- Processando: círculo girando -->
        <svg
          class="h-10 w-10 shrink-0 animate-spin text-brand-800 motion-reduce:animate-none dark:text-brand-300"
          v-if="planilha.etapaEtl === 'processando'"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.5"
          aria-hidden="true"
        >
          <circle class="opacity-20" cx="12" cy="12" r="9" />
          <path d="M21 12a9 9 0 0 0-9-9" stroke-linecap="round" />
        </svg>
        <!-- Concluído: check verde -->
        <svg
          class="h-10 w-10 shrink-0 text-ok-600 dark:text-ok-400"
          v-else-if="planilha.etapaEtl === 'concluido'"
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
        <!-- Falhou: X vermelho -->
        <svg
          class="h-10 w-10 shrink-0 text-erro-600 dark:text-erro-400"
          v-else
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <circle cx="12" cy="12" r="9" />
          <path d="m9 9 6 6M15 9l-6 6" />
        </svg>

        <div class="min-w-0">
          <h2 class="text-base font-semibold text-ink-900 dark:text-mist-100">
            <template v-if="planilha.etapaEtl === 'processando'">Processando a planilha…</template>
            <template v-else-if="planilha.etapaEtl === 'concluido'">Planilha processada com sucesso</template>
            <template v-else>O processamento falhou</template>
          </h2>
          <p class="text-sm text-ink-600 dark:text-mist-300">
            Lote {{ planilha.lote.id }} · {{ planilha.nomeArquivo }}
            <template v-if="planilha.etapaEtl === 'processando'">
              · {{ tempo(agora - planilha.inicioEnvio) }}
            </template>
          </p>
        </div>
      </div>

      <!-- Barra sem porcentagem: o Python não informa quanto falta. -->
      <template v-if="planilha.etapaEtl === 'processando'">
        <div class="mt-4 h-1.5 overflow-hidden rounded-full bg-brand-500/15" aria-hidden="true">
          <div class="h-full w-2/5 rounded-full bg-brand-500 animate-progresso motion-reduce:w-full motion-reduce:animate-none" />
        </div>
        <p class="mt-3 text-sm text-ink-600 dark:text-mist-300">
          O sistema está organizando os dados, calculando os indicadores e salvando o resultado. Esta
          tela atualiza sozinha; você pode sair dela, o resultado também aparece em Gráficos.
        </p>
        <p class="mt-2 text-sm text-alerta-600 dark:text-alerta-400" v-if="planilha.semRespostaEtl">
          Está demorando mais que o normal. Acompanhe em Gráficos; se não terminar, tente enviar de
          novo mais tarde ou avise o suporte.
        </p>
      </template>

      <p class="mt-3 text-sm text-ink-900 dark:text-mist-100" v-else-if="planilha.etapaEtl === 'erro'">
        {{ explicarLote(planilha.lote) }}
        A planilha continua carregada: é só usar <span class="font-semibold">Enviar de novo</span>.
      </p>

      <dl class="mt-4 grid gap-3 text-sm sm:grid-cols-4">
        <div>
          <dt class="text-ink-600 dark:text-mist-300">Linhas recebidas</dt>
          <dd class="text-lg font-semibold tabular-nums text-ink-900 dark:text-mist-100">
            {{ inteiro(planilha.lote.totalLinhas) }}
          </dd>
        </div>
        <div>
          <dt class="text-ink-600 dark:text-mist-300">Válidas (enviadas para análise)</dt>
          <dd class="text-lg font-semibold tabular-nums text-ink-900 dark:text-mist-100">
            {{ inteiro(planilha.lote.linhasValidas) }}
          </dd>
        </div>
        <template v-if="planilha.etapaEtl === 'concluido' && planilha.resumoEtl">
          <div>
            <dt class="text-ink-600 dark:text-mist-300">Clientes gravados</dt>
            <dd class="text-lg font-semibold tabular-nums text-ok-600 dark:text-ok-400">
              {{ inteiro(planilha.resumoEtl.totalClientes) }}
            </dd>
          </div>
          <div>
            <dt class="text-ink-600 dark:text-mist-300">Média de faturamento</dt>
            <dd class="text-lg font-semibold tabular-nums text-ink-900 dark:text-mist-100">
              {{ dinheiro(planilha.resumoEtl.media) }}
            </dd>
          </div>
        </template>
        <template v-else>
          <div>
            <dt class="text-ink-600 dark:text-mist-300">Inválidas</dt>
            <dd class="text-lg font-semibold tabular-nums text-ink-900 dark:text-mist-100">
              {{ inteiro(planilha.lote.linhasInvalidas) }}
            </dd>
          </div>
          <div>
            <dt class="text-ink-600 dark:text-mist-300">Status</dt>
            <dd class="mt-1"><StatusBadge :status="planilha.lote.status" /></dd>
          </div>
        </template>
      </dl>

      <RouterLink
        class="mt-4 inline-flex rounded bg-brand-800 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
        :to="{ name: 'dashboard' }"
        v-if="planilha.etapaEtl === 'concluido'"
      >
        Ver gráficos e insights
      </RouterLink>
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
        Estas linhas seguem no envio: ou o valor foi ajustado pelo tratamento, ou ele não está na
        lista de valores conhecidos do sistema (avise o suporte se for um valor novo). A contagem
        por tipo está no <RouterLink class="underline" :to="{ name: 'relatorio' }">Relatório</RouterLink>.
      </p>
      <!-- A lista cresceu quando a padronização passou a ser reportada: numa
           planilha bagunçada são dezenas de linhas. Rolar dentro da própria
           seção evita que ela empurre a prévia e a tabela de erros para fora
           da tela. -->
      <ul class="mt-3 flex max-h-64 flex-col gap-1.5 overflow-auto">
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
      v-if="planilha.quantidadeInvalidas"
    >
      <h2 class="text-sm font-semibold text-ink-900 dark:text-mist-100">
        Linhas com erro ({{ inteiro(planilha.quantidadeInvalidas) }})
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
              v-for="erro in planilha.dadosInvalidos"
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
        {{ inteiro(planilha.quantidadeInvalidas) }} motivos estão na tabela acima — corrija a planilha e
        carregue de novo.
      </p>
    </section>

  </template>
</template>
