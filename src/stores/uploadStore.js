import { defineStore } from 'pinia'
import { computed, ref, shallowRef } from 'vue'
import * as XLSX from 'xlsx'
import { resumir, tratarLinhas } from '../dados/esquemaPlanilha'

export const FORMATOS = ['.xlsx', '.xls', '.csv']
export const TAMANHO_MAXIMO = 25 * 1024 * 1024

function extensao(nome) {
  const ponto = nome.lastIndexOf('.')
  return ponto === -1 ? '' : nome.slice(ponto).toLowerCase()
}

/**
 * Lê o arquivo e devolve a matriz crua (primeira linha = cabeçalho).
 *
 * `header: 1` é deliberado: pede a planilha como array de arrays em vez de
 * deixar o SheetJS montar objetos com os nomes do cabeçalho como chave. Além de
 * dar controle do mapeamento, fecha a porta da poluição de protótipo — uma
 * coluna chamada "__proto__" nunca vira chave de objeto nossa.
 */
async function lerMatriz(arquivo) {
  const buffer = await arquivo.arrayBuffer()
  let pasta

  if (extensao(arquivo.name) === '.csv') {
    // CSV não carrega declaração de codificação. Excel no Windows exporta em
    // windows-1252 por padrão, e decodificar isso como UTF-8 transforma
    // "Indústria" em "Ind�stria". O caractere de substituição é o sinal de que
    // o palpite errou, então tentamos de novo na outra codificação.
    let texto = new TextDecoder('utf-8').decode(buffer)
    if (texto.includes('�')) {
      texto = new TextDecoder('windows-1252').decode(buffer)
    }
    pasta = XLSX.read(texto, { type: 'string' })
  } else {
    pasta = XLSX.read(buffer, { type: 'array', cellDates: true })
  }

  const [primeira, ...demais] = pasta.SheetNames
  if (!primeira) throw new Error('O arquivo não tem nenhuma aba')

  // raw: true é decisivo. Com raw: false o SheetJS RENDERIZA a célula pelo
  // formato numérico e devolve "R$ 1,850,000.00" e "1/15/25" — separador e
  // ordem de data americanos, independente de locale. Seria jogar fora o dado
  // bom (a célula guarda um número e uma data de verdade) para depois tentar
  // adivinhá-lo de volta a partir do texto. Com raw: true, número chega número
  // e data chega Date; só o CSV, que não tem tipo, continua vindo como texto.
  const matriz = XLSX.utils.sheet_to_json(pasta.Sheets[primeira], {
    header: 1,
    raw: true,
    defval: '',
    blankrows: false,
  })

  return { matriz, aba: primeira, abasIgnoradas: demais }
}

export const useUploadStore = defineStore('upload', () => {
  // shallowRef porque o File é um objeto do navegador, não um dado nosso:
  // deixar o Vue embrulhar em proxy reativo pode quebrar arrayBuffer() e o
  // envio via FormData. Aqui só a troca do arquivo precisa disparar reação.
  const arquivo = shallowRef(null)

  const dados = ref([])
  const erros = ref([])
  const resumo = ref(null)

  // vazio → lendo → pronto | falha ; enviando → enviado | falha
  const estado = ref('vazio')
  const falha = ref('')
  const aba = ref('')
  const abasIgnoradas = ref([])

  const temDados = computed(() => dados.value.length > 0)
  const podeEnviar = computed(() => estado.value === 'pronto' && temDados.value)

  function limpar() {
    arquivo.value = null
    dados.value = []
    erros.value = []
    resumo.value = null
    estado.value = 'vazio'
    falha.value = ''
    aba.value = ''
    abasIgnoradas.value = []
  }

  function recusar(motivo) {
    limpar()
    estado.value = 'falha'
    falha.value = motivo
    return false
  }

  /**
   * Recebe o File, valida, lê e trata. Devolve true quando sobrou dado válido.
   *
   * Linha com problema não derruba o arquivo: ela vai para `erros` e o resto
   * segue em `dados`, para a pessoa corrigir três linhas em vez de reenviar
   * tudo no escuro.
   */
  async function carregar(entrada) {
    if (!entrada) return recusar('Nenhum arquivo recebido')

    if (!FORMATOS.includes(extensao(entrada.name))) {
      return recusar(`Formato não aceito — envie ${FORMATOS.join(', ')}`)
    }
    if (entrada.size === 0) return recusar('O arquivo está vazio')
    if (entrada.size > TAMANHO_MAXIMO) {
      return recusar(`Arquivo acima de ${Math.round(TAMANHO_MAXIMO / 1024 / 1024)} MB`)
    }

    limpar()
    arquivo.value = entrada
    estado.value = 'lendo'

    try {
      const lido = await lerMatriz(entrada)
      const tratado = tratarLinhas(lido.matriz)

      dados.value = tratado.dados
      erros.value = tratado.erros
      resumo.value = resumir(
        tratado.dados,
        tratado.erros,
        tratado.avisos,
        tratado.colunasIgnoradas,
      )
      aba.value = lido.aba
      abasIgnoradas.value = lido.abasIgnoradas
      estado.value = 'pronto'

      return tratado.dados.length > 0
    } catch (erro) {
      // Arquivo corrompido, protegido por senha ou que não é planilha de fato
      // estoura aqui dentro do SheetJS.
      arquivo.value = entrada
      estado.value = 'falha'
      falha.value = `Não foi possível ler a planilha: ${erro.message}`
      return false
    }
  }

  // -------------------------------------------------------------------------
  // Ponto de integração com o Spring Boot.
  //
  // Está tudo montado, só não dispara. Para ligar, troque o trecho marcado por
  // um fetch/axios de verdade. Decida com o backend o que vai no corpo:
  //
  //   a) o arquivo original (FormData abaixo) — o Java reprocessa com Apache
  //      POI e o tratamento do navegador serve só de pré-visualização;
  //   b) as linhas já tratadas (JSON.stringify(dados.value)) — menos trabalho
  //      no servidor, mas ele ainda precisa revalidar: dado que chega do
  //      navegador é dado que o usuário pode ter adulterado.
  // -------------------------------------------------------------------------
  async function enviar() {
    if (!podeEnviar.value) return false

    estado.value = 'enviando'
    falha.value = ''

    const corpo = new FormData()
    corpo.append('arquivo', arquivo.value, arquivo.value.name)
    corpo.append('linhasValidas', String(dados.value.length))
    corpo.append('linhasComErro', String(erros.value.length))

    try {
      // --- TROQUE DAQUI ---
      // const resposta = await fetch('/api/planilhas', { method: 'POST', body: corpo })
      // if (!resposta.ok) throw new Error(`o servidor respondeu ${resposta.status}`)
      await new Promise((resolve) => setTimeout(resolve, 900))
      // --- ATÉ AQUI ---

      estado.value = 'enviado'
      return true
    } catch (erro) {
      estado.value = 'falha'
      falha.value = `Falha ao enviar: ${erro.message}`
      return false
    }
  }

  return {
    arquivo,
    dados,
    erros,
    resumo,
    estado,
    falha,
    aba,
    abasIgnoradas,
    temDados,
    podeEnviar,
    carregar,
    limpar,
    enviar,
  }
})
