package br.com.cti.insights.dto;

import java.time.Instant;

/**
 * Resposta do login: o token vai no header Authorization ("Bearer " + token) dos proximos pedidos.
 */
public record LoginResponse(String token, String tipo, Instant expiraEm, UsuarioDTO usuario) {

}
