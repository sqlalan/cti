package com.synthetix.cti.dto;

import com.synthetix.cti.model.NivelCliente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Dados aceitos na criacao e na substituicao (PUT) de um cliente. O consultor e informado pelo
 * nome, como vem na planilha; se ainda nao existir, e cadastrado.
 */
public record ClienteRequest(

		@NotBlank @Size(max = 30) String codigo,

		@NotBlank @Size(max = 160) String nome,

		@NotBlank @Size(max = 120) String consultor,

		@NotBlank @Size(max = 60) String segmento,

		@NotNull NivelCliente nivel,

		@NotNull @PositiveOrZero BigDecimal faturamentoAnual,

		@NotBlank @Size(max = 100) String cidade,

		@NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "deve ser a sigla da UF com 2 letras") String uf) {

}
