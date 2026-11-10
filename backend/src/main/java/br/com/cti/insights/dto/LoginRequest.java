package br.com.cti.insights.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Credenciais enviadas no login.
 */
public record LoginRequest(

		@NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") String email,

		@NotBlank(message = "Informe a senha") String senha) {

}
