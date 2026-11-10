import { onBeforeUnmount, onMounted, ref } from 'vue'

// Mede a largura real do contêiner para desenhar o SVG em pixels 1:1.
// Escalar um viewBox fixo seria mais curto, mas engorda o traço e o texto junto
// com o cartão — aqui a linha continua com 2px e o rótulo com 11px em qualquer
// largura de tela.
export function useLargura(alvo, larguraInicial = 640) {
  const largura = ref(larguraInicial)
  let observador = null

  onMounted(() => {
    if (!alvo.value) return
    observador = new ResizeObserver(([entrada]) => {
      const medida = entrada.contentRect.width
      if (medida > 0) largura.value = medida
    })
    observador.observe(alvo.value)
  })

  onBeforeUnmount(() => observador?.disconnect())

  return largura
}
