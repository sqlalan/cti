package br.com.cti.insights.controller;

import br.com.cti.insights.dto.LoginRequest;
import br.com.cti.insights.dto.LoginResponse;
import br.com.cti.insights.dto.UsuarioDTO;
import br.com.cti.insights.dto.UsuarioRequest;
import br.com.cti.insights.service.UsuarioService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cadastro, login e "quem sou eu" (/api/auth). Administrar contas fica em /api/usuarios.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UsuarioService service;

	public AuthController(UsuarioService service) {
		this.service = service;
	}

	// A conta nasce PENDENTE: so entra depois que o administrador aprovar.
	@PostMapping("/register")
	public ResponseEntity<UsuarioDTO> register(@Valid @RequestBody UsuarioRequest request) {
		UsuarioDTO criado = service.criar(request);
		return ResponseEntity.created(URI.create("/api/usuarios/" + criado.id())).body(criado);
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return service.login(request);
	}

	// O e-mail vem do proprio token ("sub"), nunca da URL.
	@GetMapping("/me")
	public UsuarioDTO me(@AuthenticationPrincipal Jwt token) {
		return service.logado(token.getSubject());
	}

}
