<script setup>
import { computed } from 'vue'

// Situação da conta na tela de usuários, com a marca de administrador ao lado.
const props = defineProps({
  status: { type: String, required: true },
  perfil: { type: String, default: 'USER' },
})

const ESTILO = {
  PENDENTE: ['Pendente', 'border-alerta-600/50 text-alerta-600 dark:border-alerta-400/50 dark:text-alerta-400'],
  APROVADO: ['Aprovada', 'border-ok-600/50 text-ok-600 dark:border-ok-400/50 dark:text-ok-400'],
  BLOQUEADO: ['Bloqueada', 'border-erro-600/50 text-erro-600 dark:border-erro-400/50 dark:text-erro-400'],
}

const estilo = computed(() => ESTILO[props.status] ?? [props.status, ESTILO.PENDENTE[1]])
</script>

<template>
  <span class="inline-flex shrink-0 flex-wrap items-center gap-1">
    <span class="rounded-full border px-2 py-0.5 text-xs font-semibold whitespace-nowrap" :class="estilo[1]">{{ estilo[0] }}</span>
    <span
      class="rounded-full border border-brand-500/50 px-2 py-0.5 text-xs font-semibold whitespace-nowrap text-brand-800 dark:text-brand-300"
      v-if="perfil === 'ADMIN'"
    >
      Admin
    </span>
  </span>
</template>
