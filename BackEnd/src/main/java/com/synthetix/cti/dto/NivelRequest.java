package com.synthetix.cti.dto;

import com.synthetix.cti.model.NivelCliente;
import jakarta.validation.constraints.NotNull;

/**
 * Atualizacao parcial (PATCH) do nivel ABC de um cliente.
 */
public record NivelRequest(@NotNull NivelCliente nivel) {

}
