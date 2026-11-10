import { defineStore } from 'pinia'
import { computed, ref, shallowRef } from 'vue'
import {
  TIPOS_AVISO,
  TIPOS_ERRO,
  resumir,
  rotularTipo,
  tratarLinhas,
} from '../dados/esquemaPlanilha'
import { useNotificacoesStore } from './notificacoes'
import { explicarLote } from '../dados/lote'
import { api, dashboardService, mensagemDeErro, uploadService } from '../services/api'

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

// Data local em aaaa-mm-dd. toISOString() converteria para UTC e, no Brasil,
// meia-noite de 15/01 viraria 14/01 no servidor.
function dataIso(data) {
  if (!(data instanceof Date)) return null
  const mes = String(data.getMonth() + 1).padStart(2, '0')
  const dia = String(data.getDate()).padStart(2, '0')
  return `${data.getFullYear()}-${mes}-${dia}`
}

// Uma linha já tratada → o ClienteDTO do back-end (campos da aula de validação).
function paraClienteDTO(registro) {
  return {
    codigoCliente: registro.codigo,
    nomeCliente: registro.nome,
    consultor: registro.consultor,
    segmento: registro.segmento,
    nivelCliente: registro.nivel,
    faturamentoAnual: registro.faturamento,
    servicosContratados: registro.servicos.join(';'),
    dataContratacao: dataIso(registro.contratacao),
    cidade: registro.cidade || '',
    uf: registro.uf || '',
  }
}

/**
 * Lê o arquivo e devolve a matriz crua (primeira linha = cabeçalho).
 *
 * `header: 1` pede array de arrays em vez de deixar o SheetJS montar objetos
 * com o cabeçalho como chave: dá controle do mapeamento e fecha a porta da
 * poluição de protótipo.
 */
async function lerMatriz(arquivo) {
  // Carregada só aqui: a biblioteca de planilhas é a parte mais pesada do site e
  // só serve para quem abre um arquivo.
  const XLSX = await import('xlsx')
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

  // O lote que o servidor gravou (status AGUARDANDO_ETL). O Python processa
  // depois, em segundo plano; o Dashboard acompanha pelo id do lote.
  const lote = ref(null)
  // Resumo que o Python calculou (média, clientes...), quando o lote termina.
  const resumoEtl = ref(null)
  // O acompanhamento do lote parou de perguntar (demorou demais ou o servidor sumiu).
  const semRespostaEtl = ref(false)
  // Quando o envio começou e quanto levou (ms), para a tela mostrar o tempo.
  const inicioEnvio = ref(null)
  const duracaoEnvio = ref(null)

  // Segunda validação, no Spring Boot (POST /api/clientes/validar), feita logo
  // depois da leitura: o Pinia manda as linhas aprovadas em JSON e o servidor
  // valida de novo e padroniza.
  // { status: 'validando' | 'aprovado' | 'recusado', clientes, mensagem, semResposta }
  const validacaoServidor = ref(null)

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
  const podeValidar = computed(
    () =>
      temDados.value &&
      (estado.value === 'pronto' || estado.value === 'falha') &&
      validacaoServidor.value?.status !== 'validando',
  )

  // Situação do Python depois do envio: null (nada enviado) | 'processando' |
  // 'concluido' | 'erro'. Sai do status do lote, que o Python atualiza no banco.
  const etapaEtl = computed(() => {
    const status = lote.value?.status
    if (!status) return null
    if (status === 'PROCESSADO') return 'concluido'
    if (status === 'ERRO' || status === 'REJEITADO') return 'erro'
    return 'processando'
  })

  // O arquivo só segue para análise depois que o servidor aprovou as linhas.
  // Lote que falhou no Python pode ser enviado de novo sem carregar a planilha.
  const podeEnviar = computed(
    () =>
      validacaoServidor.value?.status === 'aprovado' &&
      (podeValidar.value || (estado.value === 'enviado' && etapaEtl.value === 'erro')),
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
    pararAcompanhamento()
    lote.value = null
    resumoEtl.value = null
    semRespostaEtl.value = false
    inicioEnvio.value = null
    duracaoEnvio.value = null
    validacaoServidor.value = null
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

  // Recebe o File, confere, lê e manda validar aqui e no servidor. true se o
  // servidor aprovou as linhas.
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

      // Sem linha aproveitada não há o que mandar ao servidor.
      return validas > 0 ? validarNoServidor() : false
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
  // Validação no Spring Boot (POST /api/clientes/validar): só as linhas que o
  // Pinia aprovou, em JSON, no formato do ClienteDTO do back-end. O servidor
  // não confia na tela: valida de novo, confere código repetido e padroniza.
  // Roda sozinha ao fim de carregar(); a página só chama de novo quando o
  // servidor não respondeu.
  // -------------------------------------------------------------------------
  async function validarNoServidor() {
    if (!podeValidar.value) return false

    // Se a pessoa trocar de arquivo no meio, a resposta antiga é descartada.
    const validando = arquivo.value
    validacaoServidor.value = { status: 'validando', clientes: [], mensagem: '' }
    const notificacao = abrirOperacao(
      'Validando no servidor…',
      `${linhas(quantidadeValidas.value)} aprovadas no navegador`,
    )

    try {
      const { data } = await api.post('/clientes/validar', dadosValidos.value.map(paraClienteDTO))
      if (arquivo.value !== validando) return false
      validacaoServidor.value = { status: 'aprovado', clientes: data, mensagem: '' }
      // A notificação da leitura é substituída por esta, então as linhas de
      // fora são lembradas aqui.
      const deFora = quantidadeInvalidas.value
      notificacoes.atualizar(notificacao, {
        tipo: deFora ? 'aviso' : 'sucesso',
        titulo: 'Servidor aprovou a planilha',
        texto:
          `${linhas(data.length)} conferidas e padronizadas pelo servidor.` +
          (deFora ? ` ${linhas(deFora)} de fora — confira "Linhas com erro" na página.` : ''),
      })
      return true
    } catch (erro) {
      if (arquivo.value !== validando) return false
      const mensagem = explicarRecusa(erro)
      // Sem resposta (servidor fora do ar) vale tentar de novo; recusa de
      // regra só muda corrigindo a planilha.
      validacaoServidor.value = { status: 'recusado', clientes: [], mensagem, semResposta: !erro.response }
      notificacoes.atualizar(notificacao, { tipo: 'erro', titulo: 'Servidor recusou a planilha', texto: mensagem })
      return false
    }
  }

  // "posicao" é o item da lista enviada (1 = o primeiro); a pessoa procura a
  // linha do Excel, que cada registro guarda desde a leitura.
  function explicarRecusa(erro) {
    const dados = erro.response?.data
    if (!dados?.posicao) return mensagemDeErro(erro)
    const linhaExcel = dadosValidos.value[dados.posicao - 1]?.linha
    return linhaExcel ? `Linha ${linhaExcel} da planilha: ${dados.erro}` : dados.erro
  }

  // -------------------------------------------------------------------------
  // Envio ao Spring Boot (POST /api/uploads/validadas, aula 11): só as linhas
  // que o Pinia aprovou, em JSON. O servidor valida de novo, grava o lote e
  // chama o Python (POST /etl), que limpa, calcula e grava no banco.
  // -------------------------------------------------------------------------
  async function enviar() {
    if (!podeEnviar.value) return false

    pararAcompanhamento()
    estado.value = 'enviando'
    falha.value = ''
    lote.value = null
    resumoEtl.value = null
    semRespostaEtl.value = false
    inicioEnvio.value = Date.now()
    duracaoEnvio.value = null

    const notificacao = abrirOperacao(
      'Enviando ao servidor…',
      `${nomeArquivo.value} · ${linhas(quantidadeValidas.value)} validadas`,
    )

    try {
      const data = await uploadService.enviarValidadas(dadosValidos.value.map(paraClienteDTO), {
        arquivo: nomeArquivo.value,
        recebidas: quantidadeLinhas.value,
      })
      lote.value = data
      duracaoEnvio.value = Date.now() - inicioEnvio.value
      estado.value = 'enviado'

      notificacoes.atualizar(notificacao, {
        tipo: 'progresso',
        titulo: `Lote ${data.id} enviado · processando…`,
        texto: `${linhas(data.linhasValidas)} gravadas. Os indicadores estão sendo calculados.`,
      })
      acompanharLote(notificacao)

      return true
    } catch (erro) {
      const mensagem = explicarRecusa(erro)
      estado.value = 'falha'
      falha.value = `Falha ao enviar: ${mensagem}`

      notificacoes.atualizar(notificacao, {
        tipo: 'erro',
        titulo: 'Falha no envio',
        texto: `${mensagem} A planilha continua carregada — dá para tentar de novo sem selecionar o arquivo outra vez.`,
      })

      return false
    }
  }

  // -------------------------------------------------------------------------
  // Acompanhamento do lote: o Python roda depois da resposta do envio, então a
  // tela pergunta o status (GET /api/dashboard?loteId=) até ele terminar.
  // -------------------------------------------------------------------------
  const INTERVALO_ETL = 2000
  const LIMITE_ETL = 5 * 60 * 1000
  let timerEtl = null

  function pararAcompanhamento() {
    clearTimeout(timerEtl)
    timerEtl = null
  }

  function acompanharLote(notificacao) {
    pararAcompanhamento()
    const id = lote.value.id
    const inicio = Date.now()

    async function perguntar() {
      try {
        const resposta = await dashboardService.obter(id)
        if (lote.value?.id !== id) return // outra planilha entrou no meio
        lote.value = resposta.lote
        if (etapaEtl.value === 'concluido') {
          resumoEtl.value = resposta.estatisticas?.resumo ?? null
          notificacoes.atualizar(notificacao, {
            tipo: 'sucesso',
            titulo: 'Planilha processada',
            texto: `Lote ${id}: ${linhas(resposta.lote.linhasValidas)} gravadas e indicadores prontos no Dashboard.`,
          })
          return
        }
        if (etapaEtl.value === 'erro') {
          notificacoes.atualizar(notificacao, {
            tipo: 'erro',
            titulo: 'O processamento falhou',
            texto: explicarLote(resposta.lote),
          })
          return
        }
      } catch {
        // Falha passageira de rede: tenta de novo na próxima volta.
      }
      if (Date.now() - inicio > LIMITE_ETL) {
        semRespostaEtl.value = true
        return
      }
      timerEtl = setTimeout(perguntar, INTERVALO_ETL)
    }

    timerEtl = setTimeout(perguntar, INTERVALO_ETL)
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
    lote,
    resumoEtl,
    semRespostaEtl,
    inicioEnvio,
    duracaoEnvio,
    validacaoServidor,
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
    podeValidar,
    podeEnviar,
    etapaEtl,
    // actions
    limpar,
    validarDados,
    carregar,
    validarNoServidor,
    enviar,
  }
})
