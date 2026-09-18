<script setup>
import { computed } from 'vue'

const props = defineProps({
  pontos: { type: Array, required: true },
  largura: { type: Number, default: 104 },
  altura: { type: Number, default: 30 },
})

const RESPIRO = 4 // impede que o ponto final encoste na borda do desenho

// A faísca é ilustrativa: o número que ela acompanha já está no cartão, por isso
// ela fica marcada como decorativa e não recebe eixo, rótulo nem foco.
const geometria = computed(() => {
  const valores = props.pontos
  if (valores.length < 2) return null

  const minimo = Math.min(...valores)
  const maximo = Math.max(...valores)
  const faixa = maximo - minimo || 1
  const passo = (props.largura - RESPIRO * 2) / (valores.length - 1)
  const util = props.altura - RESPIRO * 2

  const coordenadas = valores.map((valor, i) => [
    RESPIRO + i * passo,
    props.altura - RESPIRO - ((valor - minimo) / faixa) * util,
  ])

  return {
    caminho: coordenadas
      .map(([x, y], i) => `${i ? 'L' : 'M'}${x.toFixed(1)} ${y.toFixed(1)}`)
      .join(' '),
    fim: coordenadas[coordenadas.length - 1],
  }
})
</script>

<template>
  <svg
    class="shrink-0"
    v-if="geometria"
    :width="largura"
    :height="altura"
    :viewBox="`0 0 ${largura} ${altura}`"
    fill="none"
    aria-hidden="true"
  >
    <path
      class="stroke-ink-400 dark:stroke-mist-500"
      :d="geometria.caminho"
      stroke-width="1.5"
      stroke-linecap="round"
      stroke-linejoin="round"
    />
    <circle
      class="fill-serie-600 dark:fill-serie-400"
      :cx="geometria.fim[0]"
      :cy="geometria.fim[1]"
      r="2.5"
    />
  </svg>
</template>
