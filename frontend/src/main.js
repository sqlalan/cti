import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import router from './router'
import { configurarSessao } from './services/api'
import { useAuthStore } from './stores/authStore'

const app = createApp(App).use(createPinia()).use(router)

// Token vencido em qualquer chamada: encerra a sessão e volta ao login avisando.
const auth = useAuthStore()
configurarSessao({
  token: () => auth.token,
  // Conta bloqueada pelo admin também chega aqui (401): a mensagem diz qual dos dois foi.
  expirou: (mensagem) => {
    auth.logout()
    router.push({ name: 'login', query: { sessao: mensagem?.includes('liberada') ? 'suspensa' : 'expirada' } })
  },
})

app.mount('#app')
