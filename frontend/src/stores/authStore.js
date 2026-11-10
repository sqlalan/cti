import { defineStore } from 'pinia'
import { api, mensagemDeErro } from '../services/api'

// Formato de opções (state / getters / actions), como na aula 07. As outras
// stores do projeto usam o formato setup; os dois são Pinia e convivem sem
// problema.

const CHAVES = ['cti_token', 'cti_expira_em', 'cti_usuario', 'cti_consultor', 'cti_email', 'cti_perfil']

export const useAuthStore = defineStore('auth', {
  // STATE = dados guardados enquanto a aplicação está em uso. Começa lendo o
  // localStorage para que atualizar a página não desconecte ninguém.
  state: () => ({
    token: localStorage.getItem('cti_token') || '',
    expiraEm: localStorage.getItem('cti_expira_em') || '',
    usuario: localStorage.getItem('cti_usuario') || '',
    consultor: localStorage.getItem('cti_consultor') || '',
    // E-mail e perfil: o menu de administração e a tela de usuários dependem deles.
    email: localStorage.getItem('cti_email') || '',
    perfil: localStorage.getItem('cti_perfil') || '',
    // E-mail da conta recém-criada, para a tela de login já trazer preenchido.
    // Fica só na memória: não é sessão, e some depois de lido.
    contaCriada: '',
  }),

  // GETTERS = valores calculados do state. "Logado" é ter token dentro da
  // validade: token vencido no localStorage não abre as telas.
  getters: {
    logado: (state) => Boolean(state.token) && new Date(state.expiraEm) > new Date(),
    nomeExibicao: (state) => state.usuario || 'Usuário',
    // Só esconde o menu: quem garante o acesso é o servidor (rotas de ADMIN).
    ehAdmin: (state) => state.perfil === 'ADMIN',
  },

  // ACTIONS = ações que alteram o state. Login e cadastro devolvem a mensagem
  // de erro (ou '' quando deu certo) para a tela mostrar.
  actions: {
    async login(email, senha) {
      if (!email || !senha) {
        return 'Preencha e-mail e senha.'
      }
      try {
        const { data } = await api.post('/auth/login', { email, senha })
        this.entrar(data)
        return ''
      } catch (erro) {
        return mensagemDeErro(erro)
      }
    },

    // O próprio consultor cria a conta, mas só entra pelo login: cadastro e
    // autenticação ficam separados, e o token sai sempre de /auth/login.
    async cadastrar(dados) {
      try {
        await api.post('/auth/register', dados)
      } catch (erro) {
        return mensagemDeErro(erro)
      }
      this.contaCriada = dados.email
      return ''
    },

    // Só o token e o que identifica a pessoa vão para o localStorage; a
    // senha nunca.
    entrar({ token, expiraEm, usuario }) {
      this.token = token
      this.expiraEm = expiraEm
      this.usuario = usuario.nome
      this.consultor = usuario.consultor || ''
      this.email = usuario.email || ''
      this.perfil = usuario.perfil || ''

      localStorage.setItem('cti_token', this.token)
      localStorage.setItem('cti_expira_em', this.expiraEm)
      localStorage.setItem('cti_usuario', this.usuario)
      localStorage.setItem('cti_consultor', this.consultor)
      localStorage.setItem('cti_email', this.email)
      localStorage.setItem('cti_perfil', this.perfil)
    },

    logout() {
      this.token = ''
      this.expiraEm = ''
      this.usuario = ''
      this.consultor = ''
      this.email = ''
      this.perfil = ''

      // Inclui a chave da versão anterior, que marcava "logado" sem token.
      for (const chave of [...CHAVES, 'cti_logado']) localStorage.removeItem(chave)
    },
  },
})
