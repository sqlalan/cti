package br.com.cti.insights.service;

import br.com.cti.insights.exception.MatriculaDuplicadaException;
import br.com.cti.insights.model.Consultor;
import br.com.cti.insights.model.Segmento;
import br.com.cti.insights.model.Servico;
import br.com.cti.insights.repository.ConsultorRepository;
import br.com.cti.insights.repository.SegmentoRepository;
import br.com.cti.insights.repository.ServicoRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultores, segmentos e servicos chegam pelo nome, como na planilha: acha o existente (sem diferenciar
 * maiusculas) ou cadastra. Regra unica para cliente, usuario e importacao.
 */
@Service
@Transactional
public class CatalogoService {

	private final ConsultorRepository consultorRepository;

	private final SegmentoRepository segmentoRepository;

	private final ServicoRepository servicoRepository;

	public CatalogoService(ConsultorRepository consultorRepository, SegmentoRepository segmentoRepository,
			ServicoRepository servicoRepository) {
		this.consultorRepository = consultorRepository;
		this.segmentoRepository = segmentoRepository;
		this.servicoRepository = servicoRepository;
	}

	public Consultor consultorPorNome(String nome) {
		return consultorRepository.findByNomeIgnoreCase(nome.trim())
			.orElseGet(() -> consultorRepository.save(new Consultor(nome)));
	}

	/**
	 * Matricula em branco mantem a atual; preenchida, nao pode ser de outro consultor.
	 */
	public void definirMatricula(Consultor consultor, String matricula) {
		if (matricula == null || matricula.isBlank()) {
			return;
		}
		String normalizada = matricula.trim().toUpperCase(Locale.ROOT);
		consultorRepository.findByMatricula(normalizada)
			.filter(outro -> !outro.getId().equals(consultor.getId()))
			.ifPresent(outro -> {
				throw new MatriculaDuplicadaException(normalizada);
			});
		consultor.setMatricula(normalizada);
	}

	public Segmento segmentoPorNome(String nome) {
		return segmentoRepository.findByNomeIgnoreCase(nome.trim())
			.orElseGet(() -> segmentoRepository.save(new Segmento(nome)));
	}

	public Servico servicoPorNome(String nome) {
		return servicoRepository.findByNomeIgnoreCase(nome.trim())
			.orElseGet(() -> servicoRepository.save(new Servico(nome)));
	}

}
