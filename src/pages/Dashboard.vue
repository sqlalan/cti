<script setup>
// Os indicadores ainda não vêm da API — valores de exemplo até a integração.
const indicadores = [
  { rotulo: 'Arquivos processados', valor: '0' },
  { rotulo: 'Registros analisados', valor: '0' },
  { rotulo: 'Relatórios gerados', valor: '0' },
]
</script>

<template>
  <h1 class="text-2xl font-bold text-ink-900 dark:text-mist-100">Dashboard</h1>
  <p class="mt-2 text-sm text-ink-600 dark:text-mist-300">
    Acompanhe os indicadores da sua operação.
  </p>

  <div class="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
    <div
      class="rounded border border-ink-200 bg-white p-6 dark:border-night-700 dark:bg-night-900"
      v-for="indicador in indicadores"
      :key="indicador.rotulo"
    >
      <p class="text-sm text-ink-600 dark:text-mist-300">{{ indicador.rotulo }}</p>
      <p class="mt-2 text-3xl font-bold text-brand-800 dark:text-brand-300">{{ indicador.valor }}</p>
    </div>
  </div>

  <p class="mt-8 rounded border border-warn-500 bg-warn-500/10 px-4 py-3 text-sm text-ink-900 dark:bg-warn-500/15 dark:text-mist-100" role="status">
    Dados ainda não conectados à API. Integre este painel ao backend antes de publicar.
  </p>
</template>
