<script setup>
import { ref } from 'vue'

const menuAberto = ref(false)

const itens = [
  { to: '/sidebar/dashboard', rotulo: 'Dashboard' },
  { to: '/sidebar/upload', rotulo: 'Upload' },
]
</script>

<template>
  <div class="flex min-h-screen bg-ink-100 dark:bg-night-950">
    <aside
      class="fixed inset-y-0 left-0 z-30 w-64 shrink-0 border-r border-ink-200 bg-white p-6 transition-transform dark:border-night-700 dark:bg-night-900 lg:static lg:translate-x-0"
      :class="menuAberto ? 'translate-x-0' : '-translate-x-full'"
    >
      <RouterLink class="block" to="/">
        <img class="h-9 w-auto dark:hidden" src="../assets/logo-claro.png" alt="Synthetix" width="421" height="120" />
        <img class="hidden h-9 w-auto dark:block" src="../assets/logo-escuro.png" alt="" aria-hidden="true" width="421" height="120" />
      </RouterLink>

      <nav class="mt-10 flex flex-col gap-1">
        <RouterLink
          class="rounded px-4 py-2.5 text-sm font-medium text-ink-600 transition-colors hover:bg-ink-100 hover:text-brand-800 dark:text-mist-300 dark:hover:bg-night-800 dark:hover:text-brand-300"
          active-class="bg-brand-800 text-white hover:bg-brand-900 hover:text-white dark:bg-brand-800 dark:text-white dark:hover:bg-brand-700"
          v-for="item in itens"
          :key="item.to"
          :to="item.to"
          @click="menuAberto = false"
        >
          {{ item.rotulo }}
        </RouterLink>
      </nav>

      <RouterLink
        class="mt-10 block text-sm font-medium text-brand-800 hover:text-brand-500 dark:text-brand-300"
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
      <header class="flex items-center gap-3 border-b border-ink-200 bg-white px-6 py-4 dark:border-night-700 dark:bg-night-900 lg:hidden">
        <button
          class="inline-flex h-11 w-11 items-center justify-center rounded text-brand-800 transition-colors hover:bg-ink-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:text-brand-300 dark:hover:bg-night-800"
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
        <span class="font-bold text-brand-800 dark:text-brand-300">Synthetix</span>
      </header>

      <main class="flex-1 px-6 py-8 lg:px-10">
        <RouterView />
      </main>
    </div>
  </div>
</template>
