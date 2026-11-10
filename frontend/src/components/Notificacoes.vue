<script setup>
import { useNotificacoesStore } from '../stores/notificacoes'

const notificacoes = useNotificacoesStore()

// Cor sozinha não comunica: quem não distingue verde de vermelho precisa do
// desenho para saber se deu certo. Por isso cada tipo traz ícone próprio, e a
// cor entra na barra e no ícone — nunca no texto, que fica no neutro de
// leitura para manter contraste em letra pequena.
const ESTILO = {
  sucesso: { icone: 'text-ok-600 dark:text-ok-400', barra: 'bg-ok-600 dark:bg-ok-400' },
  erro: { icone: 'text-erro-600 dark:text-erro-400', barra: 'bg-erro-600 dark:bg-erro-400' },
  aviso: { icone: 'text-alerta-600 dark:text-alerta-400', barra: 'bg-warn-500' },
  info: { icone: 'text-brand-800 dark:text-brand-300', barra: 'bg-brand-500' },
  progresso: { icone: 'text-brand-800 dark:text-brand-300', barra: 'bg-brand-500' },
}

// Os traços de cada ícone. Ficam em dado, e não em cinco blocos de <svg> no
// template, pelo mesmo motivo da prévia percorrer o ESQUEMA: um tipo novo
// entra aqui sem mexer na marcação.
const TRACOS = {
  sucesso: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z', 'm8.5 12 2.5 2.5 4.5-5'],
  erro: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z', 'm9 9 6 6', 'm15 9-6 6'],
  aviso: ['M10.3 4.3 2.8 17a2 2 0 0 0 1.7 3h15a2 2 0 0 0 1.7-3L13.7 4.3a2 2 0 0 0-3.4 0Z', 'M12 9v4', 'M12 17h.01'],
  info: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z', 'M12 11v5', 'M12 7.5h.01'],
}
</script>

<template>
  <!-- A região viva precisa existir no DOM antes do cartão entrar: leitor de
       tela anuncia a INSERÇÃO dentro de uma região que ele já observa. Se o
       contêiner nascesse junto com o primeiro aviso, o anúncio sairia mudo.
       É polido de propósito — nada aqui é emergência a ponto de cortar a
       frase que o leitor está dizendo. -->
  <div
    class="pointer-events-none fixed print:hidden inset-x-4 bottom-4 z-50 flex flex-col gap-3 sm:inset-x-auto sm:top-4 sm:right-4 sm:bottom-auto sm:w-96"
    aria-live="polite"
    aria-relevant="additions"
    @mouseenter="notificacoes.pausar()"
    @mouseleave="notificacoes.retomar()"
    @focusin="notificacoes.pausar()"
    @focusout="notificacoes.retomar()"
  >
    <TransitionGroup
      enter-active-class="transition duration-200 ease-out motion-reduce:transition-none"
      enter-from-class="translate-y-2 opacity-0 sm:translate-x-3 sm:translate-y-0"
      leave-active-class="transition duration-150 ease-in motion-reduce:transition-none"
      leave-to-class="scale-95 opacity-0"
      move-class="transition-transform duration-200 motion-reduce:transition-none"
    >
      <div
        class="pointer-events-auto flex overflow-hidden rounded border border-ink-200 bg-white shadow-cartao dark:border-night-700 dark:bg-night-900"
        v-for="notificacao in notificacoes.lista"
        :key="notificacao.id"
      >
        <span class="w-1 shrink-0" :class="ESTILO[notificacao.tipo].barra" aria-hidden="true"></span>

        <div class="flex min-w-0 flex-1 items-start gap-3 py-3 pl-3 pr-2">
          <!-- Progresso gira; os demais são estáticos. A rotação respeita quem
               pediu menos movimento no sistema. -->
          <svg
            class="mt-0.5 h-5 w-5 shrink-0 animate-spin motion-reduce:animate-none"
            :class="ESTILO[notificacao.tipo].icone"
            v-if="notificacao.tipo === 'progresso'"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            aria-hidden="true"
          >
            <circle class="opacity-25" cx="12" cy="12" r="9" />
            <path d="M21 12a9 9 0 0 0-9-9" stroke-linecap="round" />
          </svg>
          <svg
            class="mt-0.5 h-5 w-5 shrink-0"
            :class="ESTILO[notificacao.tipo].icone"
            v-else
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path v-for="traco in TRACOS[notificacao.tipo]" :key="traco" :d="traco" />
          </svg>

          <div class="min-w-0 flex-1">
            <p class="text-sm font-semibold text-ink-900 dark:text-mist-100">
              {{ notificacao.titulo }}
            </p>
            <p
              class="mt-0.5 text-sm break-words text-ink-600 dark:text-mist-300"
              v-if="notificacao.texto"
            >
              {{ notificacao.texto }}
            </p>
          </div>

          <button
            class="-mr-1 inline-flex h-8 w-8 shrink-0 items-center justify-center rounded text-ink-400 transition-colors hover:bg-ink-100 hover:text-ink-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:text-mist-500 dark:hover:bg-night-800 dark:hover:text-mist-100"
            type="button"
            :aria-label="`Fechar aviso: ${notificacao.titulo}`"
            @click="notificacoes.remover(notificacao.id)"
          >
            <svg
              class="h-4 w-4"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              aria-hidden="true"
            >
              <path d="m6 6 12 12" />
              <path d="M18 6 6 18" />
            </svg>
          </button>
        </div>
      </div>
    </TransitionGroup>
  </div>
</template>
