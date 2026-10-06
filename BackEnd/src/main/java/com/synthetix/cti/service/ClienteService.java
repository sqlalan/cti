package com.synthetix.cti.service;

import com.synthetix.cti.dto.ClienteDTO;
import com.synthetix.cti.dto.ClienteRequest;
import com.synthetix.cti.dto.ContratoDTO;
import com.synthetix.cti.dto.ContratoRequest;
import com.synthetix.cti.exception.ClienteDuplicadoException;
import com.synthetix.cti.exception.RecursoNaoEncontradoException;
import com.synthetix.cti.model.Cliente;
import com.synthetix.cti.model.Contrato;
import com.synthetix.cti.model.NivelCliente;
import com.synthetix.cti.repository.ClienteRepository;
import com.synthetix.cti.repository.ContratoRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClienteService {

	private final ClienteRepository clienteRepository;

	private final CatalogoService catalogo;

	private final ContratoRepository contratoRepository;

	public ClienteService(ClienteRepository clienteRepository, CatalogoService catalogo,
			ContratoRepository contratoRepository) {
		this.clienteRepository = clienteRepository;
		this.catalogo = catalogo;
		this.contratoRepository = contratoRepository;
	}

	public List<ClienteDTO> listar() {
		return clienteRepository.findAll().stream().map(ClienteDTO::de).toList();
	}

	public ClienteDTO buscar(Long id) {
		return ClienteDTO.de(buscarEntidade(id));
	}

	@Transactional
	public ClienteDTO criar(ClienteRequest request) {
		String codigo = request.codigo().trim().toUpperCase(Locale.ROOT);
		if (clienteRepository.existsByCodigo(codigo)) {
			throw new ClienteDuplicadoException(codigo);
		}
		Cliente cliente = new Cliente(codigo, request.nome(), catalogo.consultorPorNome(request.consultor()));
		preencher(cliente, request);
		return ClienteDTO.de(clienteRepository.save(cliente));
	}

	/**
	 * PUT: substitui todos os dados do cliente.
	 */
	@Transactional
	public ClienteDTO substituir(Long id, ClienteRequest request) {
		Cliente cliente = buscarEntidade(id);
		String codigo = request.codigo().trim().toUpperCase(Locale.ROOT);
		clienteRepository.findByCodigo(codigo).filter(outro -> !outro.getId().equals(id)).ifPresent(outro -> {
			throw new ClienteDuplicadoException(codigo);
		});
		cliente.setCodigo(codigo);
		cliente.setNome(request.nome());
		cliente.setConsultor(catalogo.consultorPorNome(request.consultor()));
		preencher(cliente, request);
		return ClienteDTO.de(clienteRepository.save(cliente));
	}

	/**
	 * PATCH: altera somente o nivel, sem exigir o restante dos dados.
	 */
	@Transactional
	public ClienteDTO atualizarNivel(Long id, NivelCliente nivel) {
		Cliente cliente = buscarEntidade(id);
		cliente.setNivel(nivel);
		return ClienteDTO.de(clienteRepository.save(cliente));
	}

	@Transactional
	public void remover(Long id) {
		clienteRepository.delete(buscarEntidade(id));
	}

	public List<ContratoDTO> listarContratos(Long clienteId) {
		buscarEntidade(clienteId);
		return contratoRepository.findByClienteId(clienteId).stream().map(ContratoDTO::de).toList();
	}

	@Transactional
	public ContratoDTO contratar(Long clienteId, ContratoRequest request) {
		Cliente cliente = buscarEntidade(clienteId);
		Contrato contrato = cliente.contratar(catalogo.servicoPorNome(request.servico()), request.dataContratacao());
		return ContratoDTO.de(contratoRepository.save(contrato));
	}

	@Transactional
	public ContratoDTO cancelarContrato(Long contratoId) {
		Contrato contrato = buscarContrato(contratoId);
		contrato.cancelar();
		return ContratoDTO.de(contrato);
	}

	@Transactional
	public void removerContrato(Long contratoId) {
		Contrato contrato = buscarContrato(contratoId);
		contrato.getCliente().removerContrato(contrato);
		contratoRepository.delete(contrato);
	}

	private void preencher(Cliente cliente, ClienteRequest request) {
		cliente.setSegmento(request.segmento());
		cliente.setNivel(request.nivel());
		cliente.setFaturamentoAnual(request.faturamentoAnual());
		cliente.setCidade(request.cidade());
		cliente.setUf(request.uf());
	}

	private Contrato buscarContrato(Long id) {
		return contratoRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Contrato", id));
	}

	private Cliente buscarEntidade(Long id) {
		return clienteRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
	}

}
