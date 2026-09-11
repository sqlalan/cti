<script setup>
import { computed, ref } from 'vue'
import { useLargura } from '../../composables/useLargura'
import { formatarData } from '../../dados/painel'
import { formatarNumero, ticksBonitos } from './escala'

const props = defineProps({
  pontos: { type: Array, required: true },
  altura: { type: Number, default: 240 },
  // O ponto plotado sai de `campo`, e não de um nome fixo, para o mesmo
  // gráfico servir a volume diário, contratações por mês ou faturamento.
  campo: { type: String, default: 'registros' },
  unidade: { type: String, default: 'registros' },
  formato: { type: String, default: 'inteiro' }, // inteiro | moeda
  formatoData: { type: String, default: 'curto' }, // curto (dd/mm) | mes (jan/25)
  descricao: { type: String, default: '' },
})

const valorDe = (ponto) => ponto[props.campo] ?? 0

const rotularValor = (valor) =>
  props.formato === 'moeda'
    ? valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
    : formatarNumero(valor)

// A margem inferior reserva a faixa dos rótulos de data dentro da própria
// altura do desenho. Se o eixo ficasse fora dela, o cartão ganharia uma barra
// de rolagem interna de poucos pixels.
const MARGEM = { topo: 18, direita: 16, baixo: 26, esquerda: 48 }

const raiz = ref(null)
const largura = useLargura(raiz)
const indiceAtivo = ref(null)

const escala = computed(() =>
  ticksBonitos(Math.max(...props.pontos.map(valorDe)), 4, { inteiro: props.formato === 'inteiro' }),
)
const areaUtil = computed(() => ({
  largura: Math.max(120, largura.value - MARGEM.esquerda - MARGEM.direita),
  altura: props.altura - MARGEM.topo - MARGEM.baixo,
}))

const posX = (indice) => {
  const passo = areaUtil.value.largura / Math.max(1, props.pontos.length - 1)
  return MARGEM.esquerda + indice * passo
}
const posY = (valor) => MARGEM.topo + (1 - valor / escala.value.topo) * areaUtil.value.altura

const coordenadas = computed(() => props.pontos.map((p, i) => [posX(i), posY(valorDe(p))]))

const caminhoLinha = computed(() =>
  coordenadas.value.map(([x, y], i) => `${i ? 'L' : 'M'}${x.toFixed(1)} ${y.toFixed(1)}`).join(' '),
)

const caminhoArea = computed(() => {
  const base = MARGEM.topo + areaUtil.value.altura
  const [primeiroX] = coordenadas.value[0]
  const [ultimoX] = coordenadas.value[coordenadas.value.length - 1]
  return `${caminhoLinha.value} L${ultimoX.toFixed(1)} ${base} L${primeiroX.toFixed(1)} ${base} Z`
})

// Rotula uma data a cada N pontos para que os textos nunca se toquem — o passo
// vem da largura disponível, não de um número fixo.
const rotulosX = computed(() => {
  const cabem = Math.max(2, Math.floor(areaUtil.value.largura / 72))
  const passo = Math.max(1, Math.ceil(props.pontos.length / cabem))
  const ultimo = props.pontos.length - 1

  const escolhidos = props.pontos
    .map((ponto, i) => ({ ponto, i }))
    .filter(({ i }) => i % passo === 0 || i === ultimo)

  // A data da ponta entra sempre; por isso o rótulo periódico logo antes dela
  // precisa sair quando os dois ficariam colados — senão "09/09" e "10/09" se
  // sobrepõem e viram um borrão ilegível.
  const ESPACO_MINIMO = 58
  return escolhidos.filter(({ i }) => i === ultimo || posX(ultimo) - posX(i) >= ESPACO_MINIMO)
})

const ativo = computed(() =>
  indiceAtivo.value === null ? null : props.pontos[indiceAtivo.value],
)

const resumo = computed(() => {
  const total = props.pontos.reduce((soma, p) => soma + valorDe(p), 0)
  if (props.descricao) return props.descricao
  return `${props.pontos.length} pontos, ${rotularValor(total)} em ${props.unidade} no total.`
})

function indicePorPonteiro(evento) {
  const area = raiz.value.getBoundingClientRect()
  const relativo = evento.clientX - area.left - MARGEM.esquerda
  const passo = areaUtil.value.largura / Math.max(1, props.pontos.length - 1)
  const bruto = Math.round(relativo / passo)
  return Math.max(0, Math.min(props.pontos.length - 1, bruto))
}

function mover(evento) {
  indiceAtivo.value = indicePorPonteiro(evento)
}

// O teclado percorre a mesma série e mostra exatamente a mesma leitura do
// ponteiro, em vez de deixar o valor preso atrás do mouse.
function teclar(evento) {
  const ultimo = props.pontos.length - 1
  const atual = indiceAtivo.value ?? ultimo
  const mapa = {
    ArrowRight: Math.min(ultimo, atual + 1),
    ArrowLeft: Math.max(0, atual - 1),
    Home: 0,
    End: ultimo,
  }
  if (!(evento.key in mapa)) return
  evento.preventDefault()
  indiceAtivo.value = mapa[evento.key]
}

const posicaoDica = computed(() => {
  if (indiceAtivo.value === null) return null
  const x = posX(indiceAtivo.value)
  const metade = 82
  return {
    left: `${Math.max(metade, Math.min(largura.value - metade, x))}px`,
    top: `${Math.max(0, posY(valorDe(props.pontos[indiceAtivo.value])) - 62)}px`,
  }
})
</script>

<template>
  <div class="relative w-full min-w-0" ref="raiz">
    <svg
      class="block rounded focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
      :width="largura"
      :height="altura"
      :viewBox="`0 0 ${largura} ${altura}`"
      fill="none"
      role="img"
      :aria-label="resumo"
      tabindex="0"
      @pointermove="mover"
      @pointerleave="indiceAtivo = null"
      @focus="indiceAtivo = pontos.length - 1"
      @blur="indiceAtivo = null"
      @keydown="teclar"
    >
      <!-- Grade: fio de 1px sólido, um passo fora da superfície. Tracejado leria
           como projeção ou limite, e aqui é só grade. -->
      <g class="stroke-ink-200 dark:stroke-night-700" stroke-width="1">
        <line
          v-for="corte in escala.cortes"
          :key="corte"
          :x1="MARGEM.esquerda"
          :x2="MARGEM.esquerda + areaUtil.largura"
          :y1="posY(corte)"
          :y2="posY(corte)"
        />
      </g>

      <text
        class="fill-ink-400 text-[11px] tabular-nums dark:fill-mist-500"
        v-for="corte in escala.cortes"
        :key="`r-${corte}`"
        :x="MARGEM.esquerda - 10"
        :y="posY(corte) + 4"
        text-anchor="end"
      >
        {{ formatarNumero(corte) }}
      </text>

      <path class="fill-serie-600/10 dark:fill-serie-400/10" :d="caminhoArea" />
      <path
        class="stroke-serie-600 dark:stroke-serie-400"
        :d="caminhoLinha"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />

      <!-- Rótulo direto só na ponta da série. Um número em cada ponto viraria
           ruído e ninguém leria; os demais valores ficam no eixo, na dica e na
           tabela. Some enquanto a mira está ativa para não brigar com ela. -->
      <text
        class="fill-ink-900 stroke-white text-[11px] font-semibold tabular-nums dark:fill-mist-100 dark:stroke-night-900"
        v-if="indiceAtivo === null"
        :x="posX(pontos.length - 1)"
        :y="posY(valorDe(pontos[pontos.length - 1])) - 10"
        text-anchor="end"
        stroke-width="3.5"
        paint-order="stroke"
      >
        {{ rotularValor(valorDe(pontos[pontos.length - 1])) }}
      </text>

      <text
        class="fill-ink-400 text-[11px] dark:fill-mist-500"
        v-for="{ ponto, i } in rotulosX"
        :key="`d-${i}`"
        :x="posX(i)"
        :y="altura - 8"
        :text-anchor="i === 0 ? 'start' : i === pontos.length - 1 ? 'end' : 'middle'"
      >
        {{ formatarData(ponto.data, formatoData) }}
      </text>

      <g v-if="indiceAtivo !== null">
        <line
          class="stroke-ink-400 dark:stroke-mist-500"
          :x1="posX(indiceAtivo)"
          :x2="posX(indiceAtivo)"
          :y1="MARGEM.topo"
          :y2="MARGEM.topo + areaUtil.altura"
          stroke-width="1"
        />
        <!-- O anel na cor da superfície mantém o ponto legível onde ele cruza a
             linha, sem desenhar contorno de dado por cima do dado. -->
        <circle
          class="fill-serie-600 stroke-white dark:fill-serie-400 dark:stroke-night-900"
          :cx="posX(indiceAtivo)"
          :cy="posY(valorDe(pontos[indiceAtivo]))"
          r="4.5"
          stroke-width="2"
        />
      </g>
    </svg>

    <div
      class="pointer-events-none absolute z-10 w-max -translate-x-1/2 rounded border border-ink-200 bg-white px-3 py-2 shadow-cartao dark:border-night-700 dark:bg-night-800"
      v-if="ativo"
      :style="posicaoDica"
    >
      <p class="text-sm font-semibold tabular-nums text-ink-900 dark:text-mist-100">
        {{ rotularValor(valorDe(ativo)) }}<span v-if="formato !== 'moeda'"> {{ unidade }}</span>
      </p>
      <p class="mt-0.5 flex items-center gap-1.5 text-xs text-ink-600 dark:text-mist-300">
        <span class="h-0.5 w-3 rounded-full bg-serie-600 dark:bg-serie-400" aria-hidden="true"></span>
        {{ formatarData(ativo.data, 'longo') }}
      </p>
    </div>

    <p class="sr-only" role="status">
      {{ ativo ? `${formatarData(ativo.data, 'longo')}: ${rotularValor(valorDe(ativo))} ${unidade}` : '' }}
    </p>
  </div>
</template>
