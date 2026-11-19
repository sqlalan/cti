package br.com.cti.insights.controller;

import br.com.cti.insights.exception.ClienteDuplicadoException;
import br.com.cti.insights.exception.ColunaObrigatoriaException;
import br.com.cti.insights.exception.ConsultorJaVinculadoException;
import br.com.cti.insights.exception.ContaNaoLiberadaException;
import br.com.cti.insights.exception.CredenciaisInvalidasException;
import br.com.cti.insights.exception.EmailDuplicadoException;
import br.com.cti.insights.exception.ExcelInvalidoException;
import br.com.cti.insights.exception.LinhaInvalidaException;
import br.com.cti.insights.exception.MatriculaDuplicadaException;
import br.com.cti.insights.exception.ProcessamentoException;
import br.com.cti.insights.exception.RecursoNaoEncontradoException;
import br.com.cti.insights.exception.TentativasExcedidasException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Transforma os erros da API em JSON que a tela consegue mostrar, sem detalhe tecnico.
 *
 * Validacao (@Valid): {"mensagem":"Dados inválidos","campo":...,"erro":...} com o primeiro erro,
 * mais "campos" com todos. Regra do Service: {"mensagem":"Regra de validação não atendida","erro":...}.
 * Os demais: {"mensagem":...}.
 */
@RestControllerAdvice
public class ErroHandler {

	// Um objeto no corpo (@Valid @RequestBody ClienteRequest).
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> tratarErroDto(MethodArgumentNotValidException e) {
		List<ErroDeCampo> erros = new ArrayList<>();
		Object alvo = e.getBindingResult().getTarget();
		ordenar(e.getBindingResult().getFieldErrors(), alvo)
			.forEach(erro -> erros.add(new ErroDeCampo(null, erro.getField(), erro.getDefaultMessage())));
		return dadosInvalidos(erros);
	}

	// Uma lista no corpo (List<@Valid ClienteDTO>): o Spring avisa por esta outra excecao.
	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<Map<String, Object>> tratarErroDaLista(HandlerMethodValidationException e) {
		List<ErroDeCampo> erros = new ArrayList<>();
		e.getParameterValidationResults().forEach(resultado -> {
			if (resultado instanceof ParameterErrors errosDoItem) {
				ordenar(errosDoItem.getFieldErrors(), errosDoItem.getArgument()).forEach(erro -> erros
					.add(new ErroDeCampo(errosDoItem.getContainerIndex(), erro.getField(), erro.getDefaultMessage())));
			}
		});
		erros.sort(Comparator.comparing(erro -> erro.indice() == null ? -1 : erro.indice()));
		return dadosInvalidos(erros);
	}

	// Regra que o Service checa comparando as linhas (codigo repetido na planilha).
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> tratarRegraNegocio(IllegalArgumentException e) {
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("mensagem", "Regra de validação não atendida");
		corpo.put("erro", e.getMessage());
		return ResponseEntity.badRequest().body(corpo);
	}

	// JSON quebrado, data fora de aaaa-mm-dd, texto onde era numero.
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException e) {
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("mensagem", "JSON inválido");
		corpo.put("erro", "Confira o formato dos campos: datas em aaaa-mm-dd e números sem aspas");
		return ResponseEntity.badRequest().body(corpo);
	}

	// Linha do lote recusada pelo UploadService: mesmo formato da validacao da lista, com a posicao.
	@ExceptionHandler(LinhaInvalidaException.class)
	public ResponseEntity<Map<String, Object>> linhaInvalida(LinhaInvalidaException ex) {
		return dadosInvalidos(List.of(new ErroDeCampo(ex.getPosicao() - 1, ex.getCampo(), ex.getMessage())));
	}

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

	// Senha certa, conta pendente ou bloqueada: 403 com o motivo, para a tela explicar.
	@ExceptionHandler(ContaNaoLiberadaException.class)
	public ResponseEntity<Map<String, Object>> contaNaoLiberada(ContaNaoLiberadaException ex) {
		return resposta(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	// Senhas erradas demais seguidas: 429 ate o bloqueio vencer.
	@ExceptionHandler(TentativasExcedidasException.class)
	public ResponseEntity<Map<String, Object>> tentativasExcedidas(TentativasExcedidasException ex) {
		return resposta(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
	}

	@ExceptionHandler(CredenciaisInvalidasException.class)
	public ResponseEntity<Map<String, Object>> credenciaisInvalidas(CredenciaisInvalidasException ex) {
		return resposta(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	// O Python nao respondeu (503) ou falhou (502): a planilha continua na tela para enviar de novo.
	@ExceptionHandler(ProcessamentoException.class)
	public ResponseEntity<Map<String, Object>> processamento(ProcessamentoException ex) {
		return resposta(ex.getStatus(), ex.getMessage());
	}

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ResponseEntity<Map<String, Object>> naoEncontrado(RecursoNaoEncontradoException ex) {
		return resposta(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	/**
	 * "campo" e "erro" = o primeiro problema; "posicao" = o item da lista (1 = o primeiro), quando o
	 * corpo era uma lista; "campos" = todos, com o indice na frente no caso da lista ("[0].nomeCliente").
	 */
	private static ResponseEntity<Map<String, Object>> dadosInvalidos(List<ErroDeCampo> erros) {
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("mensagem", "Dados inválidos");
		if (!erros.isEmpty()) {
			ErroDeCampo primeiro = erros.get(0);
			corpo.put("campo", primeiro.campo());
			corpo.put("erro", primeiro.mensagem());
			if (primeiro.indice() != null) {
				corpo.put("posicao", primeiro.indice() + 1);
			}
		}
		Map<String, String> campos = new LinkedHashMap<>();
		erros.forEach(erro -> campos.putIfAbsent(erro.chave(), erro.mensagem()));
		corpo.put("campos", campos);
		return ResponseEntity.badRequest().body(corpo);
	}

	// O Spring entrega os erros em ordem qualquer; aqui eles seguem a ordem dos campos no DTO.
	private static List<FieldError> ordenar(List<FieldError> erros, Object alvo) {
		List<String> ordem = alvo == null ? List.of()
				: Arrays.stream(alvo.getClass().getDeclaredFields()).map(Field::getName).toList();
		return erros.stream().sorted(Comparator.comparingInt(erro -> {
			int posicao = ordem.indexOf(erro.getField());
			return posicao < 0 ? Integer.MAX_VALUE : posicao;
		})).toList();
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

	private record ErroDeCampo(Integer indice, String campo, String mensagem) {

		String chave() {
			return indice == null ? campo : "[" + indice + "]." + campo;
		}

	}

}
