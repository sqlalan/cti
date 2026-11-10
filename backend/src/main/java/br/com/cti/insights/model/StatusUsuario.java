package br.com.cti.insights.model;

/**
 * Liberacao da conta. O cadastro publico nasce PENDENTE e so entra depois que um ADMIN aprova.
 */
public enum StatusUsuario {

	PENDENTE, APROVADO, BLOQUEADO

}
