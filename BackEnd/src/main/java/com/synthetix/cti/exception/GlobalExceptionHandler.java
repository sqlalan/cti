package com.synthetix.cti.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Traduz as excecoes da aplicacao em respostas HTTP para o Front-end. Formato unico, o do guia da
 * disciplina: {"mensagem": "..."}, mais "campos", "coluna" ou "codigo" quando ajudam a tela.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ExcelInvalidoException.class)
	public ResponseEntity<Map<String, Object>> excelInvalido(ExcelInvalidoException ex) {
		return resposta(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(ColunaObrigatoriaException.class)
	public ResponseEntity<Map<String, Object>> colunaObrigatoria(ColunaObrigatoriaException ex) {
		return resposta(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), "coluna", ex.getColuna());
	}

	@ExceptionHandler(ClienteDuplicadoException.class)
	public ResponseEntity<Map<String, Object>> clienteDuplicado(ClienteDuplicadoException ex) {
		return resposta(HttpStatus.CONFLICT, ex.getMessage(), "codigo", ex.getCodigo());
	}

	@ExceptionHandler({ EmailDuplicadoException.class, MatriculaDuplicadaException.class,
			ConsultorJaVinculadoException.class })
	public ResponseEntity<Map<String, Object>> conflito(RuntimeException ex) {
		return resposta(HttpStatus.CONFLICT, ex.getMessage());
	}

	// Dois cadastros iguais ao mesmo tempo passam pela checagem do Service; a constraint unique
	// do banco barra o segundo, e isso e conflito, nao erro do servidor.
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, Object>> violacaoDeUnicidade(DataIntegrityViolationException ex) {
		return resposta(HttpStatus.CONFLICT, "Já existe um cadastro com esses dados");
	}

	@ExceptionHandler(CredenciaisInvalidasException.class)
	public ResponseEntity<Map<String, Object>> credenciaisInvalidas(CredenciaisInvalidasException ex) {
		return resposta(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ResponseEntity<Map<String, Object>> naoEncontrado(RecursoNaoEncontradoException ex) {
		return resposta(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ProcessamentoPlanilhaException.class)
	public ResponseEntity<Map<String, Object>> processamento(ProcessamentoPlanilhaException ex) {
		return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao processar a planilha: " + ex.getMessage());
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, Object>> arquivoGrande(MaxUploadSizeExceededException ex) {
		return resposta(HttpStatus.CONTENT_TOO_LARGE, "A planilha passa do limite de 25 MB aceito pelo servidor");
	}

	@ExceptionHandler({ MissingServletRequestPartException.class, MultipartException.class })
	public ResponseEntity<Map<String, Object>> semArquivo(Exception ex) {
		return resposta(HttpStatus.BAD_REQUEST, "Envie a planilha como multipart/form-data no campo 'arquivo'");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> dadosInvalidos(MethodArgumentNotValidException ex) {
		Map<String, String> campos = new LinkedHashMap<>();
		for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
			campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
		}
		return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos", "campos", campos);
	}

	private static ResponseEntity<Map<String, Object>> resposta(HttpStatus status, String mensagem) {
		return ResponseEntity.status(status).body(Map.of("mensagem", mensagem));
	}

	private static ResponseEntity<Map<String, Object>> resposta(HttpStatus status, String mensagem, String chave,
			Object valor) {
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("mensagem", mensagem);
		corpo.put(chave, valor);
		return ResponseEntity.status(status).body(corpo);
	}

}
