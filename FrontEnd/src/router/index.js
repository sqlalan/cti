import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../layouts/AppLayout.vue'
import Home from '../pages/Home.vue'
import Login from '../pages/Login.vue'
import Cadastro from '../pages/Cadastro.vue'
import Dashboard from '../pages/Dashboard.vue'
import Upload from '../pages/Upload.vue'
import Relatorio from '../pages/Relatorio.vue'

// Os caminhos só existem aqui. O resto do app navega pelo NOME da rota
// ({ name: 'upload' }), então trocar uma URL é mudar uma linha deste arquivo —
// com caminho escrito à mão espalhado pelas telas, o link que escapasse da
// troca quebraria em silêncio, sem erro nenhum no build.
const routes = [
  { path: '/', name: 'home', component: Home },
  { path: '/login', name: 'login', component: Login },
  { path: '/cadastro', name: 'cadastro', component: Cadastro },
  {
    // O layout não acrescenta segmento à URL: a sidebar é como a tela é
    // desenhada, não uma área do sistema. As telas ficam em /upload,
    // /dashboard e /relatorio, como na aula. Dividir o caminho '/' com a home
    // é seguro porque este registro não tem filho vazio — ele só casa quando
    // há um dos segmentos abaixo, e '/' sozinho continua sendo a home.
    path: '/',
    component: AppLayout,
    children: [
      { path: 'dashboard', name: 'dashboard', component: Dashboard },
      { path: 'upload', name: 'upload', component: Upload },
      { path: 'relatorio', name: 'relatorio', component: Relatorio },
    ],
  },

  // Endereços antigos (até a aula 07 as telas moravam em /sidebar/...).
  // Mantidos para não quebrar link salvo, favorito ou o que já foi publicado.
  { path: '/sidebar', redirect: { name: 'upload' } },
  { path: '/sidebar/:tela', redirect: (to) => `/${to.params.tela}` },
]

export default createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, behavior: 'smooth' }
    return { top: 0 }
  },
})
