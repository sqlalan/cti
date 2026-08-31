<script setup>
import { ref } from 'vue'

const menuAberto = ref(false)
</script>

<template>
  <header class="sticky top-0 z-20 border-b border-[#D9E0E6] bg-white dark:border-[#25384E] dark:bg-[#132132]">
    <div class="flex items-center justify-between gap-4 px-8 py-4">
      <RouterLink class="shrink-0" to="/" @click="menuAberto = false">
        <img class="h-10 w-auto dark:hidden" src="../assets/logo-claro.png" alt="Synthetix" width="421" height="120" />
        <img class="hidden h-10 w-auto dark:block" src="../assets/logo-escuro.png" alt="" aria-hidden="true" width="421" height="120" />
      </RouterLink>

      <nav class="hidden text-sm font-medium text-[#5F5F68] dark:text-[#9FB0C2] md:ml-auto md:mr-2 md:flex md:flex-wrap md:justify-end md:gap-x-8">
        <a class="hover:text-[#3F77C4]" href="#plataforma">Plataforma</a>
        <a class="hover:text-[#3F77C4]" href="#sobre">Sobre</a>
        <a class="hover:text-[#3F77C4]" href="#contato">Contato</a>
      </nav>

      <div class="flex shrink-0 items-center gap-2">
        <RouterLink class="hidden shrink-0 items-center justify-center rounded bg-[#194675] px-5 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#11365B] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#3F77C4] sm:inline-flex" to="/login">Entrar</RouterLink>

        <button
          class="inline-flex h-11 w-11 items-center justify-center rounded text-[#194675] dark:text-[#7FB0E8] transition-colors hover:bg-[#F5F7FA] dark:hover:bg-[#1B2C40] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#3F77C4] md:hidden"
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
      class="border-t border-[#D9E0E6] px-8 pb-4 pt-2 text-sm font-medium text-[#5F5F68] dark:border-[#25384E] dark:text-[#9FB0C2] md:hidden"
      id="menu-mobile"
      v-show="menuAberto"
    >
      <a class="block py-2.5 hover:text-[#3F77C4]" href="#plataforma" @click="menuAberto = false">Plataforma</a>
      <a class="block py-2.5 hover:text-[#3F77C4]" href="#sobre" @click="menuAberto = false">Sobre</a>
      <a class="block py-2.5 hover:text-[#3F77C4]" href="#contato" @click="menuAberto = false">Contato</a>
      <RouterLink class="mt-2 inline-flex w-full items-center justify-center rounded bg-[#194675] px-5 py-2.5 font-semibold text-white transition-colors hover:bg-[#11365B] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#3F77C4] sm:hidden" to="/login" @click="menuAberto = false">Entrar</RouterLink>
    </nav>
  </header>
</template>
