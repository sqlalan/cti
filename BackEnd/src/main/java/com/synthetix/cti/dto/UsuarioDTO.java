package com.synthetix.cti.dto;

import com.synthetix.cti.model.Usuario;
import java.time.Instant;

/**
 * Representacao de saida de um usuario. A senha nunca e exposta.
 */
public record UsuarioDTO(Long id, String nome, String email, boolean ativo, Instant criadoEm) {

	public static UsuarioDTO de(Usuario usuario) {
		return new UsuarioDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.isAtivo(),
				usuario.getCriadoEm());
	}

}
