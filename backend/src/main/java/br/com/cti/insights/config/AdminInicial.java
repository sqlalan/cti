package br.com.cti.insights.config;

import br.com.cti.insights.model.Perfil;
import br.com.cti.insights.model.StatusUsuario;
import br.com.cti.insights.model.Usuario;
import br.com.cti.insights.repository.UsuarioRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Garante um administrador ao subir, para alguem poder aprovar o primeiro cadastro. Le ADMIN_EMAIL e
 * ADMIN_PASSWORD; sem ADMIN_EMAIL nao faz nada.
 */
@Component
public class AdminInicial implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

	private final AppProperties props;

	private final UsuarioRepository usuarios;

	private final PasswordEncoder passwordEncoder;

	public AdminInicial(AppProperties props, UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
		this.props = props;
		this.usuarios = usuarios;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		String email = props.admin() == null ? null : props.admin().email();
		if (!StringUtils.hasText(email)) {
			return;
		}
		email = email.trim().toLowerCase(Locale.ROOT);
		Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElse(null);

		if (usuario == null) {
			String senha = props.admin().senha();
			if (senha == null || senha.length() < 8) {
				log.warn("ADMIN_EMAIL {} nao tem conta e ADMIN_PASSWORD (8+ caracteres) nao foi definida", email);
				return;
			}
			usuario = new Usuario();
			usuario.setNome("Administrador");
			usuario.setEmail(email);
			usuario.setSenha(passwordEncoder.encode(senha));
		}
		else if (usuario.getPerfil() == Perfil.ADMIN && usuario.getStatus() == StatusUsuario.APROVADO) {
			return;
		}
		usuario.setPerfil(Perfil.ADMIN);
		usuario.setStatus(StatusUsuario.APROVADO);
		usuario.setAtivo(true);
		usuarios.save(usuario);
		log.info("Administrador garantido: {}", email);
	}

}
