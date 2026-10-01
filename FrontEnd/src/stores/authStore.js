import { defineStore } from 'pinia'
import { api, mensagemDeErro } from '../services/api'

// Formato de opções (state / getters / actions), como na aula 07. As outras
// stores do projeto usam o formato setup; os dois são Pinia e convivem sem
// problema.
export const useAuthStore = defineStore('auth', {
  // STATE = dados guardados enquanto a aplicação está em uso. Começa lendo o
  // localStorage para que atualizar a página não desconecte ninguém.
  state: () => ({
    logado: localStorage.getItem('cti_logado') === 'true',
    usuario: localStorage.getItem('cti_usuario') || '',
    consultor: localStorage.getItem('cti_consultor') || '',
  }),

  // GETTERS = valores calculados do state.
  getters: {
    nomeExibicao: (state) => state.usuario || 'Usuário',
  },

  // ACTIONS = ações que alteram o state. Login e cadastro devolvem a mensagem
  // de erro (ou '' quando deu certo) para a tela mostrar.
  actions: {
    async login(email, senha) {
      if (!email || !senha) {
        return 'Preencha e-mail e senha.'
      }
      try {
        const { data } = await api.post('/usuarios/login', { email, senha })
        this.entrar(data)
        return ''
      } catch (erro) {
        return mensagemDeErro(erro)
      }
    },

    // O próprio consultor cria a conta e já entra no sistema.
    async cadastrar(dados) {
      try {
        const { data } = await api.post('/usuarios', dados)
        this.entrar(data)
        return ''
      } catch (erro) {
        return mensagemDeErro(erro)
      }
    },

    // A senha nunca vai para o localStorage, só o que identifica a sessão.
    entrar(usuario) {
      this.logado = true
      this.usuario = usuario.nome
      this.consultor = usuario.consultor || ''

      localStorage.setItem('cti_logado', 'true')
      localStorage.setItem('cti_usuario', this.usuario)
      localStorage.setItem('cti_consultor', this.consultor)
    },

    logout() {
      this.logado = false
      this.usuario = ''
      this.consultor = ''

      localStorage.removeItem('cti_logado')
      localStorage.removeItem('cti_usuario')
      localStorage.removeItem('cti_consultor')
    },
  },
})
