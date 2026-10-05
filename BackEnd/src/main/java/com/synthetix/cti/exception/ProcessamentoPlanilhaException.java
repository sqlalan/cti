package com.synthetix.cti.exception;

/**
 * O script Python nao conseguiu processar a planilha por um motivo que nao e culpa do arquivo:
 * Python ausente, tempo esgotado, resposta ilegivel.
 */
public class ProcessamentoPlanilhaException extends RuntimeException {

	public ProcessamentoPlanilhaException(String mensagem) {
		super(mensagem);
	}

	public ProcessamentoPlanilhaException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}

}
