import { defineStore } from 'pinia'
import { ref } from 'vue'

// Quanto cada tipo fica na tela, em milissegundos. Zero significa que só sai
// no clique: erro é a explicação do que a pessoa precisa consertar, e
// progresso ainda está acontecendo — nenhum dos dois pode sumir sozinho.
export const DURACAO = {
  sucesso: 5000,
  info: 6000,
  aviso: 9000,
  erro: 0,
  progresso: 0,
}

// Acima disso a pilha vira parede e ninguém lê nenhuma.
const MAXIMO = 4

let sequencia = 0

export const useNotificacoesStore = defineStore('notificacoes', () => {
  const lista = ref([])

  // Os relógios ficam fora do ref de propósito: handle de setTimeout não é
  // dado de tela, e deixar o Vue observar isso renderizaria à toa.
  const relogios = new Map()

  function agendar(id, duracao) {
    cancelar(id)
    if (!duracao) return
    relogios.set(id, {
      restante: duracao,
      desde: Date.now(),
      handle: setTimeout(() => remover(id), duracao),
    })
  }

  function cancelar(id) {
    const relogio = relogios.get(id)
    if (relogio?.handle) clearTimeout(relogio.handle)
    relogios.delete(id)
  }

  /**
   * Congela a contagem enquanto o ponteiro ou o foco do teclado está sobre a
   * pilha. Sem isto o aviso some no meio da leitura — justamente quando a
   * pessoa parou para ler, que é o pior momento possível.
   */
  function pausar() {
    for (const relogio of relogios.values()) {
      if (!relogio.handle) continue
      clearTimeout(relogio.handle)
      relogio.restante = Math.max(0, relogio.restante - (Date.now() - relogio.desde))
      relogio.handle = null
    }
  }

  function retomar() {
    for (const [id, relogio] of relogios) {
      if (relogio.handle) continue
      relogio.desde = Date.now()
      relogio.handle = setTimeout(() => remover(id), relogio.restante)
    }
  }

  function notificar({ tipo = 'info', titulo, texto = '', duracao }) {
    const id = ++sequencia
    const espera = duracao ?? DURACAO[tipo] ?? DURACAO.info

    lista.value.push({ id, tipo, titulo, texto })
    podar()
    agendar(id, espera)

    return id
  }

  /**
   * Troca o conteúdo de uma notificação que já está na tela. É assim que
   * "Lendo a planilha…" vira "Planilha lida" no mesmo cartão, em vez de
   * empilhar um segundo aviso e deixar o primeiro mentindo que ainda lê.
   */
  function atualizar(id, mudancas) {
    const alvo = lista.value.find((n) => n.id === id)
    if (!alvo) return

    Object.assign(alvo, mudancas)
    agendar(id, mudancas.duracao ?? DURACAO[alvo.tipo] ?? DURACAO.info)
  }

  function remover(id) {
    cancelar(id)
    lista.value = lista.value.filter((n) => n.id !== id)
  }

  function limpar() {
    for (const id of [...relogios.keys()]) cancelar(id)
    lista.value = []
  }

  // Descarta pelas mais antigas, mas erro é o último a sair: é o que a pessoa
  // precisa ler para consertar a planilha.
  function podar() {
    while (lista.value.length > MAXIMO) {
      const i = lista.value.findIndex((n) => n.tipo !== 'erro')
      const [descartada] = lista.value.splice(i === -1 ? 0 : i, 1)
      cancelar(descartada.id)
    }
  }

  return { lista, notificar, atualizar, remover, limpar, pausar, retomar }
})
