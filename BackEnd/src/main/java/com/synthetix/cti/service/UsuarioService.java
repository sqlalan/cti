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
import java.util.Locale;
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
		return repository.findByEmailIgnoreCase(normalizar(request.email()))
			.filter(Usuario::isAtivo)
			.filter(usuario -> passwordEncoder.matches(request.senha(), usuario.getSenha()))
			.map(UsuarioDTO::de)
			.map(tokenService::emitir)
			.orElseThrow(CredenciaisInvalidasException::new);
	}

	@Transactional
	public UsuarioDTO criar(UsuarioRequest request) {
		String email = normalizar(request.email());
		if (repository.existsByEmailIgnoreCase(email)) {
			throw new EmailDuplicadoException();
		}
		Usuario usuario = new Usuario();
		usuario.setNome(request.nome());
		usuario.setEmail(email);
		usuario.setSenha(passwordEncoder.encode(request.senha()));
		usuario.setConsultor(consultorLivre(request.consultor(), null));
		catalogo.definirMatricula(usuario.getConsultor(), request.matricula());
		return UsuarioDTO.de(repository.save(usuario));
	}

	@Transactional
	public UsuarioDTO atualizar(Long id, UsuarioRequest request) {
		Usuario usuario = buscarEntidade(id);
		String email = normalizar(request.email());
		repository.findByEmailIgnoreCase(email).filter(outro -> !outro.getId().equals(id)).ifPresent(outro -> {
			throw new EmailDuplicadoException();
		});
		usuario.setNome(request.nome());
		usuario.setEmail(email);
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
			.orElseThrow(() -> new RecursoNaoEncontradoException("usuário", id));
	}

	/**
	 * Quem esta logado, pelo e-mail que o token carrega ("sub"). Conta apagada ou desativada depois
	 * do login deixa de valer mesmo com o token ainda na validade.
	 */
	public UsuarioDTO logado(String email) {
		return repository.findByEmailIgnoreCase(email)
			.filter(Usuario::isAtivo)
			.map(UsuarioDTO::de)
			.orElseThrow(CredenciaisInvalidasException::new);
	}

	// "Ana@Email.com " e "ana@email.com" sao o mesmo login.
	private static String normalizar(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
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
