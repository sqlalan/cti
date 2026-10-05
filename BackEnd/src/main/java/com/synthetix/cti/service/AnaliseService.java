package com.synthetix.cti.service;

import com.synthetix.cti.dto.AnaliseDTO;
import com.synthetix.cti.dto.AnaliseResumoDTO;
import com.synthetix.cti.dto.InsightDTO;
import com.synthetix.cti.exception.RecursoNaoEncontradoException;
import com.synthetix.cti.model.Analise;
import com.synthetix.cti.repository.AnaliseRepository;
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

	public String problemas(Long id) {
		return buscarEntidade(id).getProblemas();
	}

	private Analise buscarEntidade(Long id) {
		return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Analise", id));
	}

	private Analise ultimaEntidade() {
		return repository.findFirstByOrderByGeradoEmDescIdDesc()
			.orElseThrow(() -> new RecursoNaoEncontradoException("Nenhuma planilha foi processada ainda"));
	}

}
