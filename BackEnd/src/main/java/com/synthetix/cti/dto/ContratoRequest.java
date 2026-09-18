package com.synthetix.cti.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Novo contrato de um cliente. O servico e informado pelo nome; se ainda nao existir no catalogo,
 * e cadastrado.
 */
public record ContratoRequest(@NotBlank @Size(max = 80) String servico,

		@NotNull @PastOrPresent LocalDate dataContratacao) {

}
