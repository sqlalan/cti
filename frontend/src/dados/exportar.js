
// Exportação de tabelas para arquivo, usada na Carteira (Dashboard) e no
// Relatório. Cada tabela é { colunas: [[título, (linha) => valor], ...], linhas }.

export const hoje = () => new Date().toISOString().slice(0, 10)

// Célula que começa com = + - @ é executada como fórmula ao abrir o CSV no
// Excel. Como o conteúdo veio de uma planilha enviada por terceiros, prefixar
// com apóstrofo neutraliza a injeção sem alterar o que a pessoa lê.
function protegerCelula(texto) {
  const valor = String(texto ?? '')
  const perigosa = /^[=+\-@\t\r]/.test(valor)
  const escapada = (perigosa ? `'${valor}` : valor).replace(/"/g, '""')
  return `"${escapada}"`
}

function baixar(conteudo, nome) {
  const url = URL.createObjectURL(conteudo)
  const ancora = document.createElement('a')
  ancora.href = url
  ancora.download = nome

  // A âncora precisa estar no documento para o clique valer em alguns
  // navegadores, e a URL do blob só pode ser revogada depois que o download
  // começou — revogar na mesma volta do laço cancela o arquivo pela metade.
  document.body.appendChild(ancora)
  ancora.click()
  ancora.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export function baixarCsv(nome, { colunas, linhas }) {
  const texto = [
    colunas.map(([titulo]) => protegerCelula(titulo)).join(';'),
    ...linhas.map((linha) => colunas.map(([, obter]) => protegerCelula(obter(linha))).join(';')),
  ].join('\r\n')

  // BOM na frente para o Excel abrir em UTF-8; sem ele os acentos quebram.
  baixar(new Blob(['﻿' + texto], { type: 'text/csv;charset=utf-8;' }), nome)
}

/**
 * Um arquivo .xlsx com uma aba por tabela: abas = [{ nome, colunas, linhas }].
 * Número sai como número (o Excel soma e ordena); texto sai como texto, nunca
 * como fórmula, então aqui não há injeção a neutralizar.
 */
export async function baixarXlsx(nome, abas) {
  const XLSX = await import('xlsx') // só carrega quando alguém exporta
  const pasta = XLSX.utils.book_new()
  for (const aba of abas) {
    const matriz = [aba.colunas.map(([titulo]) => titulo), ...aba.linhas.map((linha) => aba.colunas.map(([, obter]) => obter(linha) ?? ''))]
    const planilha = XLSX.utils.aoa_to_sheet(matriz)
    // Largura de cada coluna pelo maior texto dela, para abrir legível.
    planilha['!cols'] = matriz[0].map((_, c) => ({
      wch: Math.min(60, Math.max(...matriz.map((l) => String(l[c] ?? '').length)) + 2),
    }))
    // Nome de aba no Excel: até 31 caracteres e sem : \ / ? * [ ]
    XLSX.utils.book_append_sheet(pasta, planilha, aba.nome.replace(/[:\\/?*[\]]/g, '-').slice(0, 31))
  }
  XLSX.writeFile(pasta, nome)
}
