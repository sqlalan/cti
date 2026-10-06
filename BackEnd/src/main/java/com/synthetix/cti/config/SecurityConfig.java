package com.synthetix.cti.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			// API stateless com token no header: CSRF nao se aplica.
			.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// Cadastro e login abertos: o proprio consultor cria a conta e recebe o token no login.
			.authorizeHttpRequests(requests -> requests.requestMatchers(HttpMethod.POST, "/api/usuarios", "/api/usuarios/login")
				.permitAll()
				.anyRequest()
				.authenticated())
			.oauth2ResourceServer(servidor -> servidor.jwt(Customizer.withDefaults()))
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

	// Origens do Front-end (Vite local, Vercel...) separadas por virgula em CORS_ORIGENS.
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
