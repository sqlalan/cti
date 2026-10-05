package com.synthetix.cti.service;

import com.synthetix.cti.model.Consultor;
import com.synthetix.cti.model.Servico;
import com.synthetix.cti.repository.ConsultorRepository;
import com.synthetix.cti.repository.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultores e servicos chegam pelo nome, como na planilha: acha o existente (sem diferenciar
 * maiusculas) ou cadastra. Regra unica para cliente, usuario e importacao.
 */
@Service
@Transactional
public class CatalogoService {

	private final ConsultorRepository consultorRepository;

	private final ServicoRepository servicoRepository;

	public CatalogoService(ConsultorRepository consultorRepository, ServicoRepository servicoRepository) {
		this.consultorRepository = consultorRepository;
		this.servicoRepository = servicoRepository;
	}

	public Consultor consultorPorNome(String nome) {
		return consultorRepository.findByNomeIgnoreCase(nome.trim())
			.orElseGet(() -> consultorRepository.save(new Consultor(nome)));
	}

	public Servico servicoPorNome(String nome) {
		return servicoRepository.findByNomeIgnoreCase(nome.trim())
			.orElseGet(() -> servicoRepository.save(new Servico(nome)));
	}

}
