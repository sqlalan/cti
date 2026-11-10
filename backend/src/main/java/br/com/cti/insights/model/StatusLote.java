package br.com.cti.insights.model;

/**
 * Situacao de um envio de planilha. O Spring cria o lote em AGUARDANDO_ETL; o Python passa para
 * PROCESSANDO e depois PROCESSADO ou ERRO.
 */
public enum StatusLote {

	AGUARDANDO_ETL, PROCESSANDO, PROCESSADO, ERRO, REJEITADO

}
