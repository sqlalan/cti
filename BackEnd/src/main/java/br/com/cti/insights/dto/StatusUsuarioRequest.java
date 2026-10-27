package br.com.cti.insights.dto;

import br.com.cti.insights.model.StatusUsuario;
import jakarta.validation.constraints.NotNull;

public record StatusUsuarioRequest(@NotNull(message = "Informe o status: PENDENTE, APROVADO ou BLOQUEADO") StatusUsuario status) {

}
