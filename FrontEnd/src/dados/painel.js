// Formatação e cores compartilhadas pelas telas de indicadores (Dashboard e
// Relatórios). Os números do Dashboard vêm da API (/api/indicadores); os
// dados de exemplo que moravam aqui saíram quando o painel foi ligado ao back-end.

// Nível é escala ORDINAL: o tom escurece de C para A. Mesma rampa nas duas telas.
export const CORES_NIVEL = {
  A: 'bg-nivel-a-600 dark:bg-nivel-a-400',
  B: 'bg-nivel-b-600 dark:bg-nivel-b-400',
  C: 'bg-nivel-c-600 dark:bg-nivel-c-400',
}

// Compacta valores grandes para caber no cartão: 12.482 vira "12,5 mil".
export function compactar(valor) {
  if (valor >= 1000000) return `${(valor / 1000000).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} mi`
  if (valor >= 10000) return `${(valor / 1000).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} mil`
  return valor.toLocaleString('pt-BR')
}

export function formatarData(data, formato = 'curto') {
  if (formato === 'curto') {
    return data.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })
  }
  // Série mensal precisa de "jan/25": mostrar 01/01 num eixo de meses se lê
  // como primeiro de janeiro e sugere granularidade diária que não existe.
  if (formato === 'mes') {
    // "jan/25", e não "jan de 25": a forma longa mede quase o dobro e os dois
    // últimos rótulos do eixo acabavam se sobrepondo.
    const mes = data.toLocaleDateString('pt-BR', { month: 'short' }).replace('.', '')
    return `${mes}/${String(data.getFullYear()).slice(2)}`
  }
  return data.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' })
}

// "2025-02" (formato do Python) vira o dia 1º do mês, no fuso local. new
// Date("2025-02") leria UTC e, no Brasil, cairia em 31 de janeiro.
export function mesParaData(anoMes) {
  const [ano, mes] = anoMes.split('-').map(Number)
  return new Date(ano, mes - 1, 1)
}
