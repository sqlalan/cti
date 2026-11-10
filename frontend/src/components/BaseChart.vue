<script setup>
// BaseChart.vue — desenha UM gráfico com Chart.js. Todas as telas reutilizam este componente.
import { onMounted, onBeforeUnmount, ref, watch } from 'vue' // ciclo de vida: onMounted = "a tela abriu"
import { Chart, registerables } from 'chart.js' // Chart.js: a biblioteca que desenha gráficos no <canvas>
import { compactar } from './graficos/escala' // 70510000 vira "R$ 70,5 mi" no eixo

Chart.register(...registerables) // liga todos os tipos de gráfico (barra, linha, rosca...)

const props = defineProps({ // o que a tela entrega ao componente
  tipo: { type: String, default: 'bar' }, // 'bar' | 'line' | 'doughnut'
  rotulos: { type: Array, default: () => [] }, // nomes (eixo X): Comércio, Indústria...
  valores: { type: Array, default: () => [] }, // números (altura das barras)
  titulo: { type: String, default: '' }, // nome da série (aparece no tooltip)
  horizontal: Boolean, // true = barras deitadas
  formato: { type: String, default: 'inteiro' }, // 'inteiro' (contagem) | 'moeda' (R$)
  cores: { type: [Array, Object], default: null }, // uma cor por fatia/barra; { claro: [...], escuro: [...] } muda com o tema
})

const canvas = ref(null) // aponta para o <canvas> do template
let grafico = null // guarda o gráfico atual para poder apagar depois

// Cores do tema do projeto (style.css), uma versão para o claro e outra para o escuro.
const TEMA = {
  claro: { serie: '#3f77c4', texto: '#5a6b80', grade: '#d9e0e6' },
  escuro: { serie: '#5b95e0', texto: '#9fb0c2', grade: '#24405f' },
}
const escuro = window.matchMedia('(prefers-color-scheme: dark)') // o sistema está no tema escuro?

const moeda = (n) => Number(n).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })

function coresDoTema() { // as cores próprias na versão do tema atual
  if (!props.cores || Array.isArray(props.cores)) return props.cores
  return escuro.matches ? props.cores.escuro : props.cores.claro
}

function desenhar() { // monta e desenha o gráfico
  grafico?.destroy() // apaga o desenho antigo, se houver
  const rosca = props.tipo === 'doughnut' // rosca usa uma cor por fatia
  const tema = escuro.matches ? TEMA.escuro : TEMA.claro // cores do tema atual
  const eixoValor = props.horizontal ? 'x' : 'y' // eixo dos números (deitado = X)
  const eixoNomes = props.horizontal ? 'y' : 'x' // eixo dos nomes
  const emReais = props.formato === 'moeda' // valores em dinheiro?

  grafico = new Chart(canvas.value, { // cria o gráfico
    type: props.tipo, // tipo
    data: {
      labels: props.rotulos, // nomes
      datasets: [{
        label: props.titulo, // nome da série
        data: props.valores, // números vindos do banco
        backgroundColor: coresDoTema() ?? tema.serie, // cores próprias (níveis) ou a cor da série
        borderColor: rosca ? (escuro.matches ? '#122438' : '#ffffff') : tema.serie, // separa as fatias; cor da linha
        borderWidth: 2, // espessura da linha e da borda das fatias
        borderRadius: props.tipo === 'bar' ? 4 : 0, // cantos levemente arredondados nas barras
        tension: 0, // linha reta entre os meses: a curva inventava valores entre um ponto e outro
        pointRadius: props.tipo === 'line' ? 3 : 0, // marca cada mês na linha
      }],
    },
    options: {
      indexAxis: props.horizontal ? 'y' : 'x', // 'y' deita as barras
      responsive: true, maintainAspectRatio: false, // ocupa a largura da caixa
      color: tema.texto, // cor dos textos (legenda)
      plugins: {
        legend: { display: rosca, position: 'bottom', labels: { color: tema.texto, usePointStyle: true } }, // legenda só na rosca
        tooltip: { callbacks: { label: (c) => ` ${emReais ? moeda(c.parsed[eixoValor] ?? c.parsed) : (c.parsed[eixoValor] ?? c.parsed).toLocaleString('pt-BR')}` } }, // valor exato no tooltip
      },
      scales: rosca ? {} : {
        [eixoValor]: { // eixo dos números
          beginAtZero: true, // começa no zero
          grid: { color: tema.grade }, // linhas de grade no tom do tema
          ticks: {
            color: tema.texto, // cor dos números do eixo
            precision: 0, // contagem não tem meio: só inteiros
            callback: (v) => (emReais ? compactar(v, { moeda: true }) : Number(v).toLocaleString('pt-BR')), // "R$ 30 mi"
          },
        },
        [eixoNomes]: { grid: { display: false }, ticks: { color: tema.texto } }, // eixo dos nomes, sem grade
      },
    },
  })
}

onMounted(() => { // desenha quando a tela abre
  desenhar()
  escuro.addEventListener('change', desenhar) // troca de tema no sistema: redesenha com as cores novas
})
watch(() => [props.rotulos, props.valores], desenhar) // redesenha se os dados mudarem
onBeforeUnmount(() => { // limpa ao sair da tela
  escuro.removeEventListener('change', desenhar)
  grafico?.destroy()
})
</script>

<template>
  <div class="h-64"> <!-- altura fixa da caixa do gráfico -->
    <canvas ref="canvas"></canvas> <!-- o quadro onde o Chart.js desenha -->
  </div>
</template>
