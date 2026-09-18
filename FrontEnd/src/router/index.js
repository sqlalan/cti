import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../layouts/AppLayout.vue'
import Home from '../pages/Home.vue'
import Login from '../pages/Login.vue'
import Dashboard from '../pages/Dashboard.vue'
import Upload from '../pages/Upload.vue'
import Relatorio from '../pages/Relatorio.vue'

const routes = [
  { path: '/', name: 'home', component: Home },
  { path: '/login', name: 'login', component: Login },
  {
    path: '/sidebar',
    component: AppLayout,
    redirect: '/sidebar/upload', // Redireciona automaticamente ao acessar /sidebar
    children: [
      { path: 'dashboard', name: 'dashboard', component: Dashboard },
      { path: 'upload', name: 'upload', component: Upload },
      { path: 'relatorio', name: 'relatorio', component: Relatorio },
    ],
  },
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
