package com.synthetix.cti.dto;

import com.synthetix.cti.model.Insight;
import java.time.Instant;

/**
 * Insight pronto para o dashboard. Categoria, titulo e texto vem do metodo de cada subclasse.
 */
public record InsightDTO(Long id, String categoria, String titulo, String texto, Instant geradoEm) {

	public static InsightDTO de(Insight insight) {
		return new InsightDTO(insight.getId(), insight.getCategoria(), insight.getTitulo(), insight.gerarResumo(),
				insight.getGeradoEm());
	}

}
