import axios from 'axios'

// Endereço do Spring Boot. Em produção vem de VITE_API_URL (ex.: no painel da
// Vercel); sem ela, usa o back-end rodando na máquina.
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
})

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
