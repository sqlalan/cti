import { defineStore } from 'pinia'
import { computed, ref, shallowRef } from 'vue'
import * as XLSX from 'xlsx'
import {
  TIPOS_AVISO,
  TIPOS_ERRO,
  resumir,
  rotularTipo,
  tratarLinhas,
} from '../dados/esquemaPlanilha'
import { useNotificacoesStore } from './notificacoes'

export const FORMATOS = ['.xlsx', '.xls', '.csv']
export const TAMANHO_MAXIMO = 25 * 1024 * 1024

// Mora aqui, e não na página, porque a recusa por arquivo grande precisa do
// valor dentro da mensagem — duas cópias divergem no primeiro ajuste.
export function formatarTamanho(bytes) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1).replace('.', ',')} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1).replace('.', ',')} MB`
}

// "1 linhas" denuncia texto montado por máquina.
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
 * `header: 1` pede array de arrays em vez de deixar o SheetJS montar objetos
 * com o cabeçalho como chave: dá controle do mapeamento e fecha a porta da
 * poluição de protótipo.
 */
async function lerMatriz(arquivo) {
  const buffer = await arquivo.arrayBuffer()
  let pasta

  if (extensao(arquivo.name) === '.csv') {
    // CSV não declara codificação, e o Excel no Windows exporta windows-1252:
    // lido como UTF-8, "Indústria" vira "Ind�stria". O caractere de substituição
    // é o sinal de que o palpite errou.
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

  // raw: true é decisivo. Com raw: false o SheetJS renderiza a célula pelo
  // formato e devolve "R$ 1,850,000.00" e "1/15/25" — separador e data
  // americanos, seja qual for o locale. Seria descartar o dado bom para depois
  // adivinhá-lo de volta do texto. Só o CSV, sem tipo, ainda chega como texto.
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
// Formato setup do Pinia: ref é state, computed é getter, function é action.
// As regras de validação ficam em dados/esquemaPlanilha.js e a action
// validarDados() as aplica — assim elas são testáveis sem montar o app.
export const useUploadStore = defineStore('upload', () => {
  const notificacoes = useNotificacoesStore()

  // ---------------------------------------------------------------------------
  // STATE — dados temporários da interface. Nada aqui vai para o banco.
  // ---------------------------------------------------------------------------

  // shallowRef: embrulhar o File em proxy reativo pode quebrar arrayBuffer() e
  // o envio via FormData. Só a troca do arquivo precisa disparar reação.
  const arquivo = shallowRef(null)
  const nomeArquivo = ref('')
  const dataUpload = ref(null)
  const tamanhoArquivo = ref(0) // em bytes

  // A matriz crua, antes de qualquer tratamento. shallowRef porque ela só é
  // trocada inteira, e milhares de linhas não precisam de proxy por célula.
  const dadosBrutos = shallowRef([])

  const dadosValidos = ref([])
  // Cada item é a linha rejeitada com o motivo: { linha, codigo, nome, motivo }.
  const dadosInvalidos = ref([])
  // Linhas que passaram, mas com algo a conferir (segmento fora do dicionário…).
  const avisos = ref([])
  const colunasIgnoradas = ref([])
  const aba = ref('')
  const abasIgnoradas = ref([])

  // vazio → lendo → pronto | falha ; enviando → enviado | falha. Não se chama
  // statusValidacao porque cobre também o envio.
  const estado = ref('vazio')
  // Motivo do último problema do ARQUIVO (formato, leitura, envio). Problema
  // de LINHA mora em dadosInvalidos.
  const falha = ref('')

  // A notificação em curso, para a etapa seguinte reescrever o mesmo cartão em
  // vez de empilhar outro. Fora do state porque não é dado de tela.
  let idOperacao = null

  // ---------------------------------------------------------------------------
  // GETTERS — derivam do state e recalculam sozinhos, então nenhum total fica
  // desatualizado.
  // ---------------------------------------------------------------------------

  const quantidadeValidas = computed(() => dadosValidos.value.length)
  const quantidadeInvalidas = computed(() => dadosInvalidos.value.length)
  const quantidadeLinhas = computed(() => quantidadeValidas.value + quantidadeInvalidas.value)

  const percentualValidos = computed(() =>
    quantidadeLinhas.value === 0 ? 0 : (quantidadeValidas.value / quantidadeLinhas.value) * 100,
  )

  const temDados = computed(() => quantidadeValidas.value > 0)

  // Os recortes por nível, segmento, consultor, serviço e cidade. Getter e não
  // state: guardado, bastava alterar dadosValidos para ele passar a descrever
  // outra planilha.
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

  // Códigos distintos em todas as linhas, válidas ou não. Repetido conta uma
  // vez, então pode dar menos que quantidadeLinhas. '—' é marcador, não código.
  const quantidadeClientes = computed(() => {
    const codigos = [...dadosValidos.value, ...dadosInvalidos.value]
      .map((linha) => linha.codigo)
      .filter((codigo) => codigo && codigo !== '—')
    return new Set(codigos).size
  })

  // Mensagens prontas, no formato "Linha 8: Consultor em branco". Getter porque
  // é só outra forma de ler dadosInvalidos. Não capitaliza: a mensagem já nasce
  // com maiúscula no esquema, e a regra daqui a tabela do Upload não usava.
  const erros = computed(() =>
    dadosInvalidos.value.map((item) => `Linha ${item.linha}: ${item.motivo}`),
  )

  // Uma linha por PROBLEMA, não por registro — é o formato que o relatório pede
  // (linha, campo, descrição). Os dois contadores divergem de propósito:
  // quantidadeInvalidas conta registros reprovados, totalOcorrencias, problemas.
  const ocorrencias = computed(() =>
    dadosInvalidos.value.flatMap((item) =>
      (item.problemas ?? []).map((problema) => ({
        linha: item.linha,
        codigo: item.codigo,
        nome: item.nome,
        campo: problema.coluna,
        tipo: problema.tipo,
        rotulo: rotularTipo(problema.tipo),
        descricao: problema.mensagem,
      })),
    ),
  )

  const totalOcorrencias = computed(() => ocorrencias.value.length)

  // Ordem de TIPOS_ERRO e não por quantidade: é uma lista de verificações, e
  // ordem estável deixa reencontrar a linha. Tipo com zero fica — "Duplicados 0"
  // é resultado, e esconder faria parecer que a checagem não rodou.
  const errosPorTipo = computed(() => {
    const contagem = new Map()
    for (const ocorrencia of ocorrencias.value) {
      contagem.set(ocorrencia.tipo, (contagem.get(ocorrencia.tipo) ?? 0) + 1)
    }
    return TIPOS_ERRO.map(({ tipo, rotulo }) => ({
      tipo,
      rotulo,
      quantidade: contagem.get(tipo) ?? 0,
    }))
  })

  // Quadro à parte: somados aos erros, inflariam o total com o que já foi
  // resolvido.
  const avisosPorTipo = computed(() => {
    const contagem = new Map()
    for (const aviso of avisos.value) {
      contagem.set(aviso.tipo, (contagem.get(aviso.tipo) ?? 0) + 1)
    }
    return TIPOS_AVISO.map(({ tipo, rotulo }) => ({
      tipo,
      rotulo,
      quantidade: contagem.get(tipo) ?? 0,
    }))
  })

  // O quadro "Validação realizada / Quantidade". Registros válidos fecha a
  // lista: sem ela o quadro mostra só o que falhou.
  const quadroValidacao = computed(() => [
    ...errosPorTipo.value,
    { tipo: 'validos', rotulo: 'Registros válidos', quantidade: quantidadeValidas.value },
  ])

  // Leitura ou envio em andamento.
  const carregando = computed(() => estado.value === 'lendo' || estado.value === 'enviando')

  // O resultado em texto. Continua "válida" ou "com inconsistências" durante e
  // depois do envio — o envio é outro assunto, contado em `estado`.
  const statusValidacao = computed(() => {
    if (quantidadeLinhas.value > 0) {
      return quantidadeInvalidas.value === 0 ? 'Arquivo válido' : 'Arquivo possui inconsistências'
    }
    if (estado.value === 'lendo') return 'Lendo planilha'
    if (estado.value === 'falha') return 'Arquivo não lido'
    return 'Aguardando arquivo'
  })

  // 'falha' entra porque, com dados na tela, ela só pode ter vindo do envio.
  // Sem isto o botão travava justo quando a mensagem convida a tentar de novo.
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
   * Aplica as regras do esquema sobre dadosBrutos e separa válidas de inválidas.
   * Lê do state em vez de receber a lista, para dar para validar de novo — depois
   * de cadastrar um segmento, por exemplo — sem pedir o arquivo outra vez.
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
   * Recusa o arquivo antes de tentar ler. O motivo vai para dois lugares: a
   * notificação avisa na hora, o banner fixo continua depois que ela passa.
   */
  function recusar(titulo, detalhe) {
    limpar()
    estado.value = 'falha'
    falha.value = detalhe ? `${titulo} — ${detalhe}` : titulo

    if (idOperacao) notificacoes.remover(idOperacao)
    idOperacao = notificacoes.notificar({ tipo: 'erro', titulo, texto: detalhe })

    return false
  }

  // Recebe o File, confere, lê e manda validar. true se sobrou dado válido.
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

      // Três desfechos, três tipos: chamar de sucesso uma leitura em que nada
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
      // Corrompido, protegido por senha ou que não é planilha estoura no SheetJS.
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
  // Ponto de integração com o Spring Boot. Está montado, só não dispara: a
  // chamada HTTP vai morar num service em src/services/ e esta action só o
  // chama. Falta decidir o corpo — o arquivo original (FormData abaixo) ou as
  // linhas já tratadas. Em qualquer caso o servidor revalida: dado que vem do
  // navegador é dado que o usuário pode ter adulterado.
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
    ocorrencias,
    totalOcorrencias,
    errosPorTipo,
    avisosPorTipo,
    quadroValidacao,
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
