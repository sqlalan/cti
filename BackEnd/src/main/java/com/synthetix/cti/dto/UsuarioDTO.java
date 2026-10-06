package com.synthetix.cti.dto;

import com.synthetix.cti.model.Consultor;
import com.synthetix.cti.model.Usuario;
import java.time.Instant;

/**
 * Representacao de saida de um usuario. A senha nunca e exposta.
 */
public record UsuarioDTO(Long id, String nome, String email, String consultor, String matricula, boolean ativo,
		Instant criadoEm) {

	public static UsuarioDTO de(Usuario usuario) {
		Consultor consultor = usuario.getConsultor();
		return new UsuarioDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(),
				consultor == null ? null : consultor.getNome(), consultor == null ? null : consultor.getMatricula(),
				usuario.isAtivo(), usuario.getCriadoEm());
	}

}
