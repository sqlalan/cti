package com.synthetix.cti.controller;

import com.synthetix.cti.dto.LoginRequest;
import com.synthetix.cti.dto.LoginResponse;
import com.synthetix.cti.dto.UsuarioDTO;
import com.synthetix.cti.dto.UsuarioRequest;
import com.synthetix.cti.service.UsuarioService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

	// Quem esta logado: o e-mail vem do proprio token ("sub"), nunca da URL.
	@GetMapping("/me")
	public UsuarioDTO me(@AuthenticationPrincipal Jwt token) {
		return service.logado(token.getSubject());
	}

	@GetMapping("/{id}")
	public UsuarioDTO buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@PostMapping
	public ResponseEntity<UsuarioDTO> criar(@Valid @RequestBody UsuarioRequest request) {
		UsuarioDTO criado = service.criar(request);
		return ResponseEntity.created(URI.create("/api/usuarios/" + criado.id())).body(criado);
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return service.login(request);
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
