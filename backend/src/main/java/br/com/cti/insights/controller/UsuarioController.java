package br.com.cti.insights.controller;

import br.com.cti.insights.dto.UsuarioDTO;
import br.com.cti.insights.dto.UsuarioRequest;
import br.com.cti.insights.service.UsuarioService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administracao de contas (so ADMIN). Cadastro, login e /me ficam em AuthController.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

	private final UsuarioService service;

	public UsuarioController(UsuarioService service) {
		this.service = service;
	}

	@GetMapping
	public List<UsuarioDTO> listar() {
		return service.listar();
	}

	@GetMapping("/{id}")
	public UsuarioDTO buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	// Cadastro feito pelo administrador; o publico se cadastra em /api/auth/register.
	@PostMapping
	public ResponseEntity<UsuarioDTO> criar(@Valid @RequestBody UsuarioRequest request) {
		UsuarioDTO criado = service.criar(request);
		return ResponseEntity.created(URI.create("/api/usuarios/" + criado.id())).body(criado);
	}

	@PutMapping("/{id}")
	public UsuarioDTO atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
		return service.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable Long id) {
		service.remover(id);
		return ResponseEntity.noContent().build();
	}

}
