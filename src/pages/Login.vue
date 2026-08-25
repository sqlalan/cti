<script setup>
import { ref } from 'vue'

const email = ref('')
const senha = ref('')
const lembrar = ref(false)
const mostrarSenha = ref(false)
const aviso = ref('')

// A autenticação ainda não está conectada a um backend.
function entrar() {
  aviso.value =
    'Autenticação ainda não conectada. Integre este formulário à API antes de publicar.'
}
</script>

<template>
  <div class="grid min-h-screen lg:grid-cols-2">
    <section class="hidden flex-col justify-between bg-[#11365B] p-12 lg:flex">
      <RouterLink class="text-xl font-bold text-white" to="/">Synthetix</RouterLink>
      <div>
        <p class="text-3xl font-bold leading-snug text-white">
          Dados que conectam.<br />Inteligência que transforma.
        </p>
        <p class="mt-6 max-w-md leading-relaxed text-[#D9E0E6]">
          Acesse sua conta para acompanhar indicadores, explorar análises e
          gerar relatórios da sua operação.
        </p>
      </div>
      <p class="text-sm text-[#8C99A6]">© 2026 — Synthetix</p>
    </section>

    <section class="relative flex items-center justify-center bg-white px-6 dark:bg-[#0B1622] py-16">
      <RouterLink
        class="absolute right-6 top-6 inline-flex items-center gap-2 rounded text-sm font-semibold text-[#194675] dark:text-[#7FB0E8] transition-colors hover:text-[#3F77C4] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#3F77C4]"
        to="/"
      >
        <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M19 12H5" /><path d="m12 19-7-7 7-7" /></svg>
        Voltar para o site
      </RouterLink>

      <div class="w-full max-w-sm">
        <RouterLink class="text-xl font-bold text-[#194675] dark:text-[#7FB0E8] lg:hidden" to="/">Synthetix</RouterLink>

        <h1 class="mt-8 text-2xl font-bold text-[#262F3E] dark:text-[#E8EDF2] lg:mt-0">Acessar a plataforma</h1>
        <p class="mt-2 text-sm text-[#5F5F68] dark:text-[#9FB0C2]">
          Entre com as credenciais fornecidas pela sua empresa.
        </p>

        <form class="mt-8" @submit.prevent="entrar">
          <label class="block text-sm font-medium text-[#262F3E] dark:text-[#E8EDF2]" for="email">E-mail</label>
          <input
            class="mt-2 w-full rounded border border-[#D9E0E6] bg-white dark:border-[#25384E] dark:bg-[#0F1B29] px-4 py-2.5 text-sm text-[#262F3E] dark:text-[#E8EDF2] placeholder:text-[#8C99A6] dark:placeholder:text-[#6C7D90] focus:border-[#3F77C4] focus:outline-2 focus:outline-offset-0 focus:outline-[#3F77C4]"
            id="email"
            v-model="email"
            type="email"
            autocomplete="email"
            placeholder="voce@empresa.com.br"
            required
          />

          <label class="mt-5 block text-sm font-medium text-[#262F3E] dark:text-[#E8EDF2]" for="senha">Senha</label>
          <div class="relative mt-2">
            <input
              class="w-full rounded border border-[#D9E0E6] bg-white dark:border-[#25384E] dark:bg-[#0F1B29] py-2.5 pl-4 pr-24 text-sm text-[#262F3E] dark:text-[#E8EDF2] placeholder:text-[#8C99A6] dark:placeholder:text-[#6C7D90] focus:border-[#3F77C4] focus:outline-2 focus:outline-offset-0 focus:outline-[#3F77C4]"
              id="senha"
              v-model="senha"
              :type="mostrarSenha ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="••••••••"
              required
            />
            <button
              class="absolute inset-y-0 right-0 px-4 text-xs font-semibold text-[#194675] dark:text-[#7FB0E8] hover:text-[#3F77C4] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#3F77C4]"
              type="button"
              @click="mostrarSenha = !mostrarSenha"
            >
              {{ mostrarSenha ? 'Ocultar' : 'Mostrar' }}
            </button>
          </div>

          <div class="mt-5 flex flex-wrap items-center justify-between gap-3">
            <label class="flex items-center gap-2 text-sm text-[#5F5F68] dark:text-[#9FB0C2]">
              <input
                class="h-4 w-4 rounded border-[#D9E0E6] accent-[#194675] dark:border-[#25384E]"
                v-model="lembrar"
                type="checkbox"
              />
              Manter conectado
            </label>
            <a class="text-sm font-medium text-[#194675] dark:text-[#7FB0E8] hover:text-[#3F77C4]" href="#">
              Esqueci minha senha
            </a>
          </div>

          <p
            class="mt-6 rounded border border-[#D7AF49] bg-[#D7AF49]/10 dark:bg-[#D7AF49]/15 px-4 py-3 text-sm text-[#262F3E] dark:text-[#E8EDF2]"
            v-if="aviso"
            role="alert"
          >
            {{ aviso }}
          </p>

          <button
            class="mt-6 w-full rounded bg-[#194675] px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-[#11365B] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#3F77C4]"
            type="submit"
          >
            Entrar
          </button>
        </form>

        <p class="mt-8 text-center text-sm text-[#5F5F68] dark:text-[#9FB0C2]">
          Ainda não tem acesso?
          <RouterLink class="font-medium text-[#194675] dark:text-[#7FB0E8] hover:text-[#3F77C4]" to="/#contato">
            Fale com nosso time
          </RouterLink>
        </p>
      </div>
    </section>
  </div>
</template>
