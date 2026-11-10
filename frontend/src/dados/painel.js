// Formatação de datas dos gráficos do Relatório (evolução entre envios).

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
