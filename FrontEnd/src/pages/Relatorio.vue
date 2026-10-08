<script setup>
import HistoricoProcessamentos from '../components/HistoricoProcessamentos.vue'
import { useUploadStore } from '../stores/uploadStore'

// Relatório do PROCESSAMENTO dos dados. A visão comercial da carteira mora no
// Dashboard; aqui fica o que aconteceu com as planilhas:
//  - a validação da planilha carregada nesta sessão, lida do Pinia (é o que o
//    store existe para resolver: o dado atravessa a navegação do Upload até
//    aqui sem passar por prop nem por URL);
//  - o que o servidor gravou de cada envio (HistoricoProcessamentos), que
//    continua disponível depois de recarregar a página.
const planilha = useUploadStore()

const inteiro = (valor) => valor.toLocaleString('pt-BR')

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

      <div class="mt-3 max-h-96 overflow-auto print:max-h-none print:overflow-visible">
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
    class="mt-8 flex flex-col items-start gap-3 print:hidden rounded border border-ink-200 bg-white p-5 sm:flex-row sm:items-center sm:justify-between dark:border-night-700 dark:bg-night-900"
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

  <HistoricoProcessamentos />
</template>
