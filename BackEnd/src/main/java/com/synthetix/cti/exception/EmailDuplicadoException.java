package com.synthetix.cti.exception;

public class EmailDuplicadoException extends RuntimeException {

	public EmailDuplicadoException(String email) {
		super("E-mail ja cadastrado: " + email);
	}

}
