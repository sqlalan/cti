<script setup>
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const route = useRoute()
const auth = useAuthStore()
</script>

<template>
  <main class="flex min-h-screen items-center justify-center bg-white px-6 py-16 dark:bg-night-950">
    <div class="max-w-md text-center">
      <p class="text-sm font-semibold text-brand-800 dark:text-brand-300">Erro 404</p>
      <h1 class="mt-2 text-2xl font-bold text-ink-900 dark:text-mist-100">Página não encontrada</h1>
      <p class="mt-3 break-words text-sm text-ink-600 dark:text-mist-300">
        O endereço <code class="text-xs">{{ route.fullPath }}</code> não existe. Confira o link ou volte para uma
        página conhecida.
      </p>
      <div class="mt-6 flex flex-wrap justify-center gap-3">
        <RouterLink
          class="rounded bg-brand-800 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-brand-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          :to="auth.logado ? { name: 'upload' } : { name: 'login' }"
        >
          {{ auth.logado ? 'Ir para o Upload' : 'Entrar' }}
        </RouterLink>
        <RouterLink
          class="rounded border border-ink-200 px-5 py-2.5 text-sm font-semibold text-brand-800 transition-colors hover:border-brand-500 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:border-night-700 dark:text-brand-300"
          :to="{ name: 'home' }"
        >
          Página inicial
        </RouterLink>
      </div>
    </div>
  </main>
</template>
