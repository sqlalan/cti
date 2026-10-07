package br.com.cti.insights.dto;

import br.com.cti.insights.validacao.CabeNoBcrypt;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados aceitos na criacao e na atualizacao de um usuario. Cada usuario e um consultor: sem
 * "consultor", vale o proprio nome, que deve ser o da planilha. Consultor que ainda nao existe e
 * cadastrado. A matricula e opcional.
 */
public record UsuarioRequest(

		@NotBlank(message = "Informe o nome") @Size(max = 120, message = "Nome com no máximo 120 caracteres") String nome,

		@NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido")
		@Size(max = 180, message = "E-mail com no máximo 180 caracteres") String email,

		@CabeNoBcrypt @NotBlank(message = "Informe a senha")
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
				message = "A senha deve ter de 8 a 72 caracteres, com letras e números") String senha,

		// opcional: vazio = o proprio nome
		@Size(max = 120, message = "Consultor com no máximo 120 caracteres") String consultor,

		@Size(max = 20, message = "Matrícula com no máximo 20 caracteres") String matricula) {

}
