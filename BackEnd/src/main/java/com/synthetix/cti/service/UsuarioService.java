package com.synthetix.cti.service;

import com.synthetix.cti.dto.UsuarioDTO;
import com.synthetix.cti.dto.UsuarioRequest;
import com.synthetix.cti.exception.EmailDuplicadoException;
import com.synthetix.cti.exception.RecursoNaoEncontradoException;
import com.synthetix.cti.model.Usuario;
import com.synthetix.cti.repository.UsuarioRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

	private final UsuarioRepository repository;

	private final PasswordEncoder passwordEncoder;

	public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	public List<UsuarioDTO> listar() {
		return repository.findAll().stream().map(UsuarioDTO::de).toList();
	}

	public UsuarioDTO buscar(Long id) {
		return UsuarioDTO.de(buscarEntidade(id));
	}

	@Transactional
	public UsuarioDTO criar(UsuarioRequest request) {
		if (repository.existsByEmail(request.email())) {
			throw new EmailDuplicadoException(request.email());
		}
		Usuario usuario = new Usuario();
		usuario.setNome(request.nome());
		usuario.setEmail(request.email());
		usuario.setSenha(passwordEncoder.encode(request.senha()));
		return UsuarioDTO.de(repository.save(usuario));
	}

	@Transactional
	public UsuarioDTO atualizar(Long id, UsuarioRequest request) {
		Usuario usuario = buscarEntidade(id);
		repository.findByEmail(request.email()).filter(outro -> !outro.getId().equals(id)).ifPresent(outro -> {
			throw new EmailDuplicadoException(request.email());
		});
		usuario.setNome(request.nome());
		usuario.setEmail(request.email());
		usuario.setSenha(passwordEncoder.encode(request.senha()));
		return UsuarioDTO.de(repository.save(usuario));
	}

	@Transactional
	public void remover(Long id) {
		repository.delete(buscarEntidade(id));
	}

	private Usuario buscarEntidade(Long id) {
		return repository.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", id));
	}

}
