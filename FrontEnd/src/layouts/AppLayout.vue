<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import Notificacoes from '../components/Notificacoes.vue'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const auth = useAuthStore()
const menuAberto = ref(false)

const itens = [
  { to: '/sidebar/dashboard', rotulo: 'Dashboard' },
  { to: '/sidebar/upload', rotulo: 'Upload' },
  { to: '/sidebar/relatorio', rotulo: 'Relatórios' },
]

// A sessão é descartada ANTES de navegar: sair sem limpar deixaria o
// localStorage marcado como logado, e a próxima pessoa no mesmo navegador
// entraria na sessão anterior. Quando houver token de API, ele sai aqui também.
function sair() {
  menuAberto.value = false
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="flex min-h-screen bg-ink-100 dark:bg-night-950">
    <aside
      class="fixed inset-y-0 left-0 z-30 flex w-64 shrink-0 flex-col border-r border-ink-200 bg-white p-6 transition-transform dark:border-night-700 dark:bg-night-900 lg:sticky lg:inset-y-auto lg:top-0 lg:h-screen lg:translate-x-0 lg:overflow-y-auto"
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

      <p class="mt-auto truncate px-1 pb-3 text-sm text-ink-600 dark:text-mist-300">
        Conectado como
        <span class="font-semibold text-ink-900 dark:text-mist-100">{{ auth.nomeExibicao }}</span>
      </p>

      <button
        class="flex w-full items-center justify-center gap-2 rounded border border-ink-200 px-4 py-2.5 text-sm font-medium text-ink-600 transition-colors hover:border-brand-500/50 hover:text-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:border-night-700 dark:text-mist-300 dark:hover:text-brand-300"
        type="button"
        @click="sair"
      >
        <svg
          class="h-4 w-4 shrink-0"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.75"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
          <path d="m16 17 5-5-5-5" />
          <path d="M21 12H9" />
        </svg>
        Sair
      </button>
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

    <!-- Fica no layout, e não dentro da página de upload, por dois motivos: a
         região viva precisa existir antes do primeiro aviso nascer, e qualquer
         tela da sidebar passa a poder avisar sem montar nada. -->
    <Notificacoes />
  </div>
</template>
