package com.synthetix.cti.service;

import com.synthetix.cti.dto.LoginRequest;
import com.synthetix.cti.dto.LoginResponse;
import com.synthetix.cti.dto.UsuarioDTO;
import com.synthetix.cti.dto.UsuarioRequest;
import com.synthetix.cti.exception.ConsultorJaVinculadoException;
import com.synthetix.cti.exception.CredenciaisInvalidasException;
import com.synthetix.cti.exception.EmailDuplicadoException;
import com.synthetix.cti.exception.RecursoNaoEncontradoException;
import com.synthetix.cti.model.Consultor;
import com.synthetix.cti.model.Usuario;
import com.synthetix.cti.repository.ConsultorRepository;
import com.synthetix.cti.repository.UsuarioRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

	private final UsuarioRepository repository;

	private final CatalogoService catalogo;

	private final PasswordEncoder passwordEncoder;

	private final TokenService tokenService;

	public UsuarioService(UsuarioRepository repository, CatalogoService catalogo,
			PasswordEncoder passwordEncoder, TokenService tokenService) {
		this.repository = repository;
		this.catalogo = catalogo;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
	}

	public List<UsuarioDTO> listar() {
		return repository.findAll().stream().map(UsuarioDTO::de).toList();
	}

	public UsuarioDTO buscar(Long id) {
		return UsuarioDTO.de(buscarEntidade(id));
	}

	// Mesma mensagem para e-mail inexistente e senha errada: nao revela quais e-mails existem.
	public LoginResponse login(LoginRequest request) {
		return repository.findByEmail(request.email())
			.filter(Usuario::isAtivo)
			.filter(usuario -> passwordEncoder.matches(request.senha(), usuario.getSenha()))
			.map(UsuarioDTO::de)
			.map(tokenService::emitir)
			.orElseThrow(CredenciaisInvalidasException::new);
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
		usuario.setConsultor(consultorLivre(request.consultor(), null));
		catalogo.definirMatricula(usuario.getConsultor(), request.matricula());
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
		usuario.setConsultor(consultorLivre(request.consultor(), id));
		catalogo.definirMatricula(usuario.getConsultor(), request.matricula());
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

	// Mesmo criterio da planilha: acha pelo nome ou cadastra. Cada consultor tem um usuario so.
	private Consultor consultorLivre(String nome, Long idDoUsuario) {
		Consultor consultor = catalogo.consultorPorNome(nome);
		repository.findByConsultor(consultor).filter(outro -> !outro.getId().equals(idDoUsuario)).ifPresent(outro -> {
			throw new ConsultorJaVinculadoException(consultor.getNome());
		});
		return consultor;
	}

}
