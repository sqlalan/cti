<script setup>
import Header from '../components/Header.vue'
import Footer from '../components/Footer.vue'
import Card3D from '../components/Card3D.vue'
import MatrixRain from '../components/MatrixRain.vue'
</script>

<template>
  <Header />

  <!-- Hero sobre o matrix. -->
  <div class="relative overflow-hidden bg-brand-900">
    <MatrixRain class="pointer-events-none absolute inset-0" />
    <div class="pointer-events-none absolute inset-0 bg-brand-900/20"></div>

    <section class="relative flex min-h-[56svh] items-center px-8 py-16 text-center">
      <div class="pointer-events-none absolute inset-0 vinheta-hero"></div>

      <div class="pointer-events-none absolute inset-0" aria-hidden="true">
        <span class="absolute left-[8%] top-[16%] h-40 w-40 animate-flutuar motion-reduce:animate-none rounded-full bg-brand-500/25 blur-3xl"></span>
        <span class="absolute right-[9%] top-[26%] h-56 w-56 animate-flutuar-lento motion-reduce:animate-none rounded-full bg-brand-100/15 blur-3xl"></span>
        <span class="absolute bottom-[14%] left-[26%] h-32 w-32 animate-flutuar-lento motion-reduce:animate-none rounded-full bg-white/10 blur-3xl"></span>
      </div>

      <div class="relative mx-auto w-full">
        <h1 class="mx-auto max-w-3xl text-4xl font-bold leading-tight text-white sm:text-5xl">
          Dados que conectam.<br />Inteligência que transforma.
        </h1>
        <p class="mx-auto mt-6 max-w-2xl text-lg leading-relaxed text-ink-200">
          Conectamos dados, análises e inteligência em uma única plataforma
          para a sua empresa decidir com mais segurança.
        </p>
      </div>
    </section>
  </div>

  <section class="mx-auto max-w-6xl scroll-mt-24 px-8 py-20" id="plataforma">
    <h2 class="mx-auto max-w-2xl text-center text-3xl font-bold leading-snug text-ink-900 dark:text-mist-100">
      Uma plataforma para enxergar o seu negócio por completo.
    </h2>

    <ol class="mx-auto mt-8 flex max-w-5xl flex-wrap items-center justify-center gap-x-3 gap-y-3 text-sm font-medium text-ink-600 dark:text-mist-300">
      <li>Dados dispersos</li>
      <li class="text-ink-400 dark:text-mist-500" aria-hidden="true">→</li>
      <li class="rounded bg-brand-800 px-3 py-1 font-semibold text-white shadow-chip">Synthetix</li>
      <li class="text-ink-400 dark:text-mist-500" aria-hidden="true">→</li>
      <li>Informação organizada</li>
      <li class="text-ink-400 dark:text-mist-500" aria-hidden="true">→</li>
      <li>Análise</li>
      <li class="text-ink-400 dark:text-mist-500" aria-hidden="true">→</li>
      <li>Insight</li>
      <li class="text-ink-400 dark:text-mist-500" aria-hidden="true">→</li>
      <li class="font-semibold text-glow-600 dark:text-glow-300">Decisão</li>
    </ol>
    <div class="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
      <Card3D :atraso="0">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-brand-500/10 text-brand-800 dark:text-brand-300">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 21h18" /><path d="M7 21V11" /><path d="M12 21V5" /><path d="M17 21v-7" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-ink-900 dark:text-mist-100">Dashboards inteligentes</h3>
        <p class="mt-2 text-sm leading-relaxed text-ink-600 dark:text-mist-300">
          Visualize os principais indicadores em um único ambiente.
        </p>
      </Card3D>

      <Card3D :atraso="1500">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-brand-500/10 text-brand-800 dark:text-brand-300">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m3 17 6-6 4 4 8-8" /><path d="M15 7h6v6" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-ink-900 dark:text-mist-100">Análises avançadas</h3>
        <p class="mt-2 text-sm leading-relaxed text-ink-600 dark:text-mist-300">
          Explore tendências, comparações e resultados.
        </p>
      </Card3D>

      <Card3D :atraso="3000">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-brand-500/10 text-brand-800 dark:text-brand-300">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 3v12" /><path d="m7 10 5 5 5-5" /><path d="M3 17v2a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-2" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-ink-900 dark:text-mist-100">Integração de dados</h3>
        <p class="mt-2 text-sm leading-relaxed text-ink-600 dark:text-mist-300">
          Centralize e organize as informações da operação.
        </p>
      </Card3D>

      <Card3D :atraso="4500">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-glow-400/10 text-glow-600 dark:text-glow-300">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18h6" /><path d="M10 21h4" /><path d="M12 3a6 6 0 0 0-4 10.5c.7.8 1 1.5 1 2.5h6c0-1 .3-1.7 1-2.5A6 6 0 0 0 12 3Z" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-ink-900 dark:text-mist-100">Insights</h3>
        <p class="mt-2 text-sm leading-relaxed text-ink-600 dark:text-mist-300">
          Identifique padrões e oportunidades.
        </p>
      </Card3D>

      <Card3D :atraso="6000">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-brand-500/10 text-brand-800 dark:text-brand-300">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8Z" /><path d="M14 3v5h5" /><path d="M9 13h6" /><path d="M9 17h4" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-ink-900 dark:text-mist-100">Relatórios</h3>
        <p class="mt-2 text-sm leading-relaxed text-ink-600 dark:text-mist-300">
          Transforme análises em relatórios claros.
        </p>
      </Card3D>

      <Card3D :atraso="7500">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-brand-500/10 text-brand-800 dark:text-brand-300">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z" /><circle cx="12" cy="11" r="1.5" /><path d="M12 12.5V15" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-ink-900 dark:text-mist-100">Segurança</h3>
        <p class="mt-2 text-sm leading-relaxed text-ink-600 dark:text-mist-300">
          Controle de acesso e proteção em todas as etapas.
        </p>
      </Card3D>
    </div>
  </section>

  <Footer />
</template>
