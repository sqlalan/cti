package com.synthetix.cti.service;

import com.synthetix.cti.config.SecurityConfig;
import com.synthetix.cti.dto.LoginResponse;
import com.synthetix.cti.dto.UsuarioDTO;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emite o JWT do login. O "sub" e o e-mail do usuario; nome, perfil e consultor vao junto.
 */
@Service
public class TokenService {

	private final JwtEncoder encoder;

	private final Duration validade;

	public TokenService(JwtEncoder encoder, @Value("${cti.jwt.validade:8h}") Duration validade) {
		this.encoder = encoder;
		this.validade = validade;
	}

	public LoginResponse emitir(UsuarioDTO usuario) {
		Instant agora = Instant.now();
		Instant expiraEm = agora.plus(validade);
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
			.issuer(SecurityConfig.EMISSOR_TOKEN)
			.issuedAt(agora)
			.expiresAt(expiraEm)
			.subject(usuario.email())
			.claim("nome", usuario.nome())
			.claim("perfil", usuario.perfil().name());
		if (usuario.consultor() != null) {
			claims.claim("consultor", usuario.consultor());
		}
		JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = encoder.encode(JwtEncoderParameters.from(cabecalho, claims.build())).getTokenValue();
		return new LoginResponse(token, "Bearer", expiraEm, usuario);
	}

}
