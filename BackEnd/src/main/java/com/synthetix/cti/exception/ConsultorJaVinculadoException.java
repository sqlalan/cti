package com.synthetix.cti.exception;

public class ConsultorJaVinculadoException extends RuntimeException {

	public ConsultorJaVinculadoException(String consultor) {
		super("Consultor ja possui usuario: " + consultor);
	}

}
