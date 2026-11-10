package br.com.cti.insights.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.cti.insights.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	public static final String EMISSOR_TOKEN = "cti-api";

	private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioRepository usuarios) throws Exception {
		return http
			// API stateless com token no header: CSRF nao se aplica.
			.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// Abertos: health do deploy, cadastro (a conta nasce PENDENTE) e login. Cada pessoa ve a
			// propria conta em /api/auth/me. Administrar contas e so para ADMIN.
			.authorizeHttpRequests(requests -> requests.requestMatchers(HttpMethod.GET, "/api/health")
				.permitAll()
				.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login")
				.permitAll()
				.requestMatchers("/api/auth/me")
				.authenticated()
				.requestMatchers("/api/admin/**", "/api/usuarios", "/api/usuarios/**")
				.hasRole("ADMIN")
				.anyRequest()
				.authenticated())
			.oauth2ResourceServer(servidor -> servidor.jwt(Customizer.withDefaults()))
			// Conta bloqueada perde o acesso na hora, mesmo com o token ainda na validade.
			.addFilterAfter(new ContaLiberadaFilter(usuarios), BearerTokenAuthenticationFilter.class)
			.build();
	}

	/**
	 * Chave HMAC que assina os tokens. Sem JWT_SECRET gera uma aleatoria: seguro, mas todo mundo
	 * precisa entrar de novo quando a API reinicia.
	 */
	@Bean
	SecretKey chaveJwt(@Value("${cti.jwt.segredo:}") String segredo) {
		byte[] bytes;
		if (StringUtils.hasText(segredo)) {
			bytes = segredo.getBytes(StandardCharsets.UTF_8);
			if (bytes.length < 32) {
				throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 32 caracteres (256 bits)");
			}
		}
		else {
			log.warn("JWT_SECRET nao definido: usando chave aleatoria; os tokens deixam de valer quando a API reinicia");
			bytes = new byte[32];
			new SecureRandom().nextBytes(bytes);
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey chaveJwt) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey chaveJwt) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
		// Alem da assinatura e da validade, so aceita token emitido por esta API.
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISSOR_TOKEN));
		return decoder;
	}

	/**
	 * O claim "perfil" do token vira ROLE_USER ou ROLE_ADMIN, como o getAuthorities() do guia.
	 * As rotas de administracao de contas exigem ROLE_ADMIN (regra no securityFilterChain).
	 */
	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter perfis = new JwtGrantedAuthoritiesConverter();
		perfis.setAuthoritiesClaimName("perfil");
		perfis.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
		conversor.setJwtGrantedAuthoritiesConverter(perfis);
		return conversor;
	}

	// Origens do Front-end (Vite local, Vercel...) separadas por virgula em CORS_ORIGIN.
	@Bean
	CorsConfigurationSource corsConfigurationSource(@Value("${cti.cors.origens}") List<String> origens) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(origens);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}

	/**
	 * Grava BCrypt puro ("$2a$10$...", 60 caracteres), como o guia confere no banco. Hash antigo
	 * com prefixo ("{bcrypt}$2a$10$...") continua entrando: quem se cadastrou antes nao perde a conta.
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
		PasswordEncoder comPrefixo = PasswordEncoderFactories.createDelegatingPasswordEncoder();
		return new PasswordEncoder() {

			@Override
			public String encode(CharSequence senha) {
				return bcrypt.encode(senha);
			}

			@Override
			public boolean matches(CharSequence senha, String hash) {
				if (hash == null) {
					return false;
				}
				return hash.startsWith("{") ? comPrefixo.matches(senha, hash) : bcrypt.matches(senha, hash);
			}

		};
	}

}
