package br.com.cti.insights.exception;

/**
 * O registro pedido (cliente, contrato, usuario...) nao existe.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

	public RecursoNaoEncontradoException(String recurso, Object id) {
		super("Não existe " + recurso + " com id " + id);
	}

	public RecursoNaoEncontradoException(String mensagem) {
		super(mensagem);
	}

}
