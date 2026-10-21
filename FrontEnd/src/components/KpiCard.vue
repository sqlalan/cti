<script setup>
// Cartão de um número do resumo (média, mediana...). Só mostra: o valor já vem formatado.
defineProps({
  titulo: { type: String, required: true },
  valor: { type: String, required: true },
  destaque: Boolean, // realça o cartão principal
})
</script>

<template>
  <div
    class="rounded border bg-white p-4 shadow-cartao dark:bg-night-900"
    :class="destaque ? 'border-brand-500' : 'border-ink-200 dark:border-night-700'"
  >
    <p class="text-xs text-ink-600 dark:text-mist-300">{{ titulo }}</p>
    <p class="mt-1 truncate text-lg font-semibold tabular-nums text-ink-900 dark:text-mist-100" :title="valor">
      {{ valor }}
    </p>
  </div>
</template>
