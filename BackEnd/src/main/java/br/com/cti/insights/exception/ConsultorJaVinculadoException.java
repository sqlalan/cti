package br.com.cti.insights.exception;

public class ConsultorJaVinculadoException extends RuntimeException {

	public ConsultorJaVinculadoException(String consultor) {
		super("Este consultor já tem uma conta: " + consultor);
	}

}
