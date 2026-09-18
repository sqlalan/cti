<script setup>
import { ref, useId } from 'vue'

defineProps({
  titulo: { type: String, required: true },
  descricao: { type: String, default: '' },
})

// Todo gráfico precisa de um gêmeo em tabela: a dica que aparece no ponteiro
// enriquece a leitura, mas nunca pode ser o único caminho até o valor.
const mostrarTabela = ref(false)
const idPainel = useId()
</script>

<template>
  <!-- min-w-0 é obrigatório aqui: item de grid nasce com min-width:auto, então
       um gráfico desenhado em largura fixa empurraria o cartão para além da
       tela e o medidor leria essa largura inflada, sem nunca encolher. -->
  <section class="min-w-0 rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div class="min-w-0">
        <h2 class="text-sm font-semibold text-ink-900 dark:text-mist-100">{{ titulo }}</h2>
        <p class="mt-1 text-xs text-ink-600 dark:text-mist-300" v-if="descricao">{{ descricao }}</p>
      </div>

      <div class="flex shrink-0 rounded border border-ink-200 p-0.5 dark:border-night-700" role="group" aria-label="Formato de exibição">
        <button
          class="rounded px-2.5 py-1 text-xs font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          :class="!mostrarTabela
            ? 'bg-brand-800 text-white'
            : 'text-ink-600 hover:text-brand-800 dark:text-mist-300 dark:hover:text-brand-300'"
          type="button"
          :aria-pressed="!mostrarTabela"
          :aria-controls="idPainel"
          @click="mostrarTabela = false"
        >
          Gráfico
        </button>
        <button
          class="rounded px-2.5 py-1 text-xs font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          :class="mostrarTabela
            ? 'bg-brand-800 text-white'
            : 'text-ink-600 hover:text-brand-800 dark:text-mist-300 dark:hover:text-brand-300'"
          type="button"
          :aria-pressed="mostrarTabela"
          :aria-controls="idPainel"
          @click="mostrarTabela = true"
        >
          Tabela
        </button>
      </div>
    </div>

    <div class="mt-5" :id="idPainel">
      <slot name="tabela" v-if="mostrarTabela" />
      <slot v-else />
    </div>
  </section>
</template>
