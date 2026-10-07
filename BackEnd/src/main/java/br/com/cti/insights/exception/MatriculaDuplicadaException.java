package br.com.cti.insights.exception;

public class MatriculaDuplicadaException extends RuntimeException {

	public MatriculaDuplicadaException(String matricula) {
		super("Matrícula já pertence a outro consultor: " + matricula);
	}

}
