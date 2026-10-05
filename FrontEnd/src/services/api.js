import axios from 'axios'

// Endereço do Spring Boot. Em produção vem de VITE_API_URL (ex.: no painel da
// Vercel); sem ela, usa o back-end rodando na máquina.
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
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

// O back-end responde erro no formato ProblemDetail: { title, detail, campos }.
// Esta função tira dali a frase que a tela mostra.
export function mensagemDeErro(erro) {
  const dados = erro.response?.data
  if (!erro.response) {
    return 'Não foi possível falar com o servidor. Tente de novo em instantes.'
  }
  if (dados?.campos) {
    return Object.values(dados.campos)[0]
  }
  return dados?.detail || 'Algo deu errado. Tente de novo.'
}
