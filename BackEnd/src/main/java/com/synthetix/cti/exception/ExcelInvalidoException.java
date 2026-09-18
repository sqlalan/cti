package com.synthetix.cti.exception;

/**
 * Arquivo enviado nao e um Excel legivel: corrompido, vazio ou fora do formato esperado.
 */
public class ExcelInvalidoException extends RuntimeException {

	public ExcelInvalidoException(String mensagem) {
		super(mensagem);
	}

	public ExcelInvalidoException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}

}
