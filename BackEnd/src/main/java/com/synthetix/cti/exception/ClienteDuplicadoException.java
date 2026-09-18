package com.synthetix.cti.exception;

/**
 * Ja existe um cliente com o mesmo codigo CTI.
 */
public class ClienteDuplicadoException extends RuntimeException {

	private final String codigo;

	public ClienteDuplicadoException(String codigo) {
		super("Ja existe um cliente com o codigo CTI " + codigo);
		this.codigo = codigo;
	}

	public String getCodigo() {
		return codigo;
	}

}
