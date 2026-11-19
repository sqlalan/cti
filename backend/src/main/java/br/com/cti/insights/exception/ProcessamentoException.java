package br.com.cti.insights.exception;

import org.springframework.http.HttpStatus;

/**
 * O serviço de processamento (Python) não respondeu ou falhou. A mensagem é para quem usa a tela;
 * o motivo técnico fica no log e na telemetria.
 */
public class ProcessamentoException extends RuntimeException {

	private final HttpStatus status;

	public ProcessamentoException(HttpStatus status, String mensagem) {
		super(mensagem);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}

}
