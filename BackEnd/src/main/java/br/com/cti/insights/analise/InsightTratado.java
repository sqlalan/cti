package br.com.cti.insights.analise;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Um item de insights.json.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InsightTratado(String tipo, String titulo, String texto) {

}
