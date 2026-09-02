<script setup>
import { ref } from 'vue'

const arquivos = ref([])
const arrastando = ref(false)
const aviso = ref('')

function adicionar(lista) {
  arquivos.value = [...arquivos.value, ...Array.from(lista)]
  aviso.value = ''
}

function selecionar(evento) {
  adicionar(evento.target.files)
  evento.target.value = ''
}

function soltar(evento) {
  arrastando.value = false
  adicionar(evento.dataTransfer.files)
}

function remover(indice) {
  arquivos.value.splice(indice, 1)
}

function formatarTamanho(bytes) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

// O envio ainda não está conectado a um backend.
function enviar() {
  aviso.value =
    'Envio ainda não conectado. Integre este formulário à API antes de publicar.'
}
</script>

<template>
  <h1 class="text-2xl font-bold text-ink-900 dark:text-mist-100">Upload</h1>
  <p class="mt-2 text-sm text-ink-600 dark:text-mist-300">
    Envie planilhas ou arquivos CSV para processamento.
  </p>

  <label
    class="mt-8 flex cursor-pointer flex-col items-center justify-center rounded border-2 border-dashed px-6 py-14 text-center transition-colors"
    :class="arrastando
      ? 'border-brand-500 bg-brand-500/5'
      : 'border-ink-200 bg-white dark:border-night-700 dark:bg-night-900'"
    @dragover.prevent="arrastando = true"
    @dragleave.prevent="arrastando = false"
    @drop.prevent="soltar"
  >
    <svg class="h-10 w-10 text-brand-800 dark:text-brand-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
      <path d="M17 8l-5-5-5 5" />
      <path d="M12 3v13" />
    </svg>
    <p class="mt-4 text-sm font-medium text-ink-900 dark:text-mist-100">
      Arraste os arquivos ou clique para selecionar
    </p>
    <p class="mt-1 text-xs text-ink-400 dark:text-mist-500">CSV, XLS ou XLSX</p>
    <input
      class="sr-only"
      type="file"
      multiple
      accept=".csv,.xls,.xlsx"
      @change="selecionar"
    />
  </label>

  <ul class="mt-6" v-if="arquivos.length">
    <li
      class="flex items-center justify-between gap-4 border-b border-ink-200 py-3 dark:border-night-700"
      v-for="(arquivo, indice) in arquivos"
      :key="`${arquivo.name}-${indice}`"
    >
      <span class="min-w-0 truncate text-sm text-ink-900 dark:text-mist-100">{{ arquivo.name }}</span>
      <span class="flex shrink-0 items-center gap-4">
        <span class="text-xs text-ink-400 dark:text-mist-500">{{ formatarTamanho(arquivo.size) }}</span>
        <button
          class="text-sm font-medium text-brand-800 hover:text-brand-500 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:text-brand-300"
          type="button"
          @click="remover(indice)"
        >
          Remover
        </button>
      </span>
    </li>
  </ul>

  <p
    class="mt-6 rounded border border-warn-500 bg-warn-500/10 px-4 py-3 text-sm text-ink-900 dark:bg-warn-500/15 dark:text-mist-100"
    v-if="aviso"
    role="alert"
  >
    {{ aviso }}
  </p>

  <button
    class="mt-6 rounded bg-brand-800 px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-brand-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 disabled:cursor-not-allowed disabled:opacity-50"
    type="button"
    :disabled="!arquivos.length"
    @click="enviar"
  >
    Enviar arquivos
  </button>
</template>
