<script setup>
import { computed, ref } from 'vue'
import { formatarNumero } from './escala'

const props = defineProps({
  itens: { type: Array, required: true },
  campo: { type: String, default: 'valor' },
  unidade: { type: String, default: '' },
  // Formatação do número no rótulo e na dica.
  formato: { type: String, default: 'inteiro' }, // inteiro | moeda
  // Só para escala ORDINAL (faixa A/B/C, porte, estágio), onde a ordem faz
  // parte do sentido e a rampa de um matiz mostra essa ordem. Categoria nominal
  // continua com a cor única do padrão — pintar barra por valor gastaria o
  // canal de identidade repetindo o que o comprimento já diz.
  cores: { type: Object, default: null },
})

const rotular = (valor) =>
  props.formato === 'moeda'
    ? valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
    : formatarNumero(valor)

// Categorias nominais recebem todas o mesmo passo de cor. Pintar cada barra de
// um tom conforme o valor gastaria o canal de identidade repetindo o que o
// comprimento da barra já mostra.
const ativo = ref(null)

const maximo = computed(() => Math.max(...props.itens.map((item) => item[props.campo])))
const total = computed(() => props.itens.reduce((soma, item) => soma + item[props.campo], 0))

const proporcao = (item) => (item[props.campo] / maximo.value) * 100
const fatia = (item) => (item[props.campo] / total.value) * 100
</script>

<template>
  <!-- As colunas moram no <ul>, e cada linha as herda por subgrid. Com o grid
       em cada <li>, a coluna `auto` media só o próprio rótulo e o próprio
       valor: a barra de "Tecnologia" começava depois da de "Saúde", e o trilho
       encolhia ou crescia conforme o número à direita. A mesma porcentagem
       virava comprimentos diferentes — o gráfico deixava de ser comparável,
       que é a única coisa que ele precisa garantir. Aqui as três colunas são
       medidas uma vez, pelo maior conteúdo de todas as linhas. -->
  <ul class="grid grid-cols-[minmax(3.25rem,auto)_1fr_auto] gap-x-3 gap-y-3">
    <li
      class="relative col-span-3 grid grid-cols-subgrid items-center rounded focus-within:outline-2 focus-within:outline-offset-2 focus-within:outline-brand-500"
      v-for="item in itens"
      :key="item.rotulo"
      tabindex="0"
      @pointerenter="ativo = item.rotulo"
      @pointerleave="ativo = null"
      @focus="ativo = item.rotulo"
      @blur="ativo = null"
    >
      <span class="text-xs font-medium text-ink-600 dark:text-mist-300">{{ item.rotulo }}</span>

      <!-- A faixa cobre a linha inteira: a área sensível ao ponteiro é a linha,
           não só os pixels pintados da barra. -->
      <span class="flex h-6 items-center">
        <span
          class="h-full rounded-r-[4px] transition-[filter]"
          :class="[
            cores?.[item.rotulo] ?? 'bg-serie-600 dark:bg-serie-400',
            ativo === item.rotulo ? 'brightness-110' : '',
          ]"
          :style="{ width: `${Math.max(1.5, proporcao(item))}%` }"
        ></span>
      </span>

      <!-- Com a coluna compartilhada, valor curto sobraria encostado à esquerda
           e a borda direita ficaria serrilhada. Número alinhado à direita põe
           cada casa decimal embaixo da mesma casa, que é como se compara. -->
      <span class="whitespace-nowrap text-right text-xs font-semibold tabular-nums text-ink-900 dark:text-mist-100">
        {{ rotular(item[campo]) }}
      </span>

      <span
        class="pointer-events-none absolute -top-1 left-16 z-10 w-max -translate-y-full rounded border border-ink-200 bg-white px-3 py-2 shadow-cartao dark:border-night-700 dark:bg-night-800"
        v-if="ativo === item.rotulo"
      >
        <span class="block text-sm font-semibold tabular-nums text-ink-900 dark:text-mist-100">
          {{ rotular(item[campo]) }}{{ unidade ? ` ${unidade}` : '' }}
        </span>
        <span class="mt-0.5 block text-xs text-ink-600 dark:text-mist-300">
          {{ item.rotulo }} — {{ fatia(item).toLocaleString('pt-BR', { maximumFractionDigits: 1 }) }}% do total
        </span>
      </span>
    </li>
  </ul>
</template>
