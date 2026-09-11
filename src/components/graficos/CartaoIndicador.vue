<script setup>
import { computed } from 'vue'
import FaiscaTendencia from './FaiscaTendencia.vue'

const props = defineProps({
  rotulo: { type: String, required: true },
  valor: { type: String, required: true },
  delta: { type: Number, default: null },
  subirEhBom: { type: Boolean, default: true },
  faisca: { type: Array, default: () => [] },
  periodo: { type: String, default: '' },
})

// A direção da seta e o sinal carregam a variação sozinhos; a cor só reforça.
// Quem não distingue verde de vermelho continua lendo o indicador.
const favoravel = computed(() => {
  if (props.delta === null) return null
  return props.delta >= 0 === props.subirEhBom
})

const textoDelta = computed(() => {
  if (props.delta === null) return ''
  const sinal = props.delta >= 0 ? '+' : '−'
  const absoluto = Math.abs(props.delta).toLocaleString('pt-BR', { maximumFractionDigits: 1 })
  return `${sinal}${absoluto}%`
})
</script>

<template>
  <div class="rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
    <p class="text-sm text-ink-600 dark:text-mist-300">{{ rotulo }}</p>

    <!-- O valor nunca quebra: num cartão estreito "45,2 mil" viraria duas linhas
         e desalinharia esse cartão em relação aos vizinhos. Quem cede espaço é a
         faísca, que é ilustrativa. -->
    <div class="mt-3 flex items-end justify-between gap-3">
      <p class="whitespace-nowrap text-3xl font-semibold leading-none text-ink-900 dark:text-mist-100">
        {{ valor }}
      </p>
      <FaiscaTendencia class="min-w-0" v-if="faisca.length" :pontos="faisca" :largura="84" />
    </div>

    <p class="mt-3 flex items-center gap-1.5 text-xs" v-if="delta !== null">
      <svg
        class="h-3.5 w-3.5 shrink-0"
        :class="favoravel ? 'text-ok-600 dark:text-ok-400' : 'text-erro-600 dark:text-erro-400'"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2.25"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <template v-if="delta >= 0">
          <path d="M12 19V5" />
          <path d="m5 12 7-7 7 7" />
        </template>
        <template v-else>
          <path d="M12 5v14" />
          <path d="m19 12-7 7-7-7" />
        </template>
      </svg>
      <span
        class="font-semibold"
        :class="favoravel ? 'text-ok-600 dark:text-ok-400' : 'text-erro-600 dark:text-erro-400'"
      >
        {{ textoDelta }}
      </span>
      <span class="text-ink-400 dark:text-mist-500">ante os {{ periodo }} anteriores</span>
    </p>
  </div>
</template>
