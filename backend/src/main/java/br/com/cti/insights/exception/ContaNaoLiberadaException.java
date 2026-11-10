package br.com.cti.insights.exception;

/**
 * Senha certa, mas a conta ainda nao foi aprovada ou foi bloqueada pelo administrador.
 */
public class ContaNaoLiberadaException extends RuntimeException {

	public ContaNaoLiberadaException(String mensagem) {
		super(mensagem);
	}

}
