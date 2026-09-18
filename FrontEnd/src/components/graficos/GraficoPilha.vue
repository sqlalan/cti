<script setup>
import { computed, ref } from 'vue'
import { formatarNumero } from './escala'

const props = defineProps({
  itens: { type: Array, required: true },
})

// Cor de status nunca anda sozinha: cada faixa vem com ícone e rótulo próprios.
// A ordem verde → âmbar → coral é a que foi validada; trocar a sequência muda
// quais pares ficam encostados e pode derrubar a separação sob daltonismo.
const APARENCIA = {
  validos: { fundo: 'bg-ok-600 dark:bg-ok-400', texto: 'text-ok-600 dark:text-ok-400', icone: 'ok' },
  alertas: { fundo: 'bg-alerta-600 dark:bg-alerta-400', texto: 'text-alerta-600 dark:text-alerta-400', icone: 'alerta' },
  erros: { fundo: 'bg-erro-600 dark:bg-erro-400', texto: 'text-erro-600 dark:text-erro-400', icone: 'erro' },
}

const ativo = ref(null)

const total = computed(() => props.itens.reduce((soma, item) => soma + item.registros, 0))
const fatia = (item) => (item.registros / total.value) * 100
const percentual = (item) => fatia(item).toLocaleString('pt-BR', { maximumFractionDigits: 1 })

// Só rotula dentro do segmento quando o texto cabe com folga; abaixo disso o
// valor fica com a legenda e a dica, em vez de ser cortado pela borda.
const cabeDentro = (item) => fatia(item) >= 14
</script>

<template>
  <div>
    <div class="flex h-9 w-full gap-0.5 overflow-hidden rounded">
      <div
        class="relative flex items-center justify-center transition-[filter] focus:outline-2 focus:outline-offset-2 focus:outline-brand-500"
        :class="[APARENCIA[item.chave].fundo, ativo === item.chave ? 'brightness-110' : '']"
        v-for="item in itens"
        :key="item.chave"
        :style="{ width: `${fatia(item)}%` }"
        tabindex="0"
        @pointerenter="ativo = item.chave"
        @pointerleave="ativo = null"
        @focus="ativo = item.chave"
        @blur="ativo = null"
      >
        <span class="text-xs font-semibold tabular-nums text-white" v-if="cabeDentro(item)">
          {{ percentual(item) }}%
        </span>
      </div>
    </div>

    <p
      class="mt-2 rounded border border-ink-200 bg-white px-3 py-2 text-xs shadow-cartao dark:border-night-700 dark:bg-night-800"
      v-if="ativo"
      role="status"
    >
      <span class="font-semibold tabular-nums text-ink-900 dark:text-mist-100">
        {{ formatarNumero(itens.find((i) => i.chave === ativo).registros) }} registros
      </span>
      <span class="text-ink-600 dark:text-mist-300">
        — {{ itens.find((i) => i.chave === ativo).rotulo }},
        {{ percentual(itens.find((i) => i.chave === ativo)) }}% do lote
      </span>
    </p>

    <ul class="mt-4 flex flex-col gap-2">
      <li class="flex items-center gap-2" v-for="item in itens" :key="`legenda-${item.chave}`">
        <svg
          class="h-4 w-4 shrink-0"
          :class="APARENCIA[item.chave].texto"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <template v-if="APARENCIA[item.chave].icone === 'ok'">
            <circle cx="12" cy="12" r="9" />
            <path d="m8.5 12 2.5 2.5 4.5-5" />
          </template>
          <template v-else-if="APARENCIA[item.chave].icone === 'alerta'">
            <path d="M10.3 4.3 2.8 17a2 2 0 0 0 1.7 3h15a2 2 0 0 0 1.7-3L13.7 4.3a2 2 0 0 0-3.4 0Z" />
            <path d="M12 9v4" />
            <path d="M12 17h.01" />
          </template>
          <template v-else>
            <circle cx="12" cy="12" r="9" />
            <path d="m15 9-6 6" />
            <path d="m9 9 6 6" />
          </template>
        </svg>

        <span class="text-xs text-ink-600 dark:text-mist-300">{{ item.rotulo }}</span>
        <span class="ml-auto text-xs font-semibold tabular-nums text-ink-900 dark:text-mist-100">
          {{ formatarNumero(item.registros) }}
        </span>
        <span class="w-12 text-right text-xs tabular-nums text-ink-400 dark:text-mist-500">
          {{ percentual(item) }}%
        </span>
      </li>
    </ul>
  </div>
</template>
