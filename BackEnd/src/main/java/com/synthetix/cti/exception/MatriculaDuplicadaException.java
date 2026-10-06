package com.synthetix.cti.exception;

public class MatriculaDuplicadaException extends RuntimeException {

	public MatriculaDuplicadaException(String matricula) {
		super("Matricula ja pertence a outro consultor: " + matricula);
	}

}
