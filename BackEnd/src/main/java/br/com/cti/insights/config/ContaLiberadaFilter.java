package br.com.cti.insights.config;

import br.com.cti.insights.model.Usuario;
import br.com.cti.insights.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * O token vale 8 h, mas bloquear uma conta tem que tirar o acesso na hora: a cada pedido com token,
 * confere no banco se a conta continua ativa e aprovada. Se nao, responde 401 e o front encerra a
 * sessao.
 *
 * Nao e @Component: o Spring Boot registraria o filtro tambem fora da cadeia de seguranca.
 */
public class ContaLiberadaFilter extends OncePerRequestFilter {

	private final UsuarioRepository usuarios;

	public ContaLiberadaFilter(UsuarioRepository usuarios) {
		this.usuarios = usuarios;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacao instanceof JwtAuthenticationToken token) {
			boolean liberada = usuarios.findByEmailIgnoreCase(token.getName()).map(Usuario::podeEntrar).orElse(false);
			if (!liberada) {
				SecurityContextHolder.clearContext();
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setContentType(MediaType.APPLICATION_JSON_VALUE);
				response.setCharacterEncoding(StandardCharsets.UTF_8.name());
				response.getWriter().write("{\"mensagem\":\"Sua conta não está liberada para acessar o sistema\"}");
				return;
			}
		}
		chain.doFilter(request, response);
	}

}
