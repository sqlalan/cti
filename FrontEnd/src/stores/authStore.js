import { defineStore } from 'pinia'

// Formato de opções (state / getters / actions), como na aula 07. As outras
// stores do projeto usam o formato setup; os dois são Pinia e convivem sem
// problema.
export const useAuthStore = defineStore('auth', {
  // STATE = dados guardados enquanto a aplicação está em uso. Começa lendo o
  // localStorage para que atualizar a página não desconecte ninguém.
  state: () => ({
    logado: localStorage.getItem('cti_logado') === 'true',
    usuario: localStorage.getItem('cti_usuario') || '',
  }),

  // GETTERS = valores calculados do state.
  getters: {
    nomeExibicao: (state) => state.usuario || 'Usuário',
  },

  // ACTIONS = ações que alteram o state.
  actions: {
    login(usuario, senha) {
      // Aula de front-end: qualquer usuário e senha preenchidos são aceitos.
      // Quando o Spring Boot existir, é aqui que entra a chamada à API — e a
      // senha nunca vai para o localStorage, só o que identifica a sessão.
      if (!usuario || !senha) {
        return false
      }

      this.logado = true
      this.usuario = usuario

      localStorage.setItem('cti_logado', 'true')
      localStorage.setItem('cti_usuario', usuario)

      return true
    },

    logout() {
      this.logado = false
      this.usuario = ''

      localStorage.removeItem('cti_logado')
      localStorage.removeItem('cti_usuario')
    },
  },
})
