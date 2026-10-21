<script setup>
import { computed } from 'vue'

// Etiqueta do status do lote (upload_lote.status).
const props = defineProps({
  status: { type: String, required: true },
})

const ESTILO = {
  AGUARDANDO_ETL: ['Aguardando o Python', 'border-ink-200 text-ink-600 dark:border-night-700 dark:text-mist-300'],
  PROCESSANDO: ['Processando', 'border-brand-500/50 text-brand-800 dark:text-brand-300'],
  PROCESSADO: ['Processado', 'border-ok-600/50 text-ok-600 dark:border-ok-400/50 dark:text-ok-400'],
  ERRO: ['Erro', 'border-erro-600/50 text-erro-600 dark:border-erro-400/50 dark:text-erro-400'],
  REJEITADO: ['Rejeitado', 'border-erro-600/50 text-erro-600 dark:border-erro-400/50 dark:text-erro-400'],
}

const estilo = computed(() => ESTILO[props.status] ?? [props.status, ESTILO.AGUARDANDO_ETL[1]])
</script>

<template>
  <span class="inline-flex rounded-full border px-2 py-0.5 text-xs font-semibold" :class="estilo[1]">
    {{ estilo[0] }}
  </span>
</template>
