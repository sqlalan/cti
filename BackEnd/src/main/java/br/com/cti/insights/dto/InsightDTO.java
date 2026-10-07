package br.com.cti.insights.dto;

import br.com.cti.insights.model.Insight;
import java.time.Instant;

/**
 * Insight pronto para o dashboard. Origem e titulo vem do metodo de cada subclasse: ANALISE e o texto
 * do Python, RESUMO e o numero agregado pelo Java.
 */
public record InsightDTO(Long id, String origem, String tipo, String titulo, String descricao, Instant geradoEm) {

	public static InsightDTO de(Insight insight) {
		return new InsightDTO(insight.getId(), insight.getOrigem(), insight.getTipo(), insight.getTitulo(),
				insight.getDescricao(), insight.getGeradoEm());
	}

}
