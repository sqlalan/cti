package com.synthetix.cti.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz as excecoes da aplicacao em respostas HTTP claras para o Front-end, no formato
 * padrao ProblemDetail (RFC 9457): status, title e detail.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ExcelInvalidoException.class)
	public ProblemDetail excelInvalido(ExcelInvalidoException ex) {
		return problema(HttpStatus.BAD_REQUEST, "Arquivo Excel invalido", ex.getMessage());
	}

	@ExceptionHandler(ColunaObrigatoriaException.class)
	public ProblemDetail colunaObrigatoria(ColunaObrigatoriaException ex) {
		ProblemDetail problema = problema(HttpStatus.UNPROCESSABLE_CONTENT, "Coluna obrigatoria ausente",
				ex.getMessage());
		problema.setProperty("coluna", ex.getColuna());
		return problema;
	}

	@ExceptionHandler(ClienteDuplicadoException.class)
	public ProblemDetail clienteDuplicado(ClienteDuplicadoException ex) {
		ProblemDetail problema = problema(HttpStatus.CONFLICT, "Cliente duplicado", ex.getMessage());
		problema.setProperty("codigo", ex.getCodigo());
		return problema;
	}

	@ExceptionHandler(EmailDuplicadoException.class)
	public ProblemDetail emailDuplicado(EmailDuplicadoException ex) {
		return problema(HttpStatus.CONFLICT, "E-mail duplicado", ex.getMessage());
	}

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
		return problema(HttpStatus.NOT_FOUND, "Recurso nao encontrado", ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail dadosInvalidos(MethodArgumentNotValidException ex) {
		Map<String, String> campos = new LinkedHashMap<>();
		for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
			campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
		}
		ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados invalidos",
				"Um ou mais campos nao passaram na validacao");
		problema.setProperty("campos", campos);
		return problema;
	}

	private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
		problema.setTitle(titulo);
		return problema;
	}

}
