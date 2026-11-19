package br.com.cti.insights.dto;

import br.com.cti.insights.model.StatusLote;
import br.com.cti.insights.model.UploadLote;
import java.time.Instant;

public record UploadLoteDTO(Long id, String arquivoNome, Instant enviadoEm, StatusLote status, int totalLinhas,
		int linhasValidas, int linhasInvalidas, int linhasComAviso, String mensagem) {

	public static UploadLoteDTO de(UploadLote lote) {
		return new UploadLoteDTO(lote.getId(), lote.getArquivoNome(), lote.getEnviadoEm(), lote.getStatus(),
				lote.getTotalLinhas(), lote.getLinhasValidas(), lote.getLinhasInvalidas(), lote.getLinhasComAviso(),
				lote.getMensagem());
	}

}
