import { defineStore } from 'pinia'
import { api, mensagemDeErro } from '../services/api'

// Formato de opções (state / getters / actions), como na aula 07. As outras
// stores do projeto usam o formato setup; os dois são Pinia e convivem sem
// problema.

const CHAVES = ['cti_token', 'cti_expira_em', 'cti_usuario', 'cti_consultor']

export const useAuthStore = defineStore('auth', {
  // STATE = dados guardados enquanto a aplicação está em uso. Começa lendo o
  // localStorage para que atualizar a página não desconecte ninguém.
  state: () => ({
    token: localStorage.getItem('cti_token') || '',
    expiraEm: localStorage.getItem('cti_expira_em') || '',
    usuario: localStorage.getItem('cti_usuario') || '',
    consultor: localStorage.getItem('cti_consultor') || '',
  }),

  // GETTERS = valores calculados do state. "Logado" é ter token dentro da
  // validade: token vencido no localStorage não abre as telas.
  getters: {
    logado: (state) => Boolean(state.token) && new Date(state.expiraEm) > new Date(),
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

    // O próprio consultor cria a conta e já entra: o cadastro não devolve
    // token, então o login vem logo em seguida com a mesma senha.
    async cadastrar(dados) {
      try {
        await api.post('/usuarios', dados)
      } catch (erro) {
        return mensagemDeErro(erro)
      }
      return this.login(dados.email, dados.senha)
    },

    // Só o token e o que identifica a pessoa vão para o localStorage; a
    // senha nunca.
    entrar({ token, expiraEm, usuario }) {
      this.token = token
      this.expiraEm = expiraEm
      this.usuario = usuario.nome
      this.consultor = usuario.consultor || ''

      localStorage.setItem('cti_token', this.token)
      localStorage.setItem('cti_expira_em', this.expiraEm)
      localStorage.setItem('cti_usuario', this.usuario)
      localStorage.setItem('cti_consultor', this.consultor)
    },

    logout() {
      this.token = ''
      this.expiraEm = ''
      this.usuario = ''
      this.consultor = ''

      // Inclui a chave da versão anterior, que marcava "logado" sem token.
      for (const chave of [...CHAVES, 'cti_logado']) localStorage.removeItem(chave)
    },
  },
})
