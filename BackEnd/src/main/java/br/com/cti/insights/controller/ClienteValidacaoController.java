package br.com.cti.insights.controller;

import br.com.cti.insights.dto.ClienteDTO;
import br.com.cti.insights.service.ClienteValidacaoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recebe as linhas que o Pinia aprovou, valida de novo (DTO + Service) e devolve a lista
 * padronizada. Sem banco e sem Python nesta etapa.
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteValidacaoController {

	private final ClienteValidacaoService service;

	public ClienteValidacaoController(ClienteValidacaoService service) {
		this.service = service;
	}

	// POST /api/clientes/validar: @Valid em cada item da lista
	@PostMapping("/validar")
	public ResponseEntity<List<ClienteDTO>> validarClientes(@RequestBody List<@Valid ClienteDTO> clientes) {
		return ResponseEntity.ok(service.validarEPreparar(clientes));
	}

}
