import axios from 'axios'

// Endereço do Spring Boot. Em produção vem de VITE_API_URL (ex.: no painel da
// Vercel); sem ela, usa o back-end rodando na máquina. As rotas da API ficam
// em /api: o guia da disciplina escreve VITE_API_URL=http://localhost:8080,
// então o /api é acrescentado quando falta, e as duas formas funcionam.
function enderecoDaApi(configurado) {
  const base = (configurado || 'http://localhost:8080').replace(/\/+$/, '')
  return base.endsWith('/api') ? base : `${base}/api`
}

export const api = axios.create({
  baseURL: enderecoDaApi(import.meta.env.VITE_API_URL),
})

// O token mora no localStorage, mas quem decide o que fazer com ele é a store
// de autenticação. Aqui só há dois ganchos, ligados em main.js: ler o token e
// reagir à sessão vencida. Assim este arquivo não importa a store (que importa
// este arquivo) nem o router.
let lerToken = () => null
let aoExpirar = () => {}

export function configurarSessao({ token, expirou }) {
  lerToken = token
  aoExpirar = expirou
}

api.interceptors.request.use((config) => {
  const token = lerToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (resposta) => resposta,
  (erro) => {
    // 401 com token = token vencido ou inválido. Sem token (login errado) a
    // tela de login mostra a mensagem da API e não há sessão para encerrar.
    if (erro.response?.status === 401 && erro.config?.headers?.Authorization) {
      aoExpirar()
    }
    return Promise.reject(erro)
  },
)

// O back-end responde erro como { mensagem, erro?, campo?, campos? }: "erro" é o
// detalhe (o primeiro campo recusado, ou a regra do Service), "mensagem" o
// resumo. Esta função tira dali a frase que a tela mostra.
export function mensagemDeErro(erro) {
  const dados = erro.response?.data
  if (!erro.response) {
    return 'Não foi possível falar com o servidor. Tente de novo em instantes.'
  }
  if (dados?.erro) {
    return dados.erro
  }
  if (dados?.campos) {
    return Object.values(dados.campos)[0]
  }
  return dados?.mensagem || 'Algo deu errado. Tente de novo.'
}

const corpo = (resposta) => resposta.data // o axios coloca a resposta em .data; esta função pega só ela

export const uploadService = { // ---- envio da planilha ----
  // POST só com as linhas aprovadas pelo Pinia; arquivo e recebidas vão na URL
  enviarValidadas: (linhas, { arquivo, recebidas }) =>
    api.post('/uploads/validadas', linhas, { params: { arquivo, recebidas } }).then(corpo),
  listar: () => api.get('/uploads').then(corpo), // GET lista de lotes enviados
}

export const dashboardService = { // ---- dados dos gráficos ----
  obter: (loteId) => api.get('/dashboard', { params: { loteId } }).then(corpo), // GET -> { lote, geradoEm, estatisticas }
}
