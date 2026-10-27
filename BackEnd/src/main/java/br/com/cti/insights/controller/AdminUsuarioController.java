package br.com.cti.insights.controller;

import br.com.cti.insights.dto.PerfilRequest;
import br.com.cti.insights.dto.StatusUsuarioRequest;
import br.com.cti.insights.dto.UsuarioDTO;
import br.com.cti.insights.model.StatusUsuario;
import br.com.cti.insights.service.UsuarioService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liberacao de acesso: so ADMIN (regra no SecurityConfig). O cadastro publico nasce PENDENTE.
 */
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {

	private final UsuarioService service;

	public AdminUsuarioController(UsuarioService service) {
		this.service = service;
	}

	// GET /api/admin/usuarios?status=PENDENTE (sem status: todos)
	@GetMapping
	public List<UsuarioDTO> listar(@RequestParam(required = false) StatusUsuario status) {
		return service.listarPorStatus(status);
	}

	// PATCH /api/admin/usuarios/7/status {"status":"APROVADO"}
	@PatchMapping("/{id}/status")
	public UsuarioDTO alterarStatus(@PathVariable Long id, @Valid @RequestBody StatusUsuarioRequest request,
			Principal admin) {
		return service.alterarStatus(id, request.status(), admin.getName());
	}

	// PATCH /api/admin/usuarios/7/perfil {"perfil":"ADMIN"}
	@PatchMapping("/{id}/perfil")
	public UsuarioDTO alterarPerfil(@PathVariable Long id, @Valid @RequestBody PerfilRequest request, Principal admin) {
		return service.alterarPerfil(id, request.perfil(), admin.getName());
	}

}
