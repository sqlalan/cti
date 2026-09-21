import { defineStore } from 'pinia'
import { computed, ref, shallowRef } from 'vue'
import * as XLSX from 'xlsx'
import { resumir, tratarLinhas } from '../dados/esquemaPlanilha'
import { useNotificacoesStore } from './notificacoes'

export const FORMATOS = ['.xlsx', '.xls', '.csv']
export const TAMANHO_MAXIMO = 25 * 1024 * 1024

/**
 * Tamanho legível. Mora aqui, e não na página, porque a recusa por arquivo
 * grande precisa do valor dentro da própria mensagem — e duas cópias da mesma
 * conversão divergem no primeiro ajuste.
 */
export function formatarTamanho(bytes) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1).replace('.', ',')} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1).replace('.', ',')} MB`
}

// "1 linhas" denuncia texto montado por máquina e contamina a confiança no
// resto da mensagem.
function linhas(quantidade) {
  return `${quantidade.toLocaleString('pt-BR')} ${quantidade === 1 ? 'linha' : 'linhas'}`
}

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
    pasta = XLSX.read(texto, { type: 'string', raw: true })
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

// Store da planilha: o estado da tela de upload e do relatório.
//
// Segue a divisão da aula 08 — State guarda, Getters calculam, Actions fazem —
// no formato setup do Pinia: ref é state, computed é getter, function é action.
//
// As REGRAS de validação ficam em dados/esquemaPlanilha.js, e a action
// validarDados() é quem as aplica. Assim a store continua sendo o lugar onde a
// validação acontece, e as regras continuam testáveis sem montar o app.
export const usePlanilhaStore = defineStore('planilha', () => {
  const notificacoes = useNotificacoesStore()

  // ---------------------------------------------------------------------------
  // STATE — dados temporários da interface. Nada aqui vai para o banco.
  // ---------------------------------------------------------------------------

  // shallowRef porque o File é um objeto do navegador, não um dado nosso:
  // deixar o Vue embrulhar em proxy reativo pode quebrar arrayBuffer() e o
  // envio via FormData. Aqui só a troca do arquivo precisa disparar reação.
  const arquivo = shallowRef(null)
  const nomeArquivo = ref('')
  const dataUpload = ref(null)
  const tamanhoArquivo = ref(0) // em bytes

  // A matriz como veio da planilha (primeira linha = cabeçalho), antes de
  // qualquer tratamento. shallowRef porque ela nunca é alterada por dentro —
  // só trocada inteira — e uma planilha de milhares de linhas não precisa de
  // um proxy reativo em cada célula.
  const dadosBrutos = shallowRef([])

  const dadosValidos = ref([])
  // Cada item é a linha rejeitada com o motivo: { linha, codigo, nome, motivo }.
  const dadosInvalidos = ref([])
  // Linhas que passaram, mas com algo a conferir (segmento fora do dicionário…).
  const avisos = ref([])
  const colunasIgnoradas = ref([])
  const aba = ref('')
  const abasIgnoradas = ref([])

  // vazio → lendo → pronto | falha ; enviando → enviado | falha
  //
  // Não se chama statusValidacao como no slide porque cobre mais que a
  // validação: também diz se a planilha está sendo enviada ou já foi.
  const estado = ref('vazio')
  // Motivo do último problema do ARQUIVO (formato, leitura, envio). Problema
  // de LINHA mora em dadosInvalidos.
  const falha = ref('')

  // A notificação da operação em curso. Guardada para que a etapa seguinte
  // reescreva o mesmo cartão em vez de empilhar outro: uma operação de cada
  // vez, um cartão de cada vez. Fica fora do state porque não é dado de tela.
  let idOperacao = null

  // ---------------------------------------------------------------------------
  // GETTERS — calculados a partir do state, sem alterá-lo. Recalculam sozinhos
  // quando o state muda, então nenhum total fica desatualizado.
  // ---------------------------------------------------------------------------

  const quantidadeValidas = computed(() => dadosValidos.value.length)
  const quantidadeInvalidas = computed(() => dadosInvalidos.value.length)
  const quantidadeLinhas = computed(() => quantidadeValidas.value + quantidadeInvalidas.value)

  const percentualValidos = computed(() =>
    quantidadeLinhas.value === 0 ? 0 : (quantidadeValidas.value / quantidadeLinhas.value) * 100,
  )

  const temDados = computed(() => quantidadeValidas.value > 0)

  // Os recortes por nível, segmento, consultor, serviço e cidade. É getter e
  // não state: antes o resumo era calculado uma vez e guardado, e bastava
  // alguém alterar dadosValidos para ele passar a descrever outra planilha.
  // Só existe depois de uma validação — que sempre produz ao menos uma linha,
  // válida ou não.
  const resumo = computed(() =>
    quantidadeLinhas.value === 0
      ? null
      : resumir(dadosValidos.value, dadosInvalidos.value, avisos.value, colunasIgnoradas.value),
  )

  // Arredondado em centavos dentro de resumir().
  const totalFaturamento = computed(() => resumo.value?.faturamentoTotal ?? 0)

  const faturamentoMedio = computed(() =>
    quantidadeValidas.value === 0
      ? 0
      : Math.round((totalFaturamento.value / quantidadeValidas.value) * 100) / 100,
  )

  // Códigos distintos em TODAS as linhas lidas, válidas ou não: é quantos
  // clientes a planilha diz ter. Um código repetido conta uma vez só — por
  // isso pode dar menos que quantidadeLinhas. '—' é o marcador de linha sem
  // código, não um código.
  const quantidadeClientes = computed(() => {
    const codigos = [...dadosValidos.value, ...dadosInvalidos.value]
      .map((linha) => linha.codigo)
      .filter((codigo) => codigo && codigo !== '—')
    return new Set(codigos).size
  })

  // Mensagens prontas para o usuário, no formato "Linha 8: Consultor em
  // branco". Getter, e não uma lista preenchida na action: é só outra forma
  // de ler dadosInvalidos, e guardada à parte poderia discordar dela.
  const erros = computed(() =>
    dadosInvalidos.value.map(
      (item) => `Linha ${item.linha}: ${item.motivo.charAt(0).toUpperCase()}${item.motivo.slice(1)}`,
    ),
  )

  // Leitura ou envio em andamento.
  const carregando = computed(() => estado.value === 'lendo' || estado.value === 'enviando')

  // O resultado da validação em texto. Depois de validada, a planilha
  // continua "válida" ou "com inconsistências" mesmo durante e depois do
  // envio — o envio é outro assunto, contado em `estado`.
  const statusValidacao = computed(() => {
    if (quantidadeLinhas.value > 0) {
      return quantidadeInvalidas.value === 0 ? 'Arquivo válido' : 'Arquivo possui inconsistências'
    }
    if (estado.value === 'lendo') return 'Lendo planilha'
    if (estado.value === 'falha') return 'Arquivo não lido'
    return 'Aguardando arquivo'
  })

  // 'falha' entra porque, com dados na tela, ela só pode ter vindo do envio —
  // falha de leitura não deixa linha válida. Sem isto o botão travava depois
  // de um envio malsucedido, justo quando a mensagem convida a tentar de novo.
  const podeEnviar = computed(
    () => temDados.value && (estado.value === 'pronto' || estado.value === 'falha'),
  )

  // ---------------------------------------------------------------------------
  // ACTIONS — o que muda o state.
  // ---------------------------------------------------------------------------

  function limpar() {
    arquivo.value = null
    nomeArquivo.value = ''
    dataUpload.value = null
    tamanhoArquivo.value = 0
    dadosBrutos.value = []
    dadosValidos.value = []
    dadosInvalidos.value = []
    avisos.value = []
    colunasIgnoradas.value = []
    aba.value = ''
    abasIgnoradas.value = []
    estado.value = 'vazio'
    falha.value = ''
  }

  /**
   * Aplica as regras do esquema sobre dadosBrutos e separa válidas de
   * inválidas. Lê do state em vez de receber a lista: assim dá para validar de
   * novo — depois de cadastrar um segmento no dicionário, por exemplo — sem
   * pedir o arquivo outra vez.
   *
   * Linha com problema não derruba o arquivo: ela vai para dadosInvalidos e o
   * resto segue, para a pessoa corrigir três linhas em vez de reenviar tudo.
   */
  function validarDados() {
    const tratado = tratarLinhas(dadosBrutos.value)

    dadosValidos.value = tratado.dados
    dadosInvalidos.value = tratado.erros
    avisos.value = tratado.avisos
    colunasIgnoradas.value = tratado.colunasIgnoradas
  }

  function abrirOperacao(titulo, texto) {
    if (idOperacao) notificacoes.remover(idOperacao)
    idOperacao = notificacoes.notificar({ tipo: 'progresso', titulo, texto })
    return idOperacao
  }

  /**
   * Recusa o arquivo antes de tentar ler. O motivo vai para dois lugares de
   * propósito: a notificação avisa na hora, porque a pessoa pode estar olhando
   * o fim da página, e o banner fixo continua ali depois que ela sai — o
   * estado de "não deu certo" não se resolve sozinho.
   */
  function recusar(titulo, detalhe) {
    limpar()
    estado.value = 'falha'
    falha.value = detalhe ? `${titulo} — ${detalhe}` : titulo

    if (idOperacao) notificacoes.remover(idOperacao)
    idOperacao = notificacoes.notificar({ tipo: 'erro', titulo, texto: detalhe })

    return false
  }

  /**
   * Recebe o File, confere, lê e manda validar. Devolve true quando sobrou
   * dado válido.
   */
  async function carregar(entrada) {
    if (!entrada) {
      return recusar(
        'Nenhum arquivo recebido',
        'A seleção veio vazia — tente arrastar o arquivo de novo.',
      )
    }

    const tipo = extensao(entrada.name)
    if (!FORMATOS.includes(tipo)) {
      return recusar(
        'Formato não aceito',
        `${tipo ? `A extensão ${tipo} não é lida aqui.` : 'O arquivo não tem extensão.'} ` +
          `Envie ${FORMATOS.join(', ')}.`,
      )
    }
    if (entrada.size === 0) {
      return recusar(
        'Arquivo vazio',
        'O arquivo tem 0 byte — provavelmente a exportação da planilha falhou.',
      )
    }
    if (entrada.size > TAMANHO_MAXIMO) {
      return recusar(
        'Arquivo grande demais',
        `O limite é ${formatarTamanho(TAMANHO_MAXIMO)} e este tem ${formatarTamanho(entrada.size)}.`,
      )
    }

    limpar()
    arquivo.value = entrada
    nomeArquivo.value = entrada.name
    dataUpload.value = new Date()
    tamanhoArquivo.value = entrada.size
    estado.value = 'lendo'

    const notificacao = abrirOperacao('Lendo a planilha…', entrada.name)

    try {
      const lido = await lerMatriz(entrada)

      dadosBrutos.value = lido.matriz
      aba.value = lido.aba
      abasIgnoradas.value = lido.abasIgnoradas

      validarDados()
      estado.value = 'pronto'

      const validas = quantidadeValidas.value
      const comErro = quantidadeInvalidas.value

      // Três desfechos, três tipos. Chamar de sucesso uma leitura em que nada
      // passou seria mentir para quem só viu o cartão.
      if (!validas) {
        notificacoes.atualizar(notificacao, {
          tipo: 'erro',
          titulo: 'Nenhuma linha aproveitada',
          texto: `As ${linhas(comErro)} do arquivo falharam no tratamento. Os motivos estão na tabela da página.`,
        })
      } else if (comErro) {
        notificacoes.atualizar(notificacao, {
          tipo: 'aviso',
          titulo: 'Planilha lida com pendências',
          texto: `${linhas(validas)} prontas para envio e ${linhas(comErro)} de fora — confira "Linhas com erro" na página.`,
        })
      } else {
        notificacoes.atualizar(notificacao, {
          tipo: 'sucesso',
          titulo: 'Planilha lida',
          texto: `${linhas(validas)} prontas para envio, na aba ${lido.aba}.`,
        })
      }

      return validas > 0
    } catch (erro) {
      // Arquivo corrompido, protegido por senha ou que não é planilha de fato
      // estoura aqui dentro do SheetJS.
      estado.value = 'falha'
      falha.value = `Não foi possível ler a planilha: ${erro.message}`

      notificacoes.atualizar(notificacao, {
        tipo: 'erro',
        titulo: 'Não foi possível ler a planilha',
        texto: `${erro.message}. Verifique se o arquivo não está corrompido nem protegido por senha.`,
      })

      return false
    }
  }

  // -------------------------------------------------------------------------
  // Ponto de integração com o Spring Boot.
  //
  // Está tudo montado, só não dispara. Pelo fluxo da aula (Pinia → Service
  // Axios → Spring), a chamada HTTP vai morar num service em src/services/, e
  // esta action só chama o service. Decida com o backend o que vai no corpo:
  //
  //   a) o arquivo original (FormData abaixo) — o Java reprocessa com Apache
  //      POI e o tratamento do navegador serve só de pré-visualização;
  //   b) as linhas já tratadas (JSON.stringify(dadosValidos.value)) — menos
  //      trabalho no servidor, mas ele ainda precisa revalidar: dado que chega
  //      do navegador é dado que o usuário pode ter adulterado.
  // -------------------------------------------------------------------------
  async function enviar() {
    if (!podeEnviar.value) return false

    estado.value = 'enviando'
    falha.value = ''

    const enviadas = quantidadeValidas.value
    const notificacao = abrirOperacao(
      'Enviando para o servidor…',
      `${linhas(enviadas)} · ${nomeArquivo.value}`,
    )

    const corpo = new FormData()
    corpo.append('arquivo', arquivo.value, nomeArquivo.value)
    corpo.append('dataUpload', dataUpload.value.toISOString())
    corpo.append('linhasValidas', String(enviadas))
    corpo.append('linhasComErro', String(quantidadeInvalidas.value))

    try {
      // --- TROQUE DAQUI ---
      // const resposta = await fetch('/api/planilhas', { method: 'POST', body: corpo })
      // if (!resposta.ok) throw new Error(`o servidor respondeu ${resposta.status}`)
      await new Promise((resolve) => setTimeout(resolve, 900))
      // --- ATÉ AQUI ---

      estado.value = 'enviado'
      notificacoes.atualizar(notificacao, {
        tipo: 'sucesso',
        titulo: 'Envio concluído',
        texto: `${linhas(enviadas)} de ${nomeArquivo.value} chegaram ao servidor.`,
      })

      return true
    } catch (erro) {
      estado.value = 'falha'
      falha.value = `Falha ao enviar: ${erro.message}`

      notificacoes.atualizar(notificacao, {
        tipo: 'erro',
        titulo: 'Falha no envio',
        texto: `${erro.message}. A planilha continua carregada — dá para tentar de novo sem selecionar o arquivo outra vez.`,
      })

      return false
    }
  }

  return {
    // state
    arquivo,
    nomeArquivo,
    dataUpload,
    tamanhoArquivo,
    dadosBrutos,
    dadosValidos,
    dadosInvalidos,
    avisos,
    colunasIgnoradas,
    aba,
    abasIgnoradas,
    estado,
    falha,
    // getters
    quantidadeLinhas,
    quantidadeValidas,
    quantidadeInvalidas,
    percentualValidos,
    totalFaturamento,
    faturamentoMedio,
    quantidadeClientes,
    erros,
    carregando,
    statusValidacao,
    temDados,
    resumo,
    podeEnviar,
    // actions
    limpar,
    validarDados,
    carregar,
    enviar,
  }
})
