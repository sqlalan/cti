package br.com.cti.insights.exception;

/**
 * Uma linha do lote nao passou nas regras do ClienteDTO. posicao = o item da lista (1 = o primeiro).
 */
public class LinhaInvalidaException extends RuntimeException {

	private final int posicao;

	private final String campo;

	public LinhaInvalidaException(int posicao, String campo, String mensagem) {
		super(mensagem);
		this.posicao = posicao;
		this.campo = campo;
	}

	public int getPosicao() {
		return posicao;
	}

	public String getCampo() {
		return campo;
	}

}
