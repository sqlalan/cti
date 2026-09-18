// Arredonda o topo do eixo para um número limpo (1, 2, 2,5 ou 5 vezes uma
// potência de 10) e devolve os cortes. Sem isto o eixo sai 0 / 1.847 / 3.694,
// que ninguém lê — e os cortes carregam justamente os valores que não foram
// rotulados diretamente no gráfico.
export function ticksBonitos(maximo, quantidade = 4, { inteiro = false } = {}) {
  if (!(maximo > 0)) return { topo: 1, cortes: [0, 1] }

  const passoBruto = maximo / quantidade
  const potencia = 10 ** Math.floor(Math.log10(passoBruto))
  const normalizado = passoBruto / potencia
  const escolhido =
    normalizado <= 1 ? 1 : normalizado <= 2 ? 2 : normalizado <= 2.5 ? 2.5 : normalizado <= 5 ? 5 : 10

  // Contagem não tem meio: numa série que vai só até 2, o passo natural seria
  // 0,5 e o eixo sairia 0 / 1 / 1 / 2 / 2, com rótulo repetido, porque a
  // formatação arredonda. Forçar passo inteiro resolve na origem.
  const passo = inteiro ? Math.max(1, Math.round(escolhido * potencia)) : escolhido * potencia
  const topo = Math.ceil(maximo / passo) * passo

  const cortes = []
  for (let valor = 0; valor <= topo + passo / 2; valor += passo) {
    cortes.push(Math.round(valor * 1000) / 1000)
  }
  return { topo, cortes }
}

export function formatarNumero(valor, casas = 0) {
  return valor.toLocaleString('pt-BR', { maximumFractionDigits: casas })
}
