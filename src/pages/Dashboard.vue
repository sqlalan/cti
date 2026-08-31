<script setup>
// Os indicadores ainda não vêm da API — valores de exemplo até a integração.
const indicadores = [
  { rotulo: 'Arquivos processados', valor: '0' },
  { rotulo: 'Registros analisados', valor: '0' },
  { rotulo: 'Relatórios gerados', valor: '0' },
]
</script>

<template>
  <h1 class="text-2xl font-bold text-[#262F3E] dark:text-[#E8EDF2]">Dashboard</h1>
  <p class="mt-2 text-sm text-[#5F5F68] dark:text-[#9FB0C2]">
    Acompanhe os indicadores da sua operação.
  </p>

  <div class="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
    <div
      class="rounded border border-[#D9E0E6] bg-white p-6 dark:border-[#25384E] dark:bg-[#132132]"
      v-for="indicador in indicadores"
      :key="indicador.rotulo"
    >
      <p class="text-sm text-[#5F5F68] dark:text-[#9FB0C2]">{{ indicador.rotulo }}</p>
      <p class="mt-2 text-3xl font-bold text-[#194675] dark:text-[#7FB0E8]">{{ indicador.valor }}</p>
    </div>
  </div>

  <p class="mt-8 rounded border border-[#D7AF49] bg-[#D7AF49]/10 px-4 py-3 text-sm text-[#262F3E] dark:bg-[#D7AF49]/15 dark:text-[#E8EDF2]" role="status">
    Dados ainda não conectados à API. Integre este painel ao backend antes de publicar.
  </p>
</template>
