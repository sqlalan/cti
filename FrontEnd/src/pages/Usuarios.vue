<script setup>
import { computed, onMounted, ref } from 'vue'
import StatusConta from '../components/StatusConta.vue'
import { adminService, mensagemDeErro } from '../services/api'
import { useAdminStore } from '../stores/adminStore'
import { useAuthStore } from '../stores/authStore'
import { useNotificacoesStore } from '../stores/notificacoes'

// Liberação de acesso (só ADMIN): o cadastro público nasce PENDENTE e só entra
// depois de aprovado aqui. Bloquear tira o acesso na hora.
const auth = useAuthStore()
const admin = useAdminStore()
const notificacoes = useNotificacoesStore()

const FILTROS = [
  { status: 'PENDENTE', rotulo: 'Pendentes' },
  { status: 'APROVADO', rotulo: 'Aprovados' },
  { status: 'BLOQUEADO', rotulo: 'Bloqueados' },
  { status: '', rotulo: 'Todos' },
]

const filtro = ref('PENDENTE')
const contas = ref([]) // todas; o filtro é feito aqui para os contadores das abas
const estado = ref('carregando') // carregando | pronto | erro
const erro = ref('')
const ocupado = ref(null) // id da conta com ação em andamento

const visiveis = computed(() => (filtro.value ? contas.value.filter((c) => c.status === filtro.value) : contas.value))
const contagem = (status) => (status ? contas.value.filter((c) => c.status === status).length : contas.value.length)
const souEu = (conta) => conta.email === auth.email

async function carregar() {
  estado.value = 'carregando'
  try {
    contas.value = await adminService.listar()
    estado.value = 'pronto'
    // Sem pendentes, abre direto nos aprovados.
    if (filtro.value === 'PENDENTE' && !contagem('PENDENTE')) filtro.value = 'APROVADO'
  } catch (falha) {
    erro.value = mensagemDeErro(falha)
    estado.value = 'erro'
  }
}

onMounted(carregar)

// Ações por situação. Bloquear e mudar perfil pedem confirmação: mexem no acesso de alguém.
function acoes(conta) {
  if (souEu(conta)) return []
  const lista = []
  if (conta.status === 'PENDENTE') lista.push({ rotulo: 'Aprovar', principal: true, executar: () => mudarStatus(conta, 'APROVADO') })
  if (conta.status === 'BLOQUEADO') lista.push({ rotulo: 'Reativar', principal: true, executar: () => mudarStatus(conta, 'APROVADO') })
  if (conta.status !== 'BLOQUEADO') lista.push({ rotulo: 'Bloquear', perigo: true, executar: () => mudarStatus(conta, 'BLOQUEADO') })
  if (conta.status === 'APROVADO') {
    lista.push(conta.perfil === 'ADMIN'
      ? { rotulo: 'Remover admin', executar: () => mudarPerfil(conta, 'USER') }
      : { rotulo: 'Tornar admin', executar: () => mudarPerfil(conta, 'ADMIN') })
  }
  return lista
}

const TEXTO_STATUS = { APROVADO: 'aprovada', BLOQUEADO: 'bloqueada', PENDENTE: 'pendente' }

async function mudarStatus(conta, status) {
  if (status === 'BLOQUEADO' && !window.confirm(`Bloquear ${conta.nome}? A pessoa perde o acesso na hora.`)) return
  await executar(conta, () => adminService.alterarStatus(conta.id, status), `Conta de ${conta.nome} ${TEXTO_STATUS[status]}`)
}

async function mudarPerfil(conta, perfil) {
  const texto = perfil === 'ADMIN'
    ? `Tornar ${conta.nome} administrador? Ela poderá aprovar e bloquear contas.`
    : `Remover ${conta.nome} dos administradores?`
  if (!window.confirm(texto)) return
  await executar(
    conta,
    () => adminService.alterarPerfil(conta.id, perfil),
    perfil === 'ADMIN' ? `${conta.nome} agora é administrador` : `${conta.nome} não é mais administrador`,
    'Vale a partir do próximo login da pessoa.',
  )
}

async function executar(conta, chamada, titulo, texto = '') {
  ocupado.value = conta.id
  try {
    const atualizada = await chamada()
    contas.value = contas.value.map((c) => (c.id === atualizada.id ? atualizada : c))
    notificacoes.notificar({ tipo: 'sucesso', titulo, texto })
    admin.atualizarPendentes()
  } catch (falha) {
    notificacoes.notificar({ tipo: 'erro', titulo: 'Não foi possível concluir', texto: mensagemDeErro(falha) })
  } finally {
    ocupado.value = null
  }
}

const data = (iso) => new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })

const classeBotao = (acao) =>
  [
    'rounded px-3 py-1.5 text-sm font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 disabled:cursor-not-allowed disabled:opacity-50',
    acao.principal
      ? 'bg-brand-800 text-white hover:bg-brand-900'
      : acao.perigo
        ? 'border border-erro-600/50 text-erro-600 hover:bg-erro-600/5 dark:border-erro-400/50 dark:text-erro-400'
        : 'border border-ink-200 text-brand-800 hover:border-brand-500/50 dark:border-night-700 dark:text-brand-300',
  ].join(' ')
</script>

<template>
  <h1 class="text-2xl font-bold text-ink-900 dark:text-mist-100">Usuários</h1>
  <p class="mt-2 text-sm text-ink-600 dark:text-mist-300">
    Contas criadas no cadastro ficam pendentes até serem aprovadas aqui. Bloquear tira o acesso na hora.
  </p>

  <!-- Filtros: quebram a linha no celular, todos ficam visíveis. -->
  <div class="mt-6 flex flex-wrap gap-2" role="tablist" aria-label="Filtrar contas por situação">
    <button
      class="shrink-0 rounded-full border px-4 py-1.5 text-sm font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
      :class="filtro === f.status
        ? 'border-brand-800 bg-brand-800 text-white dark:border-brand-700 dark:bg-brand-700'
        : 'border-ink-200 bg-white text-ink-600 hover:border-brand-500/50 dark:border-night-700 dark:bg-night-900 dark:text-mist-300'"
      type="button"
      role="tab"
      :aria-selected="filtro === f.status"
      v-for="f in FILTROS"
      :key="f.rotulo"
      @click="filtro = f.status"
    >
      {{ f.rotulo }}
      <span class="ml-1 tabular-nums opacity-80" v-if="estado === 'pronto'">{{ contagem(f.status) }}</span>
    </button>
  </div>

  <p class="mt-6 text-sm text-ink-600 dark:text-mist-300" v-if="estado === 'carregando'" role="status">Carregando as contas…</p>

  <div
    class="mt-6 rounded border border-erro-600 bg-erro-600/5 p-4 text-sm dark:border-erro-400 dark:bg-erro-400/10"
    v-else-if="estado === 'erro'"
    role="alert"
  >
    <p class="text-ink-900 dark:text-mist-100">Não foi possível carregar as contas: {{ erro }}</p>
    <button class="mt-3 font-semibold text-brand-800 underline dark:text-brand-300" type="button" @click="carregar">
      Tentar de novo
    </button>
  </div>

  <template v-else>
    <p
      class="mt-6 rounded border border-ink-200 bg-white px-4 py-6 text-center text-sm text-ink-600 dark:border-night-700 dark:bg-night-900 dark:text-mist-300"
      v-if="!visiveis.length"
    >
      {{ filtro === 'PENDENTE' ? 'Nenhuma conta aguardando aprovação.' : 'Nenhuma conta nesta situação.' }}
    </p>

    <!-- Computador e tablet: tabela. -->
    <div
      class="mt-6 hidden overflow-x-auto rounded border border-ink-200 bg-white md:block dark:border-night-700 dark:bg-night-900"
      v-if="visiveis.length"
    >
      <table class="w-full text-left text-sm">
        <thead>
          <tr class="border-b border-ink-200 dark:border-night-700">
            <th class="px-4 py-3 font-medium text-ink-600 dark:text-mist-300" scope="col">Nome</th>
            <th class="px-4 py-3 font-medium text-ink-600 dark:text-mist-300" scope="col">E-mail</th>
            <th class="px-4 py-3 font-medium text-ink-600 dark:text-mist-300" scope="col">Cadastro</th>
            <th class="px-4 py-3 font-medium text-ink-600 dark:text-mist-300" scope="col">Situação</th>
            <th class="px-4 py-3 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Ações</th>
          </tr>
        </thead>
        <tbody>
          <tr class="border-b border-ink-200 last:border-0 dark:border-night-700" v-for="conta in visiveis" :key="conta.id">
            <td class="px-4 py-3">
              <p class="font-medium text-ink-900 dark:text-mist-100">{{ conta.nome }}</p>
              <p class="text-xs text-ink-600 dark:text-mist-300" v-if="conta.consultor && conta.consultor !== conta.nome">
                Consultor: {{ conta.consultor }}
              </p>
            </td>
            <td class="px-4 py-3 break-all text-ink-600 dark:text-mist-300">{{ conta.email }}</td>
            <td class="px-4 py-3 whitespace-nowrap tabular-nums text-ink-600 dark:text-mist-300">{{ data(conta.criadoEm) }}</td>
            <td class="px-4 py-3"><StatusConta :status="conta.status" :perfil="conta.perfil" /></td>
            <td class="px-4 py-3">
              <div class="flex flex-wrap justify-end gap-2">
                <span class="text-xs text-ink-400 dark:text-mist-500" v-if="souEu(conta)">Sua conta</span>
                <button
                  :class="classeBotao(acao)"
                  type="button"
                  :disabled="ocupado === conta.id"
                  v-for="acao in acoes(conta)"
                  :key="acao.rotulo"
                  @click="acao.executar"
                >
                  {{ acao.rotulo }}
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Celular: um cartão por conta, com os botões embaixo. -->
    <ul class="mt-6 grid gap-3 md:hidden" v-if="visiveis.length">
      <li
        class="rounded border border-ink-200 bg-white p-4 dark:border-night-700 dark:bg-night-900"
        v-for="conta in visiveis"
        :key="conta.id"
      >
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0">
            <p class="font-medium text-ink-900 dark:text-mist-100">{{ conta.nome }}</p>
            <p class="break-all text-sm text-ink-600 dark:text-mist-300">{{ conta.email }}</p>
          </div>
          <StatusConta :status="conta.status" :perfil="conta.perfil" />
        </div>
        <p class="mt-2 text-xs text-ink-600 dark:text-mist-300">
          Cadastro em {{ data(conta.criadoEm) }}<template v-if="conta.consultor && conta.consultor !== conta.nome"> · Consultor: {{ conta.consultor }}</template>
        </p>
        <p class="mt-3 text-xs text-ink-400 dark:text-mist-500" v-if="souEu(conta)">Sua conta</p>
        <div class="mt-3 flex flex-wrap gap-2" v-else>
          <button
            :class="classeBotao(acao)"
            type="button"
            :disabled="ocupado === conta.id"
            v-for="acao in acoes(conta)"
            :key="acao.rotulo"
            @click="acao.executar"
          >
            {{ acao.rotulo }}
          </button>
        </div>
      </li>
    </ul>
  </template>
</template>
