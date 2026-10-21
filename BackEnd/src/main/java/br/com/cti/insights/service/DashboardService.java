package br.com.cti.insights.service;

import br.com.cti.insights.dto.DashboardDTO;
import br.com.cti.insights.dto.UploadLoteDTO;
import br.com.cti.insights.exception.RecursoNaoEncontradoException;
import br.com.cti.insights.model.EstatisticaLote;
import br.com.cti.insights.model.StatusLote;
import br.com.cti.insights.model.UploadLote;
import br.com.cti.insights.repository.EstatisticaLoteRepository;
import br.com.cti.insights.repository.UploadLoteRepository;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Le o que o Python gravou (estatistica_lote) para o dashboard. Nao calcula nada.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

	private final UploadLoteRepository lotes;

	private final EstatisticaLoteRepository estatisticas;

	private final JsonMapper json;

	public DashboardService(UploadLoteRepository lotes, EstatisticaLoteRepository estatisticas, JsonMapper json) {
		this.lotes = lotes;
		this.estatisticas = estatisticas;
		this.json = json;
	}

	/**
	 * Sem loteId: o ultimo lote PROCESSADO; se nenhum terminou ainda, o ultimo enviado, para a tela
	 * mostrar que o Python esta trabalhando (ou o erro).
	 */
	public DashboardDTO obter(Long loteId) {
		UploadLote lote = loteId != null
				? lotes.findById(loteId).orElseThrow(() -> new RecursoNaoEncontradoException("lote", loteId))
				: lotes.findFirstByStatusOrderByIdDesc(StatusLote.PROCESSADO)
					.or(lotes::findFirstByOrderByIdDesc)
					.orElseThrow(() -> new RecursoNaoEncontradoException(
							"Ainda não há lote processado: envie a planilha em Upload"));

		Optional<EstatisticaLote> resultado = estatisticas.findByLoteId(lote.getId());
		return new DashboardDTO(UploadLoteDTO.de(lote),
				resultado.map(e -> e.getGeradoEm().toInstant(ZoneOffset.UTC)).orElse(null),
				resultado.map(e -> json.readTree(e.getResultado())).orElse(null));
	}

}
