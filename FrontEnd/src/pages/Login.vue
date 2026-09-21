<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const usuario = ref('')
const senha = ref('')
const mensagem = ref('')
const mostrarSenha = ref(false)

const router = useRouter()
const auth = useAuthStore()

function entrar() {
  mensagem.value = ''
  // trim só no usuário: "   " passaria no teste de campo vazio da store e
  // entraria como nome de ninguém. Senha vai como foi digitada — espaço pode
  // fazer parte dela.
  if (auth.login(usuario.value.trim(), senha.value)) {
    // Pelo nome, e não por '/upload' como no slide: neste projeto a tela mora
    // em /sidebar/upload, e o nome continua valendo se o caminho mudar.
    router.push({ name: 'upload' })
  } else {
    mensagem.value = 'Preencha usuário e senha.'
  }
}
</script>

<template>
  <div class="grid min-h-screen lg:grid-cols-2">
    <section class="hidden flex-col justify-between bg-brand-900 p-12 lg:flex">
      <RouterLink class="text-xl font-bold text-white" to="/">Synthetix</RouterLink>
      <div>
        <p class="text-3xl font-bold leading-snug text-white">
          Dados que conectam.<br />Inteligência que transforma.
        </p>
        <p class="mt-6 max-w-md leading-relaxed text-ink-200">
          Acesse sua conta para acompanhar indicadores, explorar análises e
          gerar relatórios da sua operação.
        </p>
      </div>
      <p class="text-sm text-ink-400">© 2026 — Synthetix</p>
    </section>

    <section class="relative flex items-center justify-center bg-white px-6 dark:bg-night-950 py-16">
      <RouterLink
        class="absolute right-6 top-6 inline-flex items-center gap-2 rounded text-sm font-semibold text-brand-800 dark:text-brand-300 transition-colors hover:text-brand-500 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-500"
        to="/"
      >
        <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M19 12H5" /><path d="m12 19-7-7 7-7" /></svg>
        Voltar para o site
      </RouterLink>

      <div class="w-full max-w-sm">
        <RouterLink class="text-xl font-bold text-brand-800 dark:text-brand-300 lg:hidden" to="/">Synthetix</RouterLink>

        <h1 class="mt-8 text-2xl font-bold text-ink-900 dark:text-mist-100 lg:mt-0">Acessar a plataforma</h1>
        <p class="mt-2 text-sm text-ink-600 dark:text-mist-300">
          Entre com as credenciais fornecidas pela sua empresa.
        </p>

        <!-- Sem `required` nos campos: a regra de campo vazio mora na store, e
             o navegador barrando antes dela esconderia a mensagem da aula e
             deixaria duas validações dizendo coisas diferentes. -->
        <form class="mt-8" @submit.prevent="entrar">
          <label class="block text-sm font-medium text-ink-900 dark:text-mist-100" for="usuario">Usuário</label>
          <input
            class="mt-2 w-full rounded border border-ink-200 bg-white dark:border-night-700 dark:bg-night-900 px-4 py-2.5 text-sm text-ink-900 dark:text-mist-100 placeholder:text-ink-400 dark:placeholder:text-mist-500 focus:border-brand-500 focus:outline-2 focus:outline-offset-0 focus:outline-brand-500"
            id="usuario"
            v-model="usuario"
            type="text"
            autocomplete="username"
            placeholder="seu.usuario"
          />

          <label class="mt-5 block text-sm font-medium text-ink-900 dark:text-mist-100" for="senha">Senha</label>
          <div class="relative mt-2">
            <input
              class="w-full rounded border border-ink-200 bg-white dark:border-night-700 dark:bg-night-900 py-2.5 pl-4 pr-24 text-sm text-ink-900 dark:text-mist-100 placeholder:text-ink-400 dark:placeholder:text-mist-500 focus:border-brand-500 focus:outline-2 focus:outline-offset-0 focus:outline-brand-500"
              id="senha"
              v-model="senha"
              :type="mostrarSenha ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="••••••••"
            />
            <button
              class="absolute inset-y-0 right-0 px-4 text-xs font-semibold text-brand-800 dark:text-brand-300 hover:text-brand-500 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
              type="button"
              @click="mostrarSenha = !mostrarSenha"
            >
              {{ mostrarSenha ? 'Ocultar' : 'Mostrar' }}
            </button>
          </div>

          <!-- "Manter conectado" saiu: pela aula o login sempre persiste no
               localStorage, e uma caixa que não muda nada engana quem marca. -->
          <div class="mt-5 flex justify-end">
            <a class="text-sm font-medium text-brand-800 dark:text-brand-300 hover:text-brand-500" href="#">
              Esqueci minha senha
            </a>
          </div>

          <p
            class="mt-6 rounded border border-warn-500 bg-warn-500/10 dark:bg-warn-500/15 px-4 py-3 text-sm text-ink-900 dark:text-mist-100"
            v-if="mensagem"
            role="alert"
          >
            {{ mensagem }}
          </p>

          <button
            class="mt-6 w-full rounded bg-brand-800 px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-brand-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
            type="submit"
          >
            Entrar
          </button>
        </form>

        <p class="mt-8 text-center text-sm text-ink-600 dark:text-mist-300">
          Ainda não tem acesso?
          <RouterLink class="font-medium text-brand-800 dark:text-brand-300 hover:text-brand-500" to="/#contato">
            Fale com nosso time
          </RouterLink>
        </p>
      </div>
    </section>
  </div>
</template>
