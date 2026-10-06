<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const form = reactive({ nome: '', email: '', consultor: '', matricula: '', senha: '', confirmacao: '' })
const mensagem = ref('')
const mostrarSenha = ref(false)
const enviando = ref(false)

const router = useRouter()
const auth = useAuthStore()

// Só o que o back-end não tem como checar (confirmação) ou que poupa uma ida ao
// servidor. As regras de verdade (e-mail único, um usuário por consultor) são do Service.
function problemaNoFormulario() {
  if (!form.nome.trim() || !form.email.trim() || !form.consultor.trim() || !form.senha) {
    return 'Preencha todos os campos.'
  }
  // Mesma regra do back-end (UsuarioRequest): 8 a 72 caracteres, com letras e números.
  if (!/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/.test(form.senha)) {
    return 'A senha deve ter de 8 a 72 caracteres, com letras e números.'
  }
  if (form.senha !== form.confirmacao) {
    return 'As senhas não conferem.'
  }
  return ''
}

async function cadastrar() {
  mensagem.value = problemaNoFormulario()
  if (mensagem.value) return

  enviando.value = true
  mensagem.value = await auth.cadastrar({
    nome: form.nome.trim(),
    email: form.email.trim(),
    consultor: form.consultor.trim(),
    matricula: form.matricula.trim(),
    senha: form.senha,
  })
  enviando.value = false
  if (!mensagem.value) {
    router.push({ name: 'upload' })
  }
}

const classeCampo =
  'mt-2 w-full rounded border border-ink-200 bg-white dark:border-night-700 dark:bg-night-900 px-4 py-2.5 text-sm text-ink-900 dark:text-mist-100 placeholder:text-ink-400 dark:placeholder:text-mist-500 focus:border-brand-500 focus:outline-2 focus:outline-offset-0 focus:outline-brand-500'
const classeRotulo = 'mt-5 block text-sm font-medium text-ink-900 dark:text-mist-100'
</script>

<template>
  <div class="grid min-h-screen lg:grid-cols-2">
    <section class="hidden flex-col justify-between bg-brand-900 p-12 lg:flex">
      <RouterLink class="text-xl font-bold text-white" :to="{ name: 'home' }">Synthetix</RouterLink>
      <div>
        <p class="text-3xl font-bold leading-snug text-white">
          Sua carteira de clientes<br />em um só lugar.
        </p>
        <p class="mt-6 max-w-md leading-relaxed text-ink-200">
          Crie sua conta de consultor para enviar planilhas, acompanhar
          indicadores e gerar relatórios dos seus clientes.
        </p>
      </div>
      <p class="text-sm text-ink-400">© 2026 — Synthetix</p>
    </section>

    <section class="relative flex items-center justify-center bg-white px-6 dark:bg-night-950 py-16">
      <RouterLink
        class="absolute right-6 top-6 inline-flex items-center gap-2 rounded text-sm font-semibold text-brand-800 dark:text-brand-300 transition-colors hover:text-brand-500 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-500"
        :to="{ name: 'login' }"
      >
        <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M19 12H5" /><path d="m12 19-7-7 7-7" /></svg>
        Voltar para o login
      </RouterLink>

      <div class="w-full max-w-sm">
        <RouterLink class="text-xl font-bold text-brand-800 dark:text-brand-300 lg:hidden" :to="{ name: 'home' }">Synthetix</RouterLink>

        <h1 class="mt-8 text-2xl font-bold text-ink-900 dark:text-mist-100 lg:mt-0">Criar conta</h1>
        <p class="mt-2 text-sm text-ink-600 dark:text-mist-300">
          Cada consultor tem uma conta. Use seu nome como aparece na planilha.
        </p>

        <!-- Sem `required`, como no Login: as mensagens vêm de um lugar só. -->
        <form class="mt-6" @submit.prevent="cadastrar">
          <label class="block text-sm font-medium text-ink-900 dark:text-mist-100" for="nome">Seu nome</label>
          <input :class="classeCampo" id="nome" v-model="form.nome" type="text" autocomplete="name" placeholder="Ana Lima" />

          <label :class="classeRotulo" for="email">E-mail</label>
          <input :class="classeCampo" id="email" v-model="form.email" type="email" autocomplete="email" placeholder="voce@empresa.com" />

          <label :class="classeRotulo" for="consultor">Consultor na planilha</label>
          <input :class="classeCampo" id="consultor" v-model="form.consultor" type="text" autocomplete="off" placeholder="Ana Lima" aria-describedby="consultor-ajuda" />
          <p class="mt-1.5 text-xs text-ink-600 dark:text-mist-400" id="consultor-ajuda">
            Exatamente como está na coluna de consultor. Maiúsculas não importam.
          </p>

          <label :class="classeRotulo" for="matricula">
            Matrícula <span class="font-normal text-ink-600 dark:text-mist-400">(opcional)</span>
          </label>
          <input :class="classeCampo" id="matricula" v-model="form.matricula" type="text" autocomplete="off" maxlength="20" placeholder="CT-001" />

          <label :class="classeRotulo" for="senha">Senha</label>
          <div class="relative">
            <input
              :class="[classeCampo, 'pr-24']"
              id="senha"
              v-model="form.senha"
              :type="mostrarSenha ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="8 a 72, com letras e números"
            />
            <button
              class="absolute inset-y-0 right-0 mt-2 px-4 text-xs font-semibold text-brand-800 dark:text-brand-300 hover:text-brand-500 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
              type="button"
              @click="mostrarSenha = !mostrarSenha"
            >
              {{ mostrarSenha ? 'Ocultar' : 'Mostrar' }}
            </button>
          </div>

          <label :class="classeRotulo" for="confirmacao">Confirme a senha</label>
          <input
            :class="classeCampo"
            id="confirmacao"
            v-model="form.confirmacao"
            :type="mostrarSenha ? 'text' : 'password'"
            autocomplete="new-password"
          />

          <p
            class="mt-6 rounded border border-warn-500 bg-warn-500/10 dark:bg-warn-500/15 px-4 py-3 text-sm text-ink-900 dark:text-mist-100"
            v-if="mensagem"
            role="alert"
          >
            {{ mensagem }}
          </p>

          <button
            class="mt-6 w-full rounded bg-brand-800 px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-brand-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 disabled:cursor-wait disabled:opacity-70"
            type="submit"
            :disabled="enviando"
          >
            {{ enviando ? 'Criando conta…' : 'Criar conta' }}
          </button>
        </form>

        <p class="mt-8 text-center text-sm text-ink-600 dark:text-mist-300">
          Já tem conta?
          <RouterLink class="font-medium text-brand-800 dark:text-brand-300 hover:text-brand-500" :to="{ name: 'login' }">
            Entrar
          </RouterLink>
        </p>
      </div>
    </section>
  </div>
</template>
