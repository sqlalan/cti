package br.com.cti.insights.dto;

import br.com.cti.insights.model.Consultor;
import br.com.cti.insights.model.Perfil;
import br.com.cti.insights.model.Usuario;
import java.time.Instant;

/**
 * Representacao de saida de um usuario. A senha nunca e exposta.
 */
public record UsuarioDTO(Long id, String nome, String email, Perfil perfil, String consultor, String matricula,
		boolean ativo, Instant criadoEm) {

	public static UsuarioDTO de(Usuario usuario) {
		Consultor consultor = usuario.getConsultor();
		return new UsuarioDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(),
				consultor == null ? null : consultor.getNome(), consultor == null ? null : consultor.getMatricula(),
				usuario.isAtivo(), usuario.getCriadoEm());
	}

}
