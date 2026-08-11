<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const canvas = ref(null)

const FUNDO = '#11365B'
const RASTRO = 'rgba(17, 54, 91, 0.15)'
const CABECA = '#CFE2F7'
const TAMANHO = 20
const FPS = 24
const INTERVALO = 1000 / FPS
const CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@#$%^&*()<>{}[]/\\|'.split('')

let ctx = null
let raf = 0
let observer = null
let colunas = []
let ultimoFrame = 0

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

  ctx.fillStyle = RASTRO
  ctx.fillRect(0, 0, dim.l, dim.a)
  ctx.fillStyle = CABECA
  ctx.font = `${TAMANHO}px monospace`

  for (let i = 0; i < colunas.length; i += 2) {
    const char = CHARS[Math.floor(Math.random() * CHARS.length)]
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

  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  el.width = Math.round(dim.l * dpr)
  el.height = Math.round(dim.a * dpr)
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctx.fillStyle = FUNDO
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
