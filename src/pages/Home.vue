<script setup>
import Header from '../components/Header.vue'
import Footer from '../components/Footer.vue'
import Card3D from '../components/Card3D.vue'
import MatrixRain from '../components/MatrixRain.vue'
</script>

<template>
  <Header />

  <section class="relative overflow-hidden bg-[#11365B] px-8 py-24 text-center">
    <MatrixRain class="pointer-events-none absolute inset-0" />
    <div class="pointer-events-none absolute inset-0 bg-[#11365B]/35"></div>
    <div class="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_68%_60%_at_50%_50%,#11365B_34%,rgba(17,54,91,0.65)_66%,rgba(17,54,91,0)_100%)]"></div>

    <div class="pointer-events-none absolute inset-0" aria-hidden="true">
      <span class="absolute left-[8%] top-[16%] h-40 w-40 animate-flutuar motion-reduce:animate-none rounded-full bg-[#3F77C4]/25 blur-3xl"></span>
      <span class="absolute right-[9%] top-[26%] h-56 w-56 animate-flutuar-lento motion-reduce:animate-none rounded-full bg-[#CFE2F7]/15 blur-3xl"></span>
      <span class="absolute bottom-[14%] left-[26%] h-32 w-32 animate-flutuar-lento motion-reduce:animate-none rounded-full bg-white/10 blur-3xl"></span>
    </div>

    <div class="relative">
      <h1 class="mx-auto max-w-3xl text-4xl font-bold leading-tight text-white sm:text-5xl">
        Dados que conectam.<br />Inteligência que transforma.
      </h1>
      <p class="mx-auto mt-6 max-w-2xl text-lg leading-relaxed text-[#D9E0E6]">
        Conectamos dados, análises e inteligência em uma única plataforma
        para a sua empresa decidir com mais segurança.
      </p>
    </div>
  </section>

  <section class="bg-[#F5F7FA] px-8 dark:bg-[#101E2E] py-12">
    <ol class="mx-auto flex max-w-5xl flex-wrap items-center justify-center gap-x-3 gap-y-3 text-sm font-medium text-[#5F5F68] dark:text-[#9FB0C2]">
      <li>Dados dispersos</li>
      <li class="text-[#8C99A6] dark:text-[#6C7D90]" aria-hidden="true">→</li>
      <li class="rounded bg-[#194675] px-3 py-1 font-semibold text-white shadow-[0_8px_16px_-8px_rgba(25,70,117,0.8)]">Synthetix</li>
      <li class="text-[#8C99A6] dark:text-[#6C7D90]" aria-hidden="true">→</li>
      <li>Informação organizada</li>
      <li class="text-[#8C99A6] dark:text-[#6C7D90]" aria-hidden="true">→</li>
      <li>Análise</li>
      <li class="text-[#8C99A6] dark:text-[#6C7D90]" aria-hidden="true">→</li>
      <li>Insight</li>
      <li class="text-[#8C99A6] dark:text-[#6C7D90]" aria-hidden="true">→</li>
      <li class="font-semibold text-[#194675] dark:text-[#7FB0E8]">Decisão</li>
    </ol>
  </section>

  <section class="mx-auto max-w-4xl px-8 py-20 text-center" id="sobre">
    <h2 class="text-3xl font-bold leading-snug text-[#262F3E] dark:text-[#E8EDF2]">
      A Synthetix é uma empresa de tecnologia focada em dados, análises e
      inteligência para negócios.
    </h2>
    <p class="mt-6 text-lg leading-relaxed text-[#5F5F68] dark:text-[#9FB0C2]">
      Organizamos informações, conectamos diferentes fontes de dados e
      transformamos grandes volumes em análises claras — para que sua empresa
      tenha uma visão ampla da operação e decida com base em informação
      confiável.
    </p>
  </section>

  <section class="bg-[#194675] px-8 py-16 dark:bg-[#0F2C4A]">
    <blockquote class="mx-auto max-w-3xl text-center text-2xl font-semibold leading-snug text-white sm:text-3xl">
      Porque dados, por si só, não geram valor. O valor está naquilo que
      conseguimos compreender a partir deles.
    </blockquote>
  </section>

  <section class="mx-auto max-w-6xl px-8 py-20" id="plataforma">
    <h2 class="mx-auto max-w-2xl text-center text-3xl font-bold leading-snug text-[#262F3E] dark:text-[#E8EDF2]">
      Uma plataforma para enxergar o seu negócio por completo.
    </h2>
    <div class="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
      <Card3D :atraso="0">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-[#3F77C4]/10 text-[#194675] dark:text-[#7FB0E8]">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 21h18" /><path d="M7 21V11" /><path d="M12 21V5" /><path d="M17 21v-7" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-[#262F3E] dark:text-[#E8EDF2]">Dashboards inteligentes</h3>
        <p class="mt-2 text-sm leading-relaxed text-[#5F5F68] dark:text-[#9FB0C2]">
          Visualize os principais indicadores em um único ambiente.
        </p>
      </Card3D>

      <Card3D :atraso="1500">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-[#7B3FD1]/10 text-[#194675] dark:text-[#7FB0E8]">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m3 17 6-6 4 4 8-8" /><path d="M15 7h6v6" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-[#262F3E] dark:text-[#E8EDF2]">Análises avançadas</h3>
        <p class="mt-2 text-sm leading-relaxed text-[#5F5F68] dark:text-[#9FB0C2]">
          Explore tendências, comparações, comportamentos e resultados.
        </p>
      </Card3D>

      <Card3D :atraso="3000">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-[#7DB91D]/10 text-[#194675] dark:text-[#7FB0E8]">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 3v12" /><path d="m7 10 5 5 5-5" /><path d="M3 17v2a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-2" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-[#262F3E] dark:text-[#E8EDF2]">Integração e importação de dados</h3>
        <p class="mt-2 text-sm leading-relaxed text-[#5F5F68] dark:text-[#9FB0C2]">
          Centralize, organize e acompanhe informações importantes da operação.
        </p>
      </Card3D>

      <Card3D :atraso="4500">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-[#D7AF49]/15 text-[#194675] dark:text-[#7FB0E8]">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18h6" /><path d="M10 21h4" /><path d="M12 3a6 6 0 0 0-4 10.5c.7.8 1 1.5 1 2.5h6c0-1 .3-1.7 1-2.5A6 6 0 0 0 12 3Z" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-[#262F3E] dark:text-[#E8EDF2]">Insights</h3>
        <p class="mt-2 text-sm leading-relaxed text-[#5F5F68] dark:text-[#9FB0C2]">
          Identifique padrões, oportunidades e pontos que merecem atenção.
        </p>
      </Card3D>

      <Card3D :atraso="6000">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-[#3F77C4]/10 text-[#194675] dark:text-[#7FB0E8]">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8Z" /><path d="M14 3v5h5" /><path d="M9 13h6" /><path d="M9 17h4" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-[#262F3E] dark:text-[#E8EDF2]">Relatórios</h3>
        <p class="mt-2 text-sm leading-relaxed text-[#5F5F68] dark:text-[#9FB0C2]">
          Transforme análises em relatórios claros para acompanhamento e
          tomada de decisão.
        </p>
      </Card3D>

      <Card3D :atraso="7500">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-[#F18A21]/10 text-[#194675] dark:text-[#7FB0E8]">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z" /><circle cx="12" cy="11" r="1.5" /><path d="M12 12.5V15" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-[#262F3E] dark:text-[#E8EDF2]">Segurança</h3>
        <p class="mt-2 text-sm leading-relaxed text-[#5F5F68] dark:text-[#9FB0C2]">
          Controle de acesso e proteção das informações em todas as etapas.
        </p>
      </Card3D>
    </div>
  </section>

  <Footer />
</template>
