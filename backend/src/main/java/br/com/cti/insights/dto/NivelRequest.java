package br.com.cti.insights.dto;

import br.com.cti.insights.model.NivelCliente;
import jakarta.validation.constraints.NotNull;

/**
 * Atualizacao parcial (PATCH) do nivel ABC de um cliente.
 */
public record NivelRequest(@NotNull NivelCliente nivel) {

}
