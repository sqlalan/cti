package br.com.cti.insights.service;

import br.com.cti.insights.dto.AnaliseDTO;
import br.com.cti.insights.dto.AnaliseResumoDTO;
import br.com.cti.insights.dto.InsightDTO;
import br.com.cti.insights.exception.RecursoNaoEncontradoException;
import br.com.cti.insights.model.Analise;
import br.com.cti.insights.repository.AnaliseRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Leitura das planilhas ja processadas. O dashboard usa sempre a mais recente.
 */
@Service
@Transactional(readOnly = true)
public class AnaliseService {

	private final AnaliseRepository repository;

	public AnaliseService(AnaliseRepository repository) {
		this.repository = repository;
	}

	public List<AnaliseResumoDTO> listar() {
		return repository.findAllByOrderByGeradoEmDescIdDesc().stream().map(AnaliseResumoDTO::de).toList();
	}

	public AnaliseDTO buscar(Long id) {
		return AnaliseDTO.de(buscarEntidade(id));
	}

	public AnaliseDTO ultima() {
		return AnaliseDTO.de(ultimaEntidade());
	}

	public List<InsightDTO> insightsDaUltima() {
		return ultimaEntidade().getInsights().stream().map(InsightDTO::de).toList();
	}

	public String indicadoresDaUltima() {
		return ultimaEntidade().getIndicadores();
	}

	// Indicadores de qualquer envio, para o relatorio comparar uma planilha com a anterior.
	public String indicadores(Long id) {
		return buscarEntidade(id).getIndicadores();
	}

	public String problemas(Long id) {
		return buscarEntidade(id).getProblemas();
	}

	private Analise buscarEntidade(Long id) {
		return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("análise", id));
	}

	private Analise ultimaEntidade() {
		return repository.findFirstByOrderByGeradoEmDescIdDesc()
			.orElseThrow(() -> new RecursoNaoEncontradoException("Nenhuma planilha foi processada ainda"));
	}

}
