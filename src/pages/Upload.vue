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
  <h1 class="text-2xl font-bold text-[#262F3E] dark:text-[#E8EDF2]">Upload</h1>
  <p class="mt-2 text-sm text-[#5F5F68] dark:text-[#9FB0C2]">
    Envie planilhas ou arquivos CSV para processamento.
  </p>

  <label
    class="mt-8 flex cursor-pointer flex-col items-center justify-center rounded border-2 border-dashed px-6 py-14 text-center transition-colors"
    :class="arrastando
      ? 'border-[#3F77C4] bg-[#3F77C4]/5'
      : 'border-[#D9E0E6] bg-white dark:border-[#25384E] dark:bg-[#132132]'"
    @dragover.prevent="arrastando = true"
    @dragleave.prevent="arrastando = false"
    @drop.prevent="soltar"
  >
    <svg class="h-10 w-10 text-[#194675] dark:text-[#7FB0E8]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
      <path d="M17 8l-5-5-5 5" />
      <path d="M12 3v13" />
    </svg>
    <p class="mt-4 text-sm font-medium text-[#262F3E] dark:text-[#E8EDF2]">
      Arraste os arquivos ou clique para selecionar
    </p>
    <p class="mt-1 text-xs text-[#8C99A6] dark:text-[#6C7D90]">CSV, XLS ou XLSX</p>
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
      class="flex items-center justify-between gap-4 border-b border-[#D9E0E6] py-3 dark:border-[#25384E]"
      v-for="(arquivo, indice) in arquivos"
      :key="`${arquivo.name}-${indice}`"
    >
      <span class="min-w-0 truncate text-sm text-[#262F3E] dark:text-[#E8EDF2]">{{ arquivo.name }}</span>
      <span class="flex shrink-0 items-center gap-4">
        <span class="text-xs text-[#8C99A6] dark:text-[#6C7D90]">{{ formatarTamanho(arquivo.size) }}</span>
        <button
          class="text-sm font-medium text-[#194675] hover:text-[#3F77C4] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#3F77C4] dark:text-[#7FB0E8]"
          type="button"
          @click="remover(indice)"
        >
          Remover
        </button>
      </span>
    </li>
  </ul>

  <p
    class="mt-6 rounded border border-[#D7AF49] bg-[#D7AF49]/10 px-4 py-3 text-sm text-[#262F3E] dark:bg-[#D7AF49]/15 dark:text-[#E8EDF2]"
    v-if="aviso"
    role="alert"
  >
    {{ aviso }}
  </p>

  <button
    class="mt-6 rounded bg-[#194675] px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-[#11365B] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#3F77C4] disabled:cursor-not-allowed disabled:opacity-50"
    type="button"
    :disabled="!arquivos.length"
    @click="enviar"
  >
    Enviar arquivos
  </button>
</template>
