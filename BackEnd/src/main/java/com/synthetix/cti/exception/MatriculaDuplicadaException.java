package com.synthetix.cti.exception;

public class MatriculaDuplicadaException extends RuntimeException {

	public MatriculaDuplicadaException(String matricula) {
		super("Matrícula já pertence a outro consultor: " + matricula);
	}

}
