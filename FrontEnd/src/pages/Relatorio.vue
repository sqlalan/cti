<script setup>
import { computed, onMounted, ref } from 'vue'
import { formatarNumero } from '../components/graficos/escala'
import { TIPOS_AVISO, TIPOS_ERRO } from '../dados/esquemaPlanilha'
import { api, mensagemDeErro } from '../services/api'
import { useUploadStore } from '../stores/uploadStore'

// Relatório do PROCESSAMENTO dos dados. A visão comercial da carteira mora no
// Dashboard; aqui fica o que aconteceu com a planilha:
//  - a validação da planilha carregada nesta sessão, lida do Pinia (é o que o
//    store existe para resolver: o dado atravessa a navegação do Upload até
//    aqui sem passar por prop nem por URL);
//  - o último processamento gravado no servidor e o histórico de envios, que
//    continuam disponíveis depois de recarregar a página.
const planilha = useUploadStore()

// carregando → pronto | vazio (nenhuma planilha processada) | erro
const estadoServidor = ref('carregando')
const erroServidor = ref('')
const ultima = ref(null) // GET /api/analises/ultima
const qualidade = ref(null) // GET /api/indicadores → qualidade (o tratamento em Python)
const historico = ref([]) // GET /api/analises

async function carregarServidor() {
  estadoServidor.value = 'carregando'
  erroServidor.value = ''
  try {
    const [analise, indicadores, envios] = await Promise.all([
      api.get('/analises/ultima'),
      api.get('/indicadores'),
      api.get('/analises'),
    ])
    ultima.value = analise.data
    qualidade.value = indicadores.data.qualidade
    historico.value = envios.data
    estadoServidor.value = 'pronto'
  } catch (falha) {
    // 404 aqui não é erro: só quer dizer que ninguém enviou planilha ainda.
    if (falha.response?.status === 404) {
      estadoServidor.value = 'vazio'
    } else {
      erroServidor.value = mensagemDeErro(falha)
      estadoServidor.value = 'erro'
    }
  }
}

onMounted(carregarServidor)

// O Python usa os mesmos tipos de erro e de tratamento do navegador, então os
// quadros do servidor saem com os mesmos rótulos dos quadros do Pinia.
const quadroServidor = computed(() => [
  ...TIPOS_ERRO.map(({ tipo, rotulo }) => ({ tipo, rotulo, quantidade: qualidade.value.erros_por_tipo?.[tipo] ?? 0 })),
  { tipo: 'validos', rotulo: 'Registros válidos', quantidade: qualidade.value.linhas_validas },
])

const tratamentosServidor = computed(() =>
  TIPOS_AVISO.map(({ tipo, rotulo }) => ({ tipo, rotulo, quantidade: qualidade.value.avisos_por_tipo?.[tipo] ?? 0 })),
)

const inteiro = (valor) => valor.toLocaleString('pt-BR')
const porcento = (valor) => `${formatarNumero(valor, 1)}%`
const dataHora = (iso) =>
  new Date(iso).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })

// Cor no quadro de validação: verde só na linha dos válidos, vermelho na de
// erro que realmente aconteceu, cinza no zero. Pintar o zero de vermelho
// alarmaria por uma verificação que passou, e pintar tudo de vermelho apagaria
// a diferença entre "3 duplicados" e "nenhum duplicado".
function quantidadeCor(item) {
  if (item.tipo === 'validos') return 'text-ok-600 dark:text-ok-400'
  if (!item.quantidade) return 'text-ink-400 dark:text-mist-500'
  return 'text-erro-600 dark:text-erro-400'
}
</script>

<template>
  <h1 class="text-2xl font-bold text-ink-900 dark:text-mist-100">Relatórios</h1>
  <p class="mt-2 text-sm text-ink-600 dark:text-mist-300">
    Processamento das planilhas: validação, erros encontrados, tratamentos aplicados e histórico de envios.
  </p>

  <!-- =====================================================================
       Relatório da validação.

       Guardado por quantidadeLinhas, não por linhas VÁLIDAS: uma planilha em
       que tudo falhou é justamente quando o relatório de erros mais importa.
       ===================================================================== -->
  <section class="mt-8" v-if="planilha.quantidadeLinhas > 0">
    <h2 class="text-lg font-semibold text-ink-900 dark:text-mist-100">Validação da planilha</h2>

    <dl
      class="mt-4 grid grid-cols-2 gap-px overflow-hidden rounded border border-ink-200 bg-ink-200 sm:grid-cols-4 dark:border-night-700 dark:bg-night-700"
    >
      <div class="bg-white px-4 py-3 dark:bg-night-900">
        <dt class="text-xs text-ink-600 dark:text-mist-300">Arquivo analisado</dt>
        <dd
          class="mt-1 truncate text-sm font-semibold text-ink-900 dark:text-mist-100"
          :title="planilha.nomeArquivo"
        >
          {{ planilha.nomeArquivo || '—' }}
        </dd>
      </div>
      <div class="bg-white px-4 py-3 dark:bg-night-900">
        <dt class="text-xs text-ink-600 dark:text-mist-300">Total de registros</dt>
        <dd class="mt-1 text-sm font-semibold tabular-nums text-ink-900 dark:text-mist-100">
          {{ inteiro(planilha.quantidadeLinhas) }}
        </dd>
      </div>
      <div class="bg-white px-4 py-3 dark:bg-night-900">
        <dt class="text-xs text-ink-600 dark:text-mist-300">Registros válidos</dt>
        <dd class="mt-1 text-sm font-semibold tabular-nums text-ok-600 dark:text-ok-400">
          {{ inteiro(planilha.quantidadeValidas) }}
        </dd>
      </div>
      <div class="bg-white px-4 py-3 dark:bg-night-900">
        <dt class="text-xs text-ink-600 dark:text-mist-300">Registros com erro</dt>
        <dd
          class="mt-1 text-sm font-semibold tabular-nums"
          :class="planilha.quantidadeInvalidas
            ? 'text-erro-600 dark:text-erro-400'
            : 'text-ink-900 dark:text-mist-100'"
        >
          {{ inteiro(planilha.quantidadeInvalidas) }}
        </dd>
      </div>
    </dl>

    <div class="mt-4 grid gap-4 lg:grid-cols-2">
      <!-- O quadro "Validação realizada / Quantidade". -->
      <div class="rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
        <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Validação realizada</h3>
        <table class="mt-3 w-full text-left text-sm">
          <thead>
            <tr class="border-b border-ink-200 dark:border-night-700">
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">
                Verificação
              </th>
              <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                Quantidade
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              class="border-b border-ink-200 last:border-0 dark:border-night-700"
              v-for="item in planilha.quadroValidacao"
              :key="item.tipo"
            >
              <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
              <td
                class="py-2 text-right font-semibold tabular-nums"
                :class="quantidadeCor(item)"
              >
                {{ inteiro(item.quantidade) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Correções que o tratamento aplicou sozinho. Separado dos erros
           porque não reprovam linha nenhuma: misturar os dois quadros somaria
           problema resolvido com problema pendente. -->
      <div class="rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
        <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">
          Tratamentos aplicados
        </h3>
        <p class="mt-1 text-xs text-ink-600 dark:text-mist-300">
          A linha passou; o valor foi ajustado. Corrija na origem para a planilha
          não depender do tratamento. Célula vazia não entra aqui — quando a coluna
          é obrigatória, ela reprova a linha e é contada ao lado, em
          <span class="font-medium">Campos obrigatórios vazios</span>.
        </p>
        <table class="mt-3 w-full text-left text-sm">
          <thead>
            <tr class="border-b border-ink-200 dark:border-night-700">
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">
                Tratamento
              </th>
              <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">
                Quantidade
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              class="border-b border-ink-200 last:border-0 dark:border-night-700"
              v-for="item in planilha.avisosPorTipo"
              :key="item.tipo"
            >
              <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
              <td
                class="py-2 text-right font-semibold tabular-nums"
                :class="item.quantidade
                  ? 'text-alerta-600 dark:text-alerta-400'
                  : 'text-ink-400 dark:text-mist-500'"
              >
                {{ inteiro(item.quantidade) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Uma linha por problema: linha, campo e descrição, como o relatório
         pede. Um registro com dois defeitos aparece duas vezes aqui e uma vez
         só na contagem de "registros com erro". -->
    <div
      class="mt-4 min-w-0 rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900"
      v-if="planilha.totalOcorrencias"
    >
      <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">
        Problemas encontrados ({{ inteiro(planilha.totalOcorrencias) }})
      </h3>
      <p class="mt-1 text-xs text-ink-600 dark:text-mist-300">
        Em {{ inteiro(planilha.quantidadeInvalidas) }}
        {{ planilha.quantidadeInvalidas === 1 ? 'registro' : 'registros' }}.
      </p>

      <div class="mt-3 max-h-96 overflow-auto">
        <table class="w-full min-w-2xl text-left text-sm">
          <thead class="sticky top-0 bg-white dark:bg-night-900">
            <tr class="border-b border-ink-200 dark:border-night-700">
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Linha</th>
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Campo</th>
              <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Tipo</th>
              <th class="py-2 font-medium text-ink-600 dark:text-mist-300" scope="col">
                Descrição do erro
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              class="border-b border-ink-200 last:border-0 dark:border-night-700"
              v-for="(item, i) in planilha.ocorrencias"
              :key="`${item.linha}-${item.campo}-${i}`"
            >
              <td class="py-2 pr-4 tabular-nums text-ink-400 dark:text-mist-500">
                {{ item.linha }}
              </td>
              <td class="py-2 pr-4 whitespace-nowrap font-medium text-ink-900 dark:text-mist-100">
                {{ item.campo }}
              </td>
              <td class="py-2 pr-4 whitespace-nowrap text-ink-600 dark:text-mist-300">
                {{ item.rotulo }}
              </td>
              <td class="py-2 text-erro-600 dark:text-erro-400">{{ item.descricao }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <p
      class="mt-4 rounded border border-ok-600 bg-ok-600/10 px-4 py-3 text-sm text-ink-900 dark:border-ok-400 dark:bg-ok-400/15 dark:text-mist-100"
      role="status"
      v-else
    >
      Nenhum problema encontrado: as {{ inteiro(planilha.quantidadeLinhas) }} linhas passaram em
      todas as validações.
    </p>
  </section>

  <!-- Sem planilha nesta sessão o relatório detalhado (linha, campo,
       descrição) não existe: ele nasce da leitura no navegador. O que foi
       gravado no servidor continua logo abaixo. -->
  <section
    class="mt-8 flex flex-col items-start gap-3 rounded border border-ink-200 bg-white p-5 sm:flex-row sm:items-center sm:justify-between dark:border-night-700 dark:bg-night-900"
    v-else
  >
    <div>
      <h2 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Nenhuma planilha carregada nesta sessão</h2>
      <p class="mt-1 text-sm text-ink-600 dark:text-mist-300">
        O relatório de validação, com a linha, o campo e a descrição de cada erro, aparece depois de
        carregar uma planilha no Upload.
      </p>
    </div>
    <RouterLink
      class="shrink-0 rounded bg-brand-800 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
      :to="{ name: 'upload' }"
    >
      Ir para o Upload
    </RouterLink>
  </section>

  <!-- =====================================================================
       O que o servidor gravou. Vem do banco, então sobrevive a recarregar a
       página e mostra também envios feitos em outro computador.
       ===================================================================== -->
  <section class="mt-10">
    <h2 class="text-lg font-semibold text-ink-900 dark:text-mist-100">Último processamento no servidor</h2>
    <p class="mt-1 text-sm text-ink-600 dark:text-mist-300">
      O servidor trata a planilha de novo em Python antes de gravar, então estes números podem diferir da
      prévia do navegador.
    </p>

    <p class="mt-4 text-sm text-ink-600 dark:text-mist-300" v-if="estadoServidor === 'carregando'" role="status">
      Carregando…
    </p>

    <p
      class="mt-4 rounded border border-ink-200 bg-white px-4 py-3 text-sm text-ink-600 dark:border-night-700 dark:bg-night-900 dark:text-mist-300"
      v-else-if="estadoServidor === 'vazio'"
    >
      Nenhuma planilha foi processada ainda.
    </p>

    <div
      class="mt-4 rounded border border-erro-600 bg-erro-600/5 p-4 text-sm dark:border-erro-400 dark:bg-erro-400/10"
      v-else-if="estadoServidor === 'erro'"
      role="alert"
    >
      <p class="text-ink-900 dark:text-mist-100">Não foi possível consultar o servidor: {{ erroServidor }}</p>
      <button
        class="mt-3 rounded border border-ink-200 px-4 py-2 font-semibold text-brand-800 transition-colors hover:border-brand-500 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:border-night-700 dark:text-brand-300"
        type="button"
        @click="carregarServidor"
      >
        Tentar de novo
      </button>
    </div>

    <template v-else>
      <dl
        class="mt-4 grid grid-cols-2 gap-px overflow-hidden rounded border border-ink-200 bg-ink-200 sm:grid-cols-4 dark:border-night-700 dark:bg-night-700"
      >
        <div class="bg-white px-4 py-3 dark:bg-night-900">
          <dt class="text-xs text-ink-600 dark:text-mist-300">Arquivo</dt>
          <dd class="mt-1 truncate text-sm font-semibold text-ink-900 dark:text-mist-100" :title="ultima.arquivo">
            {{ ultima.arquivo }}
          </dd>
        </div>
        <div class="bg-white px-4 py-3 dark:bg-night-900">
          <dt class="text-xs text-ink-600 dark:text-mist-300">Processado em</dt>
          <dd class="mt-1 text-sm font-semibold tabular-nums text-ink-900 dark:text-mist-100">
            {{ dataHora(ultima.geradoEm) }}
          </dd>
        </div>
        <div class="bg-white px-4 py-3 dark:bg-night-900">
          <dt class="text-xs text-ink-600 dark:text-mist-300">Linhas lidas</dt>
          <dd class="mt-1 text-sm font-semibold tabular-nums text-ink-900 dark:text-mist-100">
            {{ inteiro(ultima.linhasLidas) }}
          </dd>
        </div>
        <div class="bg-white px-4 py-3 dark:bg-night-900">
          <dt class="text-xs text-ink-600 dark:text-mist-300">Aproveitamento</dt>
          <dd class="mt-1 text-sm font-semibold tabular-nums text-ink-900 dark:text-mist-100">
            {{ porcento(qualidade.percentual_aproveitado) }}
          </dd>
        </div>
        <div class="bg-white px-4 py-3 dark:bg-night-900">
          <dt class="text-xs text-ink-600 dark:text-mist-300">Linhas gravadas</dt>
          <dd class="mt-1 text-sm font-semibold tabular-nums text-ok-600 dark:text-ok-400">
            {{ inteiro(ultima.linhasValidas) }}
          </dd>
        </div>
        <div class="bg-white px-4 py-3 dark:bg-night-900">
          <dt class="text-xs text-ink-600 dark:text-mist-300">Linhas recusadas</dt>
          <dd
            class="mt-1 text-sm font-semibold tabular-nums"
            :class="ultima.linhasRejeitadas ? 'text-erro-600 dark:text-erro-400' : 'text-ink-900 dark:text-mist-100'"
          >
            {{ inteiro(ultima.linhasRejeitadas) }}
          </dd>
        </div>
        <div class="bg-white px-4 py-3 dark:bg-night-900">
          <dt class="text-xs text-ink-600 dark:text-mist-300">Clientes novos</dt>
          <dd class="mt-1 text-sm font-semibold tabular-nums text-ink-900 dark:text-mist-100">
            {{ inteiro(ultima.clientesCriados) }}
          </dd>
        </div>
        <div class="bg-white px-4 py-3 dark:bg-night-900">
          <dt class="text-xs text-ink-600 dark:text-mist-300">Clientes atualizados</dt>
          <dd class="mt-1 text-sm font-semibold tabular-nums text-ink-900 dark:text-mist-100">
            {{ inteiro(ultima.clientesAtualizados) }}
          </dd>
        </div>
      </dl>

      <div class="mt-4 grid gap-4 lg:grid-cols-2">
        <div class="rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
          <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Validação no servidor</h3>
          <table class="mt-3 w-full text-left text-sm">
            <thead>
              <tr class="border-b border-ink-200 dark:border-night-700">
                <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Verificação</th>
                <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Quantidade</th>
              </tr>
            </thead>
            <tbody>
              <tr class="border-b border-ink-200 last:border-0 dark:border-night-700" v-for="item in quadroServidor" :key="item.tipo">
                <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
                <td class="py-2 text-right font-semibold tabular-nums" :class="quantidadeCor(item)">
                  {{ inteiro(item.quantidade) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
          <h3 class="text-sm font-semibold text-ink-900 dark:text-mist-100">Tratamentos aplicados no servidor</h3>
          <table class="mt-3 w-full text-left text-sm">
            <thead>
              <tr class="border-b border-ink-200 dark:border-night-700">
                <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Tratamento</th>
                <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Quantidade</th>
              </tr>
            </thead>
            <tbody>
              <tr class="border-b border-ink-200 last:border-0 dark:border-night-700" v-for="item in tratamentosServidor" :key="item.tipo">
                <td class="py-2 pr-4 text-ink-900 dark:text-mist-100">{{ item.rotulo }}</td>
                <td
                  class="py-2 text-right font-semibold tabular-nums"
                  :class="item.quantidade ? 'text-alerta-600 dark:text-alerta-400' : 'text-ink-400 dark:text-mist-500'"
                >
                  {{ inteiro(item.quantidade) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <section class="mt-10 min-w-0">
        <h2 class="text-lg font-semibold text-ink-900 dark:text-mist-100">Histórico de processamentos</h2>
        <p class="mt-1 text-sm text-ink-600 dark:text-mist-300">Planilhas enviadas, da mais recente para a mais antiga.</p>

        <div class="mt-4 overflow-x-auto rounded border border-ink-200 bg-white p-5 dark:border-night-700 dark:bg-night-900">
          <table class="w-full min-w-136 text-left text-sm">
            <thead>
              <tr class="border-b border-ink-200 dark:border-night-700">
                <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Arquivo</th>
                <th class="py-2 pr-4 font-medium text-ink-600 dark:text-mist-300" scope="col">Processado em</th>
                <th class="py-2 pr-4 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Lidas</th>
                <th class="py-2 pr-4 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Gravadas</th>
                <th class="py-2 text-right font-medium text-ink-600 dark:text-mist-300" scope="col">Recusadas</th>
              </tr>
            </thead>
            <tbody>
              <tr class="border-b border-ink-200 last:border-0 dark:border-night-700" v-for="envio in historico" :key="envio.id">
                <td class="py-2.5 pr-4 text-ink-900 dark:text-mist-100">{{ envio.arquivo }}</td>
                <td class="py-2.5 pr-4 text-ink-600 dark:text-mist-300">{{ dataHora(envio.geradoEm) }}</td>
                <td class="py-2.5 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ inteiro(envio.linhasLidas) }}</td>
                <td class="py-2.5 pr-4 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ inteiro(envio.linhasValidas) }}</td>
                <td class="py-2.5 text-right tabular-nums text-ink-900 dark:text-mist-100">{{ inteiro(envio.linhasRejeitadas) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
  </section>
</template>
