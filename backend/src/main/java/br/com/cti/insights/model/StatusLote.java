package br.com.cti.insights.model;

/**
 * Situação de um envio de planilha. O Python cria o lote em PROCESSANDO e termina em PROCESSADO
 * ou ERRO (aula 12).
 */
public enum StatusLote {

	PROCESSANDO, PROCESSADO, ERRO

}
