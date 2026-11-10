<script setup>
// Barra de etapas do upload. Só desenha: quem decide a situação de cada etapa
// é a página, a partir da store.
// situacao: 'pendente' | 'andamento' | 'feito' | 'erro'
defineProps({
  etapas: { type: Array, required: true },
})

// Mesmo critério das notificações: cada situação tem desenho próprio, e a cor
// fica no ícone, nunca no texto.
const ICONE = {
  pendente: 'text-ink-400 dark:text-mist-500',
  andamento: 'text-brand-800 dark:text-brand-300',
  feito: 'text-ok-600 dark:text-ok-400',
  erro: 'text-erro-600 dark:text-erro-400',
}

const LEITURA = {
  pendente: 'não iniciada',
  andamento: 'em andamento',
  feito: 'concluída',
  erro: 'com problema',
}
</script>

<template>
  <ol class="grid gap-4 rounded border border-ink-200 bg-white p-4 sm:grid-cols-4 dark:border-night-700 dark:bg-night-900">
    <li
      class="flex items-start gap-2.5"
      v-for="etapa in etapas"
      :key="etapa.titulo"
      :aria-current="etapa.situacao === 'andamento' ? 'step' : undefined"
    >
      <svg
        class="mt-0.5 h-5 w-5 shrink-0"
        :class="[ICONE[etapa.situacao], etapa.situacao === 'andamento' && 'animate-spin motion-reduce:animate-none']"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <template v-if="etapa.situacao === 'andamento'">
          <circle class="opacity-25" cx="12" cy="12" r="9" />
          <path d="M21 12a9 9 0 0 0-9-9" />
        </template>
        <template v-else>
          <circle cx="12" cy="12" r="9" />
          <path d="m8.5 12 2.5 2.5 4.5-5" v-if="etapa.situacao === 'feito'" />
          <path d="m9 9 6 6M15 9l-6 6" v-else-if="etapa.situacao === 'erro'" />
        </template>
      </svg>
      <div class="min-w-0">
        <p
          class="text-sm font-semibold"
          :class="etapa.situacao === 'pendente' ? 'text-ink-600 dark:text-mist-400' : 'text-ink-900 dark:text-mist-100'"
        >
          {{ etapa.titulo }}
          <span class="sr-only">({{ LEITURA[etapa.situacao] }})</span>
        </p>
        <p class="mt-0.5 text-xs text-ink-600 tabular-nums dark:text-mist-400" v-if="etapa.detalhe">
          {{ etapa.detalhe }}
        </p>
      </div>
    </li>
  </ol>
</template>
