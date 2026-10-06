package com.synthetix.cti.exception;

public class EmailDuplicadoException extends RuntimeException {

	public EmailDuplicadoException() {
		super("E-mail já cadastrado");
	}

}
