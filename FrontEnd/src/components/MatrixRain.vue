<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const canvas = ref(null)

const TAMANHO = 20
const FPS = 24
const INTERVALO = 1000 / FPS
const OPACIDADE_RASTRO = 0.15 // quanto do quadro anterior sobra a cada passo
const CHANCE_LIDER = 0.07 // fração dos caracteres desenhada no ciano de acento
const CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@#$%^&*()<>{}[]/\|'.split('')

// Reservas usadas apenas se os tokens de --color-* não resolverem.
const RESERVA = { fundo: '#11365b', cabeca: '#cfe2f7', lider: '#5fd8e8' }

let ctx = null
let raf = 0
let observer = null
let colunas = []
let cores = { ...RESERVA, rastro: comAlfa(RESERVA.fundo, OPACIDADE_RASTRO) }
let ultimoFrame = 0

function comAlfa(hex, alfa) {
  if (!/^#[0-9a-f]{6}$/i.test(hex)) return hex
  const n = parseInt(hex.slice(1), 16)
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${alfa})`
}

// A paleta vive em style.css (@theme); aqui só lemos os tokens já resolvidos.
function lerCores(el) {
  const estilo = getComputedStyle(el)
  const token = (nome, reserva) => estilo.getPropertyValue(nome).trim() || reserva
  const fundo = token('--color-brand-900', RESERVA.fundo)
  return {
    fundo,
    rastro: comAlfa(fundo, OPACIDADE_RASTRO),
    cabeca: token('--color-brand-100', RESERVA.cabeca),
    lider: token('--color-glow-300', RESERVA.lider),
  }
}

function medir() {
  const el = canvas.value
  if (!el) return null
  const l = el.clientWidth
  const a = el.clientHeight
  return l && a ? { l, a } : null
}

function passo() {
  const dim = medir()
  if (!dim) return

  ctx.fillStyle = cores.rastro
  ctx.fillRect(0, 0, dim.l, dim.a)
  ctx.font = `${TAMANHO}px monospace`

  for (let i = 0; i < colunas.length; i += 2) {
    const char = CHARS[Math.floor(Math.random() * CHARS.length)]
    ctx.fillStyle = Math.random() < CHANCE_LIDER ? cores.lider : cores.cabeca
    ctx.fillText(char, i * TAMANHO, colunas[i] * TAMANHO)
    if (colunas[i] * TAMANHO > dim.a && Math.random() > 0.95) colunas[i] = 0
    colunas[i]++
  }
}

function animar(agora) {
  raf = requestAnimationFrame(animar)
  if (agora - ultimoFrame < INTERVALO) return
  ultimoFrame = agora
  passo()
}

function dimensionar() {
  const el = canvas.value
  const dim = medir()
  if (!el || !dim) return

  cores = lerCores(el)

  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  el.width = Math.round(dim.l * dpr)
  el.height = Math.round(dim.a * dpr)
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctx.fillStyle = cores.fundo
  ctx.fillRect(0, 0, dim.l, dim.a)

  colunas = Array.from({ length: Math.floor(dim.l / TAMANHO) }, () => Math.random() * -100)
}

onMounted(() => {
  const el = canvas.value
  if (!el) return
  ctx = el.getContext('2d')
  if (!ctx) return

  dimensionar()
  observer = new ResizeObserver(dimensionar)
  observer.observe(el)

  raf = requestAnimationFrame(animar)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  observer?.disconnect()
})
</script>

<template>
  <canvas class="block h-full w-full" ref="canvas" aria-hidden="true"></canvas>
</template>
