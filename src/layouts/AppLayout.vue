<script setup>
import { ref } from 'vue'

const menuAberto = ref(false)

const itens = [
  { to: '/sidebar/dashboard', rotulo: 'Dashboard' },
  { to: '/sidebar/upload', rotulo: 'Upload' },
]
</script>

<template>
  <div class="flex min-h-screen bg-[#F5F7FA] dark:bg-[#0B1622]">
    <aside
      class="fixed inset-y-0 left-0 z-30 w-64 shrink-0 border-r border-[#D9E0E6] bg-white p-6 transition-transform dark:border-[#25384E] dark:bg-[#132132] lg:static lg:translate-x-0"
      :class="menuAberto ? 'translate-x-0' : '-translate-x-full'"
    >
      <RouterLink class="block" to="/">
        <img class="h-9 w-auto dark:hidden" src="../assets/logo-claro.png" alt="Synthetix" width="421" height="120" />
        <img class="hidden h-9 w-auto dark:block" src="../assets/logo-escuro.png" alt="" aria-hidden="true" width="421" height="120" />
      </RouterLink>

      <nav class="mt-10 flex flex-col gap-1">
        <RouterLink
          class="rounded px-4 py-2.5 text-sm font-medium text-[#5F5F68] transition-colors hover:bg-[#F5F7FA] hover:text-[#194675] dark:text-[#9FB0C2] dark:hover:bg-[#1B2C40] dark:hover:text-[#7FB0E8]"
          active-class="bg-[#194675] text-white hover:bg-[#11365B] hover:text-white dark:bg-[#194675] dark:text-white"
          v-for="item in itens"
          :key="item.to"
          :to="item.to"
          @click="menuAberto = false"
        >
          {{ item.rotulo }}
        </RouterLink>
      </nav>

      <RouterLink
        class="mt-10 block text-sm font-medium text-[#194675] hover:text-[#3F77C4] dark:text-[#7FB0E8]"
        to="/"
      >
        Voltar para o site
      </RouterLink>
    </aside>

    <div
      class="fixed inset-0 z-20 bg-black/40 lg:hidden"
      v-if="menuAberto"
      @click="menuAberto = false"
    ></div>

    <div class="flex min-w-0 flex-1 flex-col">
      <header class="flex items-center gap-3 border-b border-[#D9E0E6] bg-white px-6 py-4 dark:border-[#25384E] dark:bg-[#132132] lg:hidden">
        <button
          class="inline-flex h-11 w-11 items-center justify-center rounded text-[#194675] transition-colors hover:bg-[#F5F7FA] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#3F77C4] dark:text-[#7FB0E8] dark:hover:bg-[#1B2C40]"
          type="button"
          :aria-expanded="menuAberto"
          :aria-label="menuAberto ? 'Fechar menu' : 'Abrir menu'"
          @click="menuAberto = !menuAberto"
        >
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" aria-hidden="true">
            <path d="M4 7h16" />
            <path d="M4 12h16" />
            <path d="M4 17h16" />
          </svg>
        </button>
        <span class="font-bold text-[#194675] dark:text-[#7FB0E8]">Synthetix</span>
      </header>

      <main class="flex-1 px-6 py-8 lg:px-10">
        <RouterView />
      </main>
    </div>
  </div>
</template>
