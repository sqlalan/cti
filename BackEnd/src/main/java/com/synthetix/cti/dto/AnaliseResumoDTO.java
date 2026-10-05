package com.synthetix.cti.dto;

import com.synthetix.cti.model.Analise;
import java.time.Instant;

/**
 * Linha do historico de planilhas processadas, sem os insights.
 */
public record AnaliseResumoDTO(Long id, String arquivo, Instant geradoEm, int linhasLidas, int linhasValidas,
		int linhasRejeitadas) {

	public static AnaliseResumoDTO de(Analise analise) {
		return new AnaliseResumoDTO(analise.getId(), analise.getArquivo(), analise.getGeradoEm(),
				analise.getLinhasLidas(), analise.getLinhasValidas(), analise.getLinhasRejeitadas());
	}

}
