import { defineStore } from 'pinia'
import { ref } from 'vue'
import { adminService } from '../services/api'

// Contas aguardando aprovação: o menu mostra o número e a tela de usuários
// atualiza depois de cada ação. Só é consultado por quem é ADMIN.
export const useAdminStore = defineStore('admin', () => {
  const pendentes = ref(0)

  async function atualizarPendentes() {
    try {
      pendentes.value = (await adminService.listar('PENDENTE')).length
    } catch {
      // Sem resposta: o contador fica como estava; a tela de usuários mostra o erro.
    }
  }

  return { pendentes, atualizarPendentes }
})
