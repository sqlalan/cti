package br.com.cti.insights.analise;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Uma linha valida de clientes_tratados.json, ja limpa e padronizada pelo Python.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClienteTratado(String codigo, String nome, String consultor, String segmento, String nivel,
		BigDecimal faturamento, List<String> servicos, LocalDate contratacao, String cidade, String uf) {

}
