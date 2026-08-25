<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  atraso: { type: Number, default: 0 },
})

const INCLINACAO = 9 // graus no acompanhamento do mouse
const AFUNDA = 70 // px de recuo no scroll
const AVANCA = 60 // px de avanco no clique

const raiz = ref(null)
const sobre = ref(false)
const aberto = ref(false)
const anima = ref(true)

const apontador = ref({ rx: 0, ry: 0, luzX: 50, luzY: 50 })
const profundidade = ref({ z: 0, rx: 0 })

let pedido = 0

function medirScroll() {
  const el = raiz.value
  if (!el || !anima.value) return
  const area = el.getBoundingClientRect()
  const centro = (area.top + area.height / 2) / window.innerHeight
  const desvio = Math.max(-1, Math.min(1, (centro - 0.5) * 2))
  profundidade.value = { z: -Math.abs(desvio) * AFUNDA, rx: desvio * 6 }
}

function aoRolar() {
  if (pedido) return
  pedido = requestAnimationFrame(() => {
    pedido = 0
    medirScroll()
  })
}

function mover(evento) {
  if (!anima.value || evento.pointerType !== 'mouse') return
  const el = raiz.value
  if (!el) return
  const area = el.getBoundingClientRect()
  const x = (evento.clientX - area.left) / area.width
  const y = (evento.clientY - area.top) / area.height
  apontador.value = {
    rx: -(y - 0.5) * 2 * INCLINACAO,
    ry: (x - 0.5) * 2 * INCLINACAO,
    luzX: x * 100,
    luzY: y * 100,
  }
  sobre.value = true
}

function sair() {
  sobre.value = false
  apontador.value = { ...apontador.value, rx: 0, ry: 0 }
}

function alternar() {
  if (!anima.value) return
  aberto.value = !aberto.value
}

function teclar(evento) {
  if (evento.key === 'Enter' || evento.key === ' ') {
    evento.preventDefault()
    alternar()
  }
}

onMounted(() => {
  anima.value = !window.matchMedia('(prefers-reduced-motion: reduce)').matches
  medirScroll()
  window.addEventListener('scroll', aoRolar, { passive: true })
  window.addEventListener('resize', aoRolar, { passive: true })
})

onBeforeUnmount(() => {
  if (pedido) cancelAnimationFrame(pedido)
  window.removeEventListener('scroll', aoRolar)
  window.removeEventListener('resize', aoRolar)
})

const eixos = computed(() => ({
  '--rx': `${apontador.value.rx + profundidade.value.rx}deg`,
  '--ry': `${apontador.value.ry}deg`,
  '--z': `${profundidade.value.z + (aberto.value ? AVANCA : 0)}px`,
  '--escala': aberto.value ? 1.04 : 1,
  '--luz-x': `${apontador.value.luzX}%`,
  '--luz-y': `${apontador.value.luzY}%`,
  '--atraso': `-${props.atraso}ms`,
}))
</script>

<template>
  <div class="[perspective:1000px]" :style="eixos">
    <article
      ref="raiz"
      class="relative h-full cursor-pointer select-none rounded border border-[#D9E0E6] bg-white p-6 shadow-[0_1px_3px_rgba(38,47,62,0.07)] transition-[box-shadow,border-color] duration-300 ease-out animate-flutuar-card motion-reduce:animate-none [animation-delay:var(--atraso)] hover:[animation-play-state:paused] [transform:translateZ(var(--z))_rotateX(var(--rx))_rotateY(var(--ry))_scale(var(--escala))] dark:border-[#25384E] dark:bg-[#132132] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#3F77C4]"
      :class="[
        sobre ? 'duration-75' : 'duration-500',
        aberto
          ? 'z-10 border-[#3F77C4] shadow-[0_35px_60px_-25px_rgba(25,70,117,0.55)]'
          : 'hover:border-[#3F77C4]/40 hover:shadow-[0_20px_35px_-20px_rgba(25,70,117,0.45)]',
      ]"
      :aria-pressed="aberto"
      role="button"
      tabindex="0"
      @pointermove="mover"
      @pointerleave="sair"
      @click="alternar"
      @keydown="teclar"
    >
      <span
        class="pointer-events-none absolute inset-0 rounded bg-[radial-gradient(circle_at_var(--luz-x)_var(--luz-y),rgba(63,119,196,0.16),rgba(63,119,196,0)_55%)] transition-opacity duration-200"
        :class="sobre ? 'opacity-100' : 'opacity-0'"
        aria-hidden="true"
      ></span>
      <slot />
    </article>
  </div>
</template>
