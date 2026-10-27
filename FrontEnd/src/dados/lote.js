// Situação do lote em linguagem de quem usa o sistema, sem nome de tecnologia.
// A mensagem técnica do servidor fica no banco (upload_lote.mensagem) e na
// telemetria, para o suporte; a tela não mostra.
export function explicarLote(lote) {
  switch (lote?.status) {
    case 'AGUARDANDO_ETL':
      return 'A planilha está na fila para ser processada.'
    case 'PROCESSANDO':
      return 'A planilha está sendo processada.'
    case 'PROCESSADO':
      return 'Planilha processada: os indicadores estão no Dashboard.'
    case 'ERRO':
    case 'REJEITADO':
      return 'Não foi possível processar esta planilha. Tente enviar de novo em alguns minutos; se continuar, avise o suporte.'
    default:
      return ''
  }
}
