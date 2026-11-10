package br.com.cti.insights.exception;

/**
 * Ja existe um cliente com o mesmo codigo CTI.
 */
public class ClienteDuplicadoException extends RuntimeException {

	private final String codigo;

	public ClienteDuplicadoException(String codigo) {
		super("Já existe um cliente com o código CTI " + codigo);
		this.codigo = codigo;
	}

	public String getCodigo() {
		return codigo;
	}

}
