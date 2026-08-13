<script setup>
import MatrixRain from '../components/MatrixRain.vue'
</script>

<template>
  <header class="flex flex-wrap items-center justify-between gap-4 border-b border-[#D9E0E6] bg-white px-8 py-4">
    <RouterLink class="shrink-0" to="/">
      <img class="h-10 w-auto" src="../assets/logo.png" alt="Synthetix" width="421" height="120" />
    </RouterLink>
    <div class="flex flex-wrap items-center gap-6">
      <nav class="flex flex-wrap gap-8 text-sm font-medium text-[#5F5F68]">
        <a class="hover:text-[#3F77C4]" href="#plataforma">Plataforma</a>
        <a class="hover:text-[#3F77C4]" href="#sobre">Sobre</a>
        <a class="hover:text-[#3F77C4]" href="#proposito">Propósito</a>
        <a class="hover:text-[#3F77C4]" href="#contato">Contato</a>
      </nav>
      <RouterLink class="inline-flex items-center justify-center rounded bg-[#194675] px-5 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#11365B] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#3F77C4]" to="/login">Entrar</RouterLink>
    </div>
  </header>

  <section class="relative overflow-hidden bg-[#11365B] px-8 py-28 text-center">
    <MatrixRain class="pointer-events-none absolute inset-0" />
    <div class="pointer-events-none absolute inset-0 bg-[#11365B]/35"></div>
    <div class="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_68%_60%_at_50%_50%,#11365B_34%,rgba(17,54,91,0.65)_66%,rgba(17,54,91,0)_100%)]"></div>

    <div class="relative">
      <h1 class="mx-auto max-w-3xl text-4xl font-bold leading-tight text-white sm:text-5xl">
        Dados que conectam.<br />Inteligência que transforma.
      </h1>
      <p class="mx-auto mt-6 max-w-2xl text-lg leading-relaxed text-[#D9E0E6]">
        A Synthetix conecta dados, análises e inteligência em uma única
        plataforma para ajudar empresas a compreenderem seus resultados,
        identificarem oportunidades e tomarem decisões com mais segurança.
      </p>
      <div class="mt-10 flex flex-wrap justify-center gap-4">
        <a class="inline-flex items-center justify-center rounded bg-white px-6 py-3 text-sm font-semibold text-[#194675] transition-colors hover:bg-[#D9E0E6] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white" href="#plataforma">Conhecer a plataforma</a>
        <a class="inline-flex items-center justify-center rounded border border-white/60 px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white" href="#contato">Falar com um especialista</a>
      </div>
    </div>
  </section>

  <section class="bg-[#F5F7FA] px-8 py-12">
    <ol class="mx-auto flex max-w-5xl flex-wrap items-center justify-center gap-x-3 gap-y-3 text-sm font-medium text-[#5F5F68]">
      <li>Dados dispersos</li>
      <li class="text-[#8C99A6]" aria-hidden="true">→</li>
      <li class="rounded bg-[#194675] px-3 py-1 font-semibold text-white">Synthetix</li>
      <li class="text-[#8C99A6]" aria-hidden="true">→</li>
      <li>Informação organizada</li>
      <li class="text-[#8C99A6]" aria-hidden="true">→</li>
      <li>Análise</li>
      <li class="text-[#8C99A6]" aria-hidden="true">→</li>
      <li>Insight</li>
      <li class="text-[#8C99A6]" aria-hidden="true">→</li>
      <li class="font-semibold text-[#194675]">Decisão</li>
    </ol>
  </section>

  <section class="mx-auto max-w-4xl px-8 py-24 text-center" id="sobre">
    <h2 class="text-3xl font-bold leading-snug text-[#262F3E]">
      A Synthetix é uma empresa de tecnologia focada em dados, análises e
      inteligência para negócios.
    </h2>
    <p class="mt-6 text-lg leading-relaxed text-[#5F5F68]">
      Desenvolvemos soluções que organizam informações, conectam diferentes
      fontes de dados e transformam grandes volumes de informação em análises
      claras e relevantes.
    </p>
    <p class="mt-4 text-lg leading-relaxed text-[#5F5F68]">
      Nosso propósito é tornar os dados mais acessíveis, compreensíveis e
      úteis, permitindo que empresas tenham uma visão mais ampla de suas
      operações e tomem decisões baseadas em informações confiáveis.
    </p>
  </section>

  <section class="bg-[#194675] px-8 py-20">
    <blockquote class="mx-auto max-w-3xl text-center text-2xl font-semibold leading-snug text-white sm:text-3xl">
      Porque dados, por si só, não geram valor. O valor está naquilo que
      conseguimos compreender a partir deles.
    </blockquote>
  </section>

  <section class="mx-auto max-w-6xl px-8 py-24" id="plataforma">
    <h2 class="mx-auto max-w-2xl text-center text-3xl font-bold leading-snug text-[#262F3E]">
      Uma plataforma para enxergar o seu negócio por completo.
    </h2>

    <div class="mt-16 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
      <article class="rounded border border-[#D9E0E6] p-6">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-[#3F77C4]/10 text-[#194675]">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 21h18" /><path d="M7 21V11" /><path d="M12 21V5" /><path d="M17 21v-7" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-[#262F3E]">Dashboards inteligentes</h3>
        <p class="mt-2 text-sm leading-relaxed text-[#5F5F68]">
          Visualize os principais indicadores em um único ambiente.
        </p>
      </article>

      <article class="rounded border border-[#D9E0E6] p-6">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-[#7B3FD1]/10 text-[#194675]">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m3 17 6-6 4 4 8-8" /><path d="M15 7h6v6" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-[#262F3E]">Análises avançadas</h3>
        <p class="mt-2 text-sm leading-relaxed text-[#5F5F68]">
          Explore tendências, comparações, comportamentos e resultados.
        </p>
      </article>

      <article class="rounded border border-[#D9E0E6] p-6">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-[#7DB91D]/10 text-[#194675]">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 3v12" /><path d="m7 10 5 5 5-5" /><path d="M3 17v2a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-2" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-[#262F3E]">Integração e importação de dados</h3>
        <p class="mt-2 text-sm leading-relaxed text-[#5F5F68]">
          Centralize, organize e acompanhe informações importantes da operação.
        </p>
      </article>

      <article class="rounded border border-[#D9E0E6] p-6">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-[#D7AF49]/15 text-[#194675]">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18h6" /><path d="M10 21h4" /><path d="M12 3a6 6 0 0 0-4 10.5c.7.8 1 1.5 1 2.5h6c0-1 .3-1.7 1-2.5A6 6 0 0 0 12 3Z" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-[#262F3E]">Insights</h3>
        <p class="mt-2 text-sm leading-relaxed text-[#5F5F68]">
          Identifique padrões, oportunidades e pontos que merecem atenção.
        </p>
      </article>

      <article class="rounded border border-[#D9E0E6] p-6">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-[#3F77C4]/10 text-[#194675]">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8Z" /><path d="M14 3v5h5" /><path d="M9 13h6" /><path d="M9 17h4" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-[#262F3E]">Relatórios</h3>
        <p class="mt-2 text-sm leading-relaxed text-[#5F5F68]">
          Transforme análises em relatórios claros para acompanhamento e
          tomada de decisão.
        </p>
      </article>

      <article class="rounded border border-[#D9E0E6] p-6">
        <span class="flex h-11 w-11 items-center justify-center rounded bg-[#F18A21]/10 text-[#194675]">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z" /><circle cx="12" cy="11" r="1.5" /><path d="M12 12.5V15" /></svg>
        </span>
        <h3 class="mt-5 text-lg font-semibold text-[#262F3E]">Segurança</h3>
        <p class="mt-2 text-sm leading-relaxed text-[#5F5F68]">
          Controle de acesso e proteção das informações em todas as etapas.
        </p>
      </article>
    </div>
  </section>

  <section class="bg-[#F5F7FA] px-8 py-24" id="proposito">
    <div class="mx-auto grid max-w-5xl gap-8 sm:grid-cols-2">
      <article class="rounded border border-[#D9E0E6] bg-white p-8">
        <span class="block h-1 w-10 rounded bg-[#7DB91D]" aria-hidden="true"></span>
        <h3 class="mt-5 text-xl font-bold text-[#194675]">Nossa missão</h3>
        <p class="mt-3 leading-relaxed text-[#5F5F68]">
          Transformar dados em inteligência acessível, confiável e
          estratégica, ajudando empresas a tomar decisões melhores e construir
          resultados sustentáveis.
        </p>
      </article>
      <article class="rounded border border-[#D9E0E6] bg-white p-8">
        <span class="block h-1 w-10 rounded bg-[#7B3FD1]" aria-hidden="true"></span>
        <h3 class="mt-5 text-xl font-bold text-[#194675]">Nossa visão</h3>
        <p class="mt-3 leading-relaxed text-[#5F5F68]">
          Ser referência em soluções de inteligência de dados, tornando a
          análise de informações mais simples, segura e estratégica para
          empresas de todos os segmentos.
        </p>
      </article>
    </div>
  </section>

  <section class="mx-auto max-w-3xl px-8 py-24 text-center" id="contato">
    <h2 class="text-3xl font-bold text-[#262F3E]">
      Pronto para enxergar melhor o seu negócio?
    </h2>
    <p class="mt-4 leading-relaxed text-[#5F5F68]">
      Converse com nosso time e veja a plataforma aplicada à realidade da sua
      operação.
    </p>
    <div class="mt-8 flex flex-wrap justify-center gap-4">
      <button class="inline-flex items-center justify-center rounded bg-[#194675] px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-[#11365B] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#3F77C4]">Solicitar demonstração</button>
      <RouterLink class="inline-flex items-center justify-center rounded bg-[#F5F7FA] px-6 py-3 text-sm font-semibold text-[#194675] transition-colors hover:bg-[#E0E3E6] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#3F77C4]" to="/login">Acessar minha conta</RouterLink>
    </div>
  </section>

  <footer class="bg-[#11365B] px-6 py-10 text-center text-sm text-[#D9E0E6]">
    <ul class="flex justify-center gap-3">
      <li>
        <a
          class="flex h-11 w-11 items-center justify-center rounded text-[#D9E0E6] transition-colors hover:bg-white/10 hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
          href="https://instagram.com/synthetix"
          target="_blank"
          rel="noopener noreferrer"
          aria-label="Synthetix no Instagram"
        >
          <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="2" y="2" width="20" height="20" rx="5" /><circle cx="12" cy="12" r="4" /><circle cx="17.5" cy="6.5" r="1.1" fill="currentColor" stroke="none" /></svg>
        </a>
      </li>
      <li>
        <a
          class="flex h-11 w-11 items-center justify-center rounded text-[#D9E0E6] transition-colors hover:bg-white/10 hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
          href="https://facebook.com"
          target="_blank"
          rel="noopener noreferrer"
          aria-label="Synthetix no Facebook"
        >
          <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z" /></svg>
        </a>
      </li>
      <li>
        <a
          class="flex h-11 w-11 items-center justify-center rounded text-[#D9E0E6] transition-colors hover:bg-white/10 hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
          href="https://wa.me/5511999999999"
          target="_blank"
          rel="noopener noreferrer"
          aria-label="Conversar com a Synthetix no WhatsApp"
        >
          <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M21 11.5a8.5 8.5 0 0 1-12.6 7.4L3 21l2.1-5.4A8.5 8.5 0 1 1 21 11.5Z" />
            <g transform="translate(6.3 6) scale(.47)" stroke-width="3.7"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92Z" /></g>
          </svg>
        </a>
      </li>
      <li>
        <a
          class="flex h-11 items-center gap-2 rounded px-3 text-[#D9E0E6] transition-colors hover:bg-white/10 hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
          href="mailto:contato@synthetix.com.br"
        >
          <svg class="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="2" y="4" width="20" height="16" rx="2" /><path d="m22 7-10 6L2 7" /></svg>
          contato@synthetix.com.br
        </a>
      </li>
    </ul>

    <p class="mt-6">© 2026 — Synthetix. Dados que conectam. Inteligência que transforma.</p>
  </footer>
</template>
