package br.com.cti.insights.dto;

import br.com.cti.insights.model.Perfil;
import jakarta.validation.constraints.NotNull;

public record PerfilRequest(@NotNull(message = "Informe o perfil: USER ou ADMIN") Perfil perfil) {

}
