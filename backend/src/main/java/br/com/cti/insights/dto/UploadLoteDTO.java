package br.com.cti.insights.dto;

import br.com.cti.insights.model.StatusLote;
import br.com.cti.insights.model.UploadLote;
import java.time.Instant;

public record UploadLoteDTO(Long id, String arquivoNome, String enviadoPor, Instant enviadoEm, StatusLote status,
		int totalLinhas, int linhasValidas, int linhasInvalidas, String mensagem) {

	public static UploadLoteDTO de(UploadLote lote) {
		return new UploadLoteDTO(lote.getId(), lote.getArquivoNome(), lote.getEnviadoPor(), lote.getEnviadoEm(),
				lote.getStatus(), lote.getTotalLinhas(), lote.getLinhasValidas(), lote.getLinhasInvalidas(),
				lote.getMensagem());
	}

}
