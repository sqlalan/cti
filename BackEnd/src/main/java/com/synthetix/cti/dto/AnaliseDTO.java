package com.synthetix.cti.dto;

import com.synthetix.cti.model.Analise;
import java.time.Instant;
import java.util.List;

/**
 * Resumo de uma planilha processada, com os insights gerados.
 */
public record AnaliseDTO(Long id, String arquivo, Instant geradoEm, int linhasLidas, int linhasValidas,
		int linhasRejeitadas, int avisos, int clientesCriados, int clientesAtualizados, List<InsightDTO> insights) {

	public static AnaliseDTO de(Analise analise) {
		return new AnaliseDTO(analise.getId(), analise.getArquivo(), analise.getGeradoEm(), analise.getLinhasLidas(),
				analise.getLinhasValidas(), analise.getLinhasRejeitadas(), analise.getAvisos(),
				analise.getClientesCriados(), analise.getClientesAtualizados(),
				analise.getInsights().stream().map(InsightDTO::de).toList());
	}

}
