package br.com.cti.insights.controller;

import br.com.cti.insights.dto.ClienteDTO;
import br.com.cti.insights.dto.ClienteRequest;
import br.com.cti.insights.dto.ContratoDTO;
import br.com.cti.insights.dto.ContratoRequest;
import br.com.cti.insights.dto.NivelRequest;
import br.com.cti.insights.service.ClienteService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

	private final ClienteService service;

	public ClienteController(ClienteService service) {
		this.service = service;
	}

	@GetMapping
	public List<ClienteDTO> listar() {
		return service.listar();
	}

	@GetMapping("/{id}")
	public ClienteDTO buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@PostMapping
	public ResponseEntity<ClienteDTO> criar(@Valid @RequestBody ClienteRequest request) {
		ClienteDTO criado = service.criar(request);
		return ResponseEntity.created(URI.create("/api/clientes/" + criado.id())).body(criado);
	}

	@PutMapping("/{id}")
	public ClienteDTO substituir(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
		return service.substituir(id, request);
	}

	@PatchMapping("/{id}/nivel")
	public ClienteDTO atualizarNivel(@PathVariable Long id, @Valid @RequestBody NivelRequest request) {
		return service.atualizarNivel(id, request.nivel());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable Long id) {
		service.remover(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/contratos")
	public List<ContratoDTO> listarContratos(@PathVariable Long id) {
		return service.listarContratos(id);
	}

	@PostMapping("/{id}/contratos")
	public ResponseEntity<ContratoDTO> contratar(@PathVariable Long id, @Valid @RequestBody ContratoRequest request) {
		ContratoDTO criado = service.contratar(id, request);
		return ResponseEntity.created(URI.create("/api/contratos/" + criado.id())).body(criado);
	}

}
