package com.synthetix.cti.exception;

/**
 * O registro pedido (cliente, contrato, usuario...) nao existe.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

	public RecursoNaoEncontradoException(String recurso, Object id) {
		super(recurso + " nao encontrado: " + id);
	}

	public RecursoNaoEncontradoException(String mensagem) {
		super(mensagem);
	}

}
