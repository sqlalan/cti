package br.com.cti.insights.model;

/**
 * Publicado quando o lote e as linhas foram salvos; o PythonEtlService escuta depois do commit.
 */
public record LoteValidadoEvent(Long loteId) {

}
