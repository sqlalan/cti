package com.synthetix.cti.dto;

import com.synthetix.cti.model.Insight;
import java.time.Instant;

/**
 * Insight pronto para o dashboard. Origem, categoria, titulo e texto vem dos metodos de cada
 * subclasse: ANALISE e o texto do Python, RESUMO e o numero agregado pelo Java.
 */
public record InsightDTO(Long id, String origem, String categoria, String titulo, String texto, Instant geradoEm) {

	public static InsightDTO de(Insight insight) {
		return new InsightDTO(insight.getId(), insight.getOrigem(), insight.getCategoria(), insight.getTitulo(),
				insight.gerarResumo(), insight.getGeradoEm());
	}

}
