package br.com.cti.insights.service;

import br.com.cti.insights.dto.LoginRequest;
import br.com.cti.insights.dto.LoginResponse;
import br.com.cti.insights.dto.UsuarioDTO;
import br.com.cti.insights.dto.UsuarioRequest;
import br.com.cti.insights.exception.ConsultorJaVinculadoException;
import br.com.cti.insights.exception.ContaNaoLiberadaException;
import br.com.cti.insights.exception.CredenciaisInvalidasException;
import br.com.cti.insights.exception.EmailDuplicadoException;
import br.com.cti.insights.exception.RecursoNaoEncontradoException;
import br.com.cti.insights.model.Consultor;
import br.com.cti.insights.model.Perfil;
import br.com.cti.insights.model.StatusUsuario;
import br.com.cti.insights.model.Usuario;
import br.com.cti.insights.repository.ConsultorRepository;
import br.com.cti.insights.repository.UsuarioRepository;
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

	private final TentativasLoginService tentativas;

	public UsuarioService(UsuarioRepository repository, CatalogoService catalogo,
			PasswordEncoder passwordEncoder, TokenService tokenService, TentativasLoginService tentativas) {
		this.repository = repository;
		this.catalogo = catalogo;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.tentativas = tentativas;
	}

	public List<UsuarioDTO> listar() {
		return repository.findAll().stream().map(UsuarioDTO::de).toList();
	}

	public UsuarioDTO buscar(Long id) {
		return UsuarioDTO.de(buscarEntidade(id));
	}

	// Mesma mensagem para e-mail inexistente e senha errada: nao revela quais e-mails existem. So quem
	// acertou a senha fica sabendo que a conta aguarda aprovacao ou foi bloqueada.
	// Senha errada conta tentativa (TentativasLoginService); senha certa zera.
	public LoginResponse login(LoginRequest request) {
		String email = normalizar(request.email());
		tentativas.conferir(email);
		Usuario usuario = repository.findByEmailIgnoreCase(email)
			.filter(Usuario::isAtivo)
			.filter(u -> passwordEncoder.matches(request.senha(), u.getSenha()))
			.orElse(null);
		if (usuario == null) {
			tentativas.registrarFalha(email);
			throw new CredenciaisInvalidasException();
		}
		tentativas.registrarSucesso(email);
		if (usuario.getStatus() == StatusUsuario.PENDENTE) {
			throw new ContaNaoLiberadaException("Sua conta aguarda a aprovação do administrador");
		}
		if (usuario.getStatus() == StatusUsuario.BLOQUEADO) {
			throw new ContaNaoLiberadaException("Sua conta está bloqueada. Fale com o administrador");
		}
		return tokenService.emitir(UsuarioDTO.de(usuario));
	}

	// Tela de administracao: filtra por status; sem status, todos. Mais recentes primeiro.
	public List<UsuarioDTO> listarPorStatus(StatusUsuario status) {
		List<Usuario> usuarios = status == null ? repository.findAllByOrderByCriadoEmDesc()
				: repository.findByStatusOrderByCriadoEmDesc(status);
		return usuarios.stream().map(UsuarioDTO::de).toList();
	}

	/**
	 * Aprovar, bloquear ou devolver para pendente. O admin nao mexe na propria conta: bloquear a si
	 * mesmo deixaria o sistema sem ninguem para liberar acessos.
	 */
	@Transactional
	public UsuarioDTO alterarStatus(Long id, StatusUsuario status, String emailAdmin) {
		Usuario usuario = buscarEntidade(id);
		recusarPropriaConta(usuario, emailAdmin);
		usuario.setStatus(status);
		return UsuarioDTO.de(repository.save(usuario));
	}

	// Quem vira ADMIN ja fica aprovado. O perfil novo vale a partir do proximo login (vai no token).
	@Transactional
	public UsuarioDTO alterarPerfil(Long id, Perfil perfil, String emailAdmin) {
		Usuario usuario = buscarEntidade(id);
		recusarPropriaConta(usuario, emailAdmin);
		usuario.setPerfil(perfil);
		if (perfil == Perfil.ADMIN) {
			usuario.setStatus(StatusUsuario.APROVADO);
		}
		return UsuarioDTO.de(repository.save(usuario));
	}

	private static void recusarPropriaConta(Usuario usuario, String emailAdmin) {
		if (usuario.getEmail().equalsIgnoreCase(emailAdmin)) {
			throw new IllegalArgumentException("Você não pode alterar o status ou o perfil da sua própria conta");
		}
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
		// Cadastro publico: entra so depois da aprovacao de um administrador.
		usuario.setStatus(StatusUsuario.PENDENTE);
		usuario.setConsultor(consultorLivre(nomeDoConsultor(request), null));
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
		usuario.setConsultor(consultorLivre(nomeDoConsultor(request), id));
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
			.filter(Usuario::podeEntrar)
			.map(UsuarioDTO::de)
			.orElseThrow(CredenciaisInvalidasException::new);
	}

	// "Ana@Email.com " e "ana@email.com" sao o mesmo login.
	private static String normalizar(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	// Cada usuario e um consultor: sem "consultor" no pedido, o proprio nome e o da planilha.
	private static String nomeDoConsultor(UsuarioRequest request) {
		return request.consultor() == null || request.consultor().isBlank() ? request.nome() : request.consultor();
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
