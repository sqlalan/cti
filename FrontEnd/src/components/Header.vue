<script setup>
import { ref } from 'vue'

const menuAberto = ref(false)
</script>

<template>
  <header class="sticky top-0 z-20 border-b border-ink-200 bg-white dark:border-night-700 dark:bg-night-900">
    <div class="flex items-center justify-between gap-4 px-8 py-4">
      <RouterLink class="shrink-0" to="/" @click="menuAberto = false">
        <img class="h-10 w-auto dark:hidden" src="../assets/logo-claro.png" alt="Synthetix" width="421" height="120" />
        <img class="hidden h-10 w-auto dark:block" src="../assets/logo-escuro.png" alt="" aria-hidden="true" width="421" height="120" />
      </RouterLink>

      <nav class="hidden text-sm font-medium text-ink-600 dark:text-mist-300 md:ml-auto md:mr-2 md:flex md:flex-wrap md:justify-end md:gap-x-8">
        <a class="hover:text-brand-500" href="#plataforma">Plataforma</a>
        <a class="hover:text-brand-500" href="#contato">Contato</a>
      </nav>

      <div class="flex shrink-0 items-center gap-2">
        <RouterLink class="hidden shrink-0 items-center justify-center rounded bg-brand-800 px-5 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 sm:inline-flex" to="/login">Entrar</RouterLink>

        <button
          class="inline-flex h-11 w-11 items-center justify-center rounded text-brand-800 dark:text-brand-300 transition-colors hover:bg-ink-100 dark:hover:bg-night-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 md:hidden"
          type="button"
          :aria-expanded="menuAberto"
          aria-controls="menu-mobile"
          :aria-label="menuAberto ? 'Fechar menu' : 'Abrir menu'"
          @click="menuAberto = !menuAberto"
        >
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" aria-hidden="true">
            <path v-if="menuAberto" d="M6 6l12 12M18 6L6 18" />
            <template v-else>
              <path d="M4 7h16" />
              <path d="M4 12h16" />
              <path d="M4 17h16" />
            </template>
          </svg>
        </button>
      </div>
    </div>

    <nav
      class="border-t border-ink-200 px-8 pb-4 pt-2 text-sm font-medium text-ink-600 dark:border-night-700 dark:text-mist-300 md:hidden"
      id="menu-mobile"
      v-show="menuAberto"
    >
      <a class="block py-2.5 hover:text-brand-500" href="#plataforma" @click="menuAberto = false">Plataforma</a>
      <a class="block py-2.5 hover:text-brand-500" href="#contato" @click="menuAberto = false">Contato</a>
      <RouterLink class="mt-2 inline-flex w-full items-center justify-center rounded bg-brand-800 px-5 py-2.5 font-semibold text-white transition-colors hover:bg-brand-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 sm:hidden" to="/login" @click="menuAberto = false">Entrar</RouterLink>
    </nav>
  </header>
</template>
