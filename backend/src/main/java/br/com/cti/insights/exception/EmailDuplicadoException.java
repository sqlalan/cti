package br.com.cti.insights.exception;

public class EmailDuplicadoException extends RuntimeException {

	public EmailDuplicadoException() {
		super("E-mail já cadastrado");
	}

}
