package br.com.cti.insights.service;

import br.com.cti.insights.dto.ClienteDTO;
import br.com.cti.insights.dto.UploadLoteDTO;
import br.com.cti.insights.exception.ColunaObrigatoriaException;
import br.com.cti.insights.exception.ExcelInvalidoException;
import br.com.cti.insights.exception.LinhaInvalidaException;
import br.com.cti.insights.model.LoteValidadoEvent;
import br.com.cti.insights.model.Telemetria;
import br.com.cti.insights.model.UploadLinha;
import br.com.cti.insights.model.UploadLote;
import br.com.cti.insights.repository.UploadLinhaRepository;
import br.com.cti.insights.repository.UploadLoteRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Recebe as linhas que o Pinia aprovou e revalida como uma planilha: o arquivo, as colunas
 * obrigatorias, cada linha (regras do ClienteDTO) e os codigos repetidos (aula 10). Grava o lote em
 * AGUARDANDO_ETL; depois do commit, o PythonEtlService envia as linhas ao Python.
 */
@Service
public class UploadService {

	static final Set<String> FORMATOS = Set.of(".xlsx", ".xls", ".csv");

	// Coluna da planilha (aba upload_clientes) -> campo do ClienteDTO. Cidade e UF sao opcionais.
	private static final Map<String, Function<ClienteDTO, Object>> OBRIGATORIAS = new LinkedHashMap<>();

	static {
		OBRIGATORIAS.put("codigo_cliente", ClienteDTO::codigoCliente);
		OBRIGATORIAS.put("nome_cliente", ClienteDTO::nomeCliente);
		OBRIGATORIAS.put("consultor", ClienteDTO::consultor);
		OBRIGATORIAS.put("segmento", ClienteDTO::segmento);
		OBRIGATORIAS.put("nivel_cliente", ClienteDTO::nivelCliente);
		OBRIGATORIAS.put("faturamento_anual", ClienteDTO::faturamentoAnual);
		OBRIGATORIAS.put("servicos_contratados", ClienteDTO::servicosContratados);
		OBRIGATORIAS.put("data_contratacao", ClienteDTO::dataContratacao);
	}

	// Ordem dos campos no DTO: o erro mostrado e o do primeiro campo da linha, como no ErroHandler.
	private static final List<String> ORDEM_CAMPOS = Arrays.stream(ClienteDTO.class.getRecordComponents())
		.map(c -> c.getName())
		.toList();

	private final ClienteValidacaoService validacao;

	private final Validator validador;

	private final UploadLoteRepository lotes;

	private final UploadLinhaRepository linhas;

	private final ApplicationEventPublisher eventos;

	private final TelemetriaService telemetria;

	public UploadService(ClienteValidacaoService validacao, Validator validador, UploadLoteRepository lotes,
			UploadLinhaRepository linhas, ApplicationEventPublisher eventos, TelemetriaService telemetria) {
		this.validacao = validacao;
		this.validador = validador;
		this.lotes = lotes;
		this.linhas = linhas;
		this.eventos = eventos;
		this.telemetria = telemetria;
	}

	/**
	 * recebidas = linhas da planilha, boas e ruins; as ruins ficaram no navegador e entram so na conta.
	 */
	@Transactional
	public UploadLoteDTO registrar(String arquivo, Integer recebidas, List<ClienteDTO> clientes, String enviadoPor) {
		long inicio = System.nanoTime();
		String nome = StringUtils.hasText(arquivo) ? StringUtils.getFilename(arquivo.trim()) : "planilha";
		try {
			validarArquivo(nome, clientes);
			validarColunas(clientes);
			validarLinhas(clientes);
			List<ClienteDTO> prontos = validacao.validarEPreparar(clientes);
			int total = recebidas == null ? prontos.size() : Math.max(recebidas, prontos.size());

			UploadLote lote = lotes.save(new UploadLote(nome, enviadoPor, total, prontos.size()));
			List<UploadLinha> novas = new ArrayList<>();
			for (int i = 0; i < prontos.size(); i++) {
				novas.add(linha(lote, i + 1, prontos.get(i)));
			}
			linhas.saveAll(novas);

			eventos.publishEvent(new LoteValidadoEvent(lote.getId()));
			telemetria.registrar(Telemetria.UPLOAD_PLANILHA, true, prontos.size(), inicio,
					nome + ": lote " + lote.getId() + " gravado com " + prontos.size() + " linha(s) válida(s)");
			return UploadLoteDTO.de(lote);
		}
		catch (RuntimeException ex) {
			telemetria.registrar(Telemetria.UPLOAD_PLANILHA, false, null, inicio, nome + ": " + ex.getMessage());
			throw ex;
		}
	}

	@Transactional(readOnly = true)
	public List<UploadLoteDTO> listar() {
		return lotes.findAllByOrderByIdDesc().stream().map(UploadLoteDTO::de).toList();
	}

	private static void validarArquivo(String nome, List<ClienteDTO> clientes) {
		String extensao = StringUtils.getFilenameExtension(nome);
		if (extensao != null) {
			extensao = "." + extensao.toLowerCase(Locale.ROOT);
			if (!FORMATOS.contains(extensao)) {
				throw new ExcelInvalidoException(
						"Formato " + extensao + " não aceito; envie .xlsx, .xls ou .csv");
			}
		}
		if (clientes == null || clientes.isEmpty()) {
			throw new ExcelInvalidoException("A planilha não tem linhas válidas para enviar");
		}
	}

	// Campo vazio em TODAS as linhas = a planilha nao tem a coluna.
	private static void validarColunas(List<ClienteDTO> clientes) {
		String ausentes = OBRIGATORIAS.entrySet()
			.stream()
			.filter(coluna -> clientes.stream().allMatch(c -> vazio(coluna.getValue().apply(c))))
			.map(Map.Entry::getKey)
			.collect(Collectors.joining(", "));
		if (!ausentes.isEmpty()) {
			throw new ColunaObrigatoriaException(ausentes);
		}
	}

	private void validarLinhas(List<ClienteDTO> clientes) {
		for (int i = 0; i < clientes.size(); i++) {
			Set<ConstraintViolation<ClienteDTO>> erros = validador.validate(clientes.get(i));
			if (!erros.isEmpty()) {
				ConstraintViolation<ClienteDTO> primeiro = erros.stream()
					.min(Comparator.comparingInt(e -> ORDEM_CAMPOS.indexOf(e.getPropertyPath().toString())))
					.orElseThrow();
				throw new LinhaInvalidaException(i + 1, primeiro.getPropertyPath().toString(), primeiro.getMessage());
			}
		}
	}

	private static boolean vazio(Object valor) {
		return valor == null || (valor instanceof String texto && !StringUtils.hasText(texto));
	}

	private static UploadLinha linha(UploadLote lote, int numero, ClienteDTO c) {
		UploadLinha linha = new UploadLinha(lote, numero);
		linha.setCodigoCliente(c.codigoCliente());
		linha.setNomeCliente(c.nomeCliente());
		linha.setConsultor(c.consultor());
		linha.setSegmento(c.segmento());
		linha.setNivelCliente(c.nivelCliente());
		linha.setFaturamentoAnual(BigDecimal.valueOf(c.faturamentoAnual()).setScale(2, RoundingMode.HALF_UP));
		linha.setServicosContratados(c.servicosContratados());
		linha.setDataContratacao(c.dataContratacao());
		linha.setCidade(StringUtils.hasText(c.cidade()) ? c.cidade() : null);
		linha.setUf(StringUtils.hasText(c.uf()) ? c.uf() : null);
		return linha;
	}

}
