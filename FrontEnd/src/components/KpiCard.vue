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
    <!-- Quebra em vez de cortar: no celular "R$ 70.510.000,00" não cabe numa linha. -->
    <p class="mt-1 text-base font-semibold tabular-nums break-words text-ink-900 sm:text-lg dark:text-mist-100">
      {{ valor }}
    </p>
  </div>
</template>
