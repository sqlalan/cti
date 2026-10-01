package com.synthetix.cti.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados aceitos na criacao e na atualizacao de um usuario. O consultor e informado pelo nome, como
 * vem na planilha; se ainda nao existir, e cadastrado.
 */
public record UsuarioRequest(

		@NotBlank @Size(max = 120) String nome,

		@NotBlank @Email @Size(max = 180) String email,

		@NotBlank @Size(min = 8, max = 100) String senha,

		@NotBlank @Size(max = 120) String consultor) {

}
