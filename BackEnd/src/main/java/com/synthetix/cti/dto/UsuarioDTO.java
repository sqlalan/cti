package com.synthetix.cti.dto;

import com.synthetix.cti.model.Usuario;
import java.time.Instant;

/**
 * Representacao de saida de um usuario. A senha nunca e exposta.
 */
public record UsuarioDTO(Long id, String nome, String email, String consultor, boolean ativo, Instant criadoEm) {

	public static UsuarioDTO de(Usuario usuario) {
		String consultor = usuario.getConsultor() == null ? null : usuario.getConsultor().getNome();
		return new UsuarioDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(), consultor, usuario.isAtivo(),
				usuario.getCriadoEm());
	}

}
