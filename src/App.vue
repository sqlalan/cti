<template>
  <header
    class="flex items-center justify-between border-b border-border bg-background px-8 py-4"
  >
    <h1 class="text-xl font-bold text-primary">Synthetix</h1>
    <nav class="flex gap-8 text-sm font-medium text-text-secondary">
      <a class="text-primary" href="#" aria-current="page">Início</a>
      <a href="#">Sobre</a>
      <a href="#">Contato</a>
    </nav>
  </header>

  <section class="bg-background-dark px-8 py-24 text-center">
    <h2 class="text-4xl font-bold text-white">
      Transforme sua planilha em dados
    </h2>
    <p class="mx-auto mt-4 max-w-xl text-base text-border">
      Centralize, organize e visualize as informações da sua operação em um
      só lugar.
    </p>
    <div class="mt-10 flex justify-center gap-4">
      <button class="btn-primary">Começar agora</button>
      <button class="btn-secondary">Falar com um consultor</button>
    </div>
  </section>

  <main class="mx-auto max-w-5xl px-8 py-20">
    <div class="grid gap-8 sm:grid-cols-3">
      <article class="rounded border border-border p-6">
        <span class="block h-1 w-10 rounded bg-accent-network" aria-hidden="true"></span>
        <p class="mt-4 text-3xl font-bold text-primary">99,9%</p>
        <h3 class="mt-2 font-semibold text-text">Conectividade</h3>
        <p class="mt-1 text-sm text-text-secondary">
          Disponibilidade monitorada de ponta a ponta.
        </p>
      </article>
      <article class="rounded border border-border p-6">
        <span class="block h-1 w-10 rounded bg-accent-cloud" aria-hidden="true"></span>
        <p class="mt-4 text-3xl font-bold text-primary">24/7</p>
        <h3 class="mt-2 font-semibold text-text">Cloud</h3>
        <p class="mt-1 text-sm text-text-secondary">
          Infraestrutura elástica com backup contínuo.
        </p>
      </article>
      <article class="rounded border border-border p-6">
        <span class="block h-1 w-10 rounded bg-accent-security" aria-hidden="true"></span>
        <p class="mt-4 text-3xl font-bold text-primary">ISO</p>
        <h3 class="mt-2 font-semibold text-text">Segurança</h3>
        <p class="mt-1 text-sm text-text-secondary">
          Controles de acesso e criptografia em repouso.
        </p>
      </article>
    </div>

    <div class="mt-16 rounded bg-surface p-8 text-center">
      <h3 class="text-lg font-semibold text-text">Pronto para começar?</h3>
      <div class="mt-6 flex justify-center gap-4">
        <button class="btn-primary">Criar conta</button>
        <button class="btn-tertiary">Ver documentação</button>
      </div>
    </div>
  </main>

  <footer class="bg-background-dark text-white text-center p-4">
     © 2026 - Synthetix - Data Analyser. Todos os direitos reservados.
  </footer>
</template>
