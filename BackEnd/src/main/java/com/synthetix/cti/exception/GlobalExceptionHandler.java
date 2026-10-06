package com.synthetix.cti.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

	@ExceptionHandler(MatriculaDuplicadaException.class)
	public ProblemDetail matriculaDuplicada(MatriculaDuplicadaException ex) {
		return problema(HttpStatus.CONFLICT, "Matricula duplicada", ex.getMessage());
	}

	@ExceptionHandler(ConsultorJaVinculadoException.class)
	public ProblemDetail consultorJaVinculado(ConsultorJaVinculadoException ex) {
		return problema(HttpStatus.CONFLICT, "Consultor ja vinculado", ex.getMessage());
	}

	@ExceptionHandler(CredenciaisInvalidasException.class)
	public ProblemDetail credenciaisInvalidas(CredenciaisInvalidasException ex) {
		return problema(HttpStatus.UNAUTHORIZED, "Credenciais invalidas", ex.getMessage());
	}

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
		return problema(HttpStatus.NOT_FOUND, "Recurso nao encontrado", ex.getMessage());
	}

	@ExceptionHandler(ProcessamentoPlanilhaException.class)
	public ProblemDetail processamento(ProcessamentoPlanilhaException ex) {
		return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao processar a planilha", ex.getMessage());
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ProblemDetail arquivoGrande(MaxUploadSizeExceededException ex) {
		return problema(HttpStatus.CONTENT_TOO_LARGE, "Arquivo grande demais",
				"A planilha passa do limite de 25 MB aceito pelo servidor");
	}

	@ExceptionHandler({ MissingServletRequestPartException.class, MultipartException.class })
	public ProblemDetail semArquivo(Exception ex) {
		return problema(HttpStatus.BAD_REQUEST, "Arquivo ausente",
				"Envie a planilha como multipart/form-data no campo 'arquivo'");
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
