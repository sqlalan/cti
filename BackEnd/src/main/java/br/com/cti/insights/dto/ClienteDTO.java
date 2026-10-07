package br.com.cti.insights.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Uma linha da planilha (aba upload_clientes) enviada pelo front depois da validacao do Pinia. Aqui
 * ficam so as regras declarativas; duplicidade e padronizacao sao do ClienteValidacaoService.
 */
public record ClienteDTO(

		// coluna codigo_cliente
		@NotBlank(message = "O código do cliente é obrigatório")
		@Size(max = 30, message = "O código do cliente deve ter no máximo 30 caracteres") String codigoCliente,

		// coluna nome_cliente
		@NotBlank(message = "O nome do cliente é obrigatório")
		@Size(max = 150, message = "O nome do cliente deve ter no máximo 150 caracteres") String nomeCliente,

		// coluna consultor
		@NotBlank(message = "O consultor é obrigatório")
		@Size(max = 120, message = "O consultor deve ter no máximo 120 caracteres") String consultor,

		// coluna segmento: "IND." e "Industria" passam aqui e viram "Indústria" no Service
		@NotBlank(message = "O segmento é obrigatório")
		@Size(max = 100, message = "O segmento deve ter no máximo 100 caracteres") String segmento,

		// coluna nivel_cliente: aceita minuscula porque o Service converte para maiuscula
		@NotBlank(message = "O nível do cliente é obrigatório")
		@Pattern(regexp = "^[ABCabc]$", message = "O nível deve ser A, B ou C") String nivelCliente,

		// coluna faturamento_anual
		@NotNull(message = "O faturamento anual é obrigatório")
		@Positive(message = "O faturamento anual deve ser maior que zero") Double faturamentoAnual,

		// coluna servicos_contratados: texto separado por ";" (Internet Dedicada;Firewall)
		@NotBlank(message = "Os serviços contratados são obrigatórios") String servicosContratados,

		// coluna data_contratacao, no formato aaaa-mm-dd
		@NotNull(message = "A data de contratação é obrigatória") LocalDate dataContratacao,

		// coluna cidade: opcional
		@Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres") String cidade,

		// coluna uf: opcional, mas com duas letras quando informada
		@Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "A UF deve possuir duas letras") String uf) {

}
