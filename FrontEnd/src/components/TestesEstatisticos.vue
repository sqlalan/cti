<script setup>
import { computed } from 'vue'

// Testes estatísticos do lote (Plano PI): o resultado e a interpretação em
// português. Os números vêm prontos do processamento; aqui só se mostra.
const props = defineProps({
  testes: { type: Object, required: true }, // { normalidade, associacao }
})

const ALFA = 0.05

const virg = (n, casas = 3) => Number(n).toLocaleString('pt-BR', { minimumFractionDigits: casas, maximumFractionDigits: casas })
// p muito pequeno vira "< 0,0001" em vez de uma fila de zeros.
const valorP = (p) => (p < 0.0001 ? '< 0,0001' : virg(p, 4))

const cartoes = computed(() => {
  const { normalidade: n, associacao: a } = props.testes
  return [
    {
      chave: 'normalidade',
      titulo: 'Distribuição do faturamento',
      pergunta: 'O faturamento segue uma distribuição normal (em forma de sino)?',
      metodo: 'Shapiro-Wilk',
      aplicado: n?.aplicado,
      conclusao: n?.normal ? 'Segue a normal' : 'Não segue a normal',
      destaque: !n?.normal, // resultado com consequência prática: usar a mediana
      numeros: n?.aplicado ? [`W = ${virg(n.estatistica)}`, `p = ${valorP(n.p)}`, `${n.n} clientes`] : [],
      texto: n?.interpretacao,
    },
    {
      chave: 'associacao',
      titulo: 'Segmento × nível do cliente',
      pergunta: 'A classificação A, B e C muda conforme o segmento?',
      metodo: 'Qui-quadrado',
      aplicado: a?.aplicado,
      conclusao: a?.associadas ? 'Há associação' : 'Sem associação',
      destaque: a?.associadas,
      numeros: a?.aplicado ? [`χ² = ${virg(a.estatistica, 2)}`, `gl = ${a.grausLiberdade}`, `p = ${valorP(a.p)}`] : [],
      texto: a?.interpretacao,
    },
  ]
})
</script>

<template>
  <section class="mt-6">
    <h2 class="font-semibold text-ink-900 dark:text-mist-100">Testes estatísticos</h2>
    <p class="mt-1 text-sm text-ink-600 dark:text-mist-300">
      Conferem se um padrão nos dados é real ou pode ser acaso. Resultado com p menor que
      {{ virg(ALFA, 2) }} é considerado significativo.
    </p>

    <div class="mt-3 grid gap-4 lg:grid-cols-2">
      <article
        class="min-w-0 rounded border border-ink-200 bg-white p-4 shadow-cartao dark:border-night-700 dark:bg-night-900"
        v-for="c in cartoes"
        :key="c.chave"
      >
        <div class="flex flex-wrap items-start justify-between gap-2">
          <div class="min-w-0">
            <h3 class="font-semibold text-ink-900 dark:text-mist-100">{{ c.titulo }}</h3>
            <p class="text-xs text-ink-600 dark:text-mist-300">{{ c.metodo }}</p>
          </div>
          <span
            class="shrink-0 rounded-full border px-2 py-0.5 text-xs font-semibold"
            :class="c.destaque
              ? 'border-alerta-600/50 text-alerta-600 dark:border-alerta-400/50 dark:text-alerta-400'
              : 'border-ok-600/50 text-ok-600 dark:border-ok-400/50 dark:text-ok-400'"
            v-if="c.aplicado"
          >
            {{ c.conclusao }}
          </span>
        </div>

        <p class="mt-3 text-sm text-ink-600 dark:text-mist-300">{{ c.pergunta }}</p>
        <p class="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-sm font-medium tabular-nums text-ink-900 dark:text-mist-100" v-if="c.numeros.length">
          <span v-for="n in c.numeros" :key="n">{{ n }}</span>
        </p>
        <p class="mt-2 text-sm leading-relaxed text-ink-900 dark:text-mist-100">{{ c.texto }}</p>
      </article>
    </div>
  </section>
</template>
