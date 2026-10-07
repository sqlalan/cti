package br.com.cti.insights.service;

import br.com.cti.insights.analise.AnalisadorPlanilha;
import br.com.cti.insights.analise.ResultadoAnalise;
import br.com.cti.insights.exception.ExcelInvalidoException;
import br.com.cti.insights.exception.ProcessamentoPlanilhaException;
import br.com.cti.insights.model.Analise;
import br.com.cti.insights.model.Telemetria;
import br.com.cti.insights.repository.TelemetriaRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload da planilha: valida o arquivo, chama o Python e grava o resultado.
 *
 * Sem @Transactional aqui de proposito: a telemetria de uma falha precisa ficar gravada mesmo
 * quando a importacao e desfeita.
 */
@Service
public class PlanilhaService {

	static final Set<String> FORMATOS = Set.of(".xlsx", ".xls", ".csv");

	private final AnalisadorPlanilha analisador;

	private final ImportacaoService importacao;

	private final TelemetriaRepository telemetria;

	public PlanilhaService(AnalisadorPlanilha analisador, ImportacaoService importacao,
			TelemetriaRepository telemetria) {
		this.analisador = analisador;
		this.importacao = importacao;
		this.telemetria = telemetria;
	}

	public Analise processar(MultipartFile arquivo) {
		long inicio = System.nanoTime();
		String nome = nomeOriginal(arquivo);
		try {
			String extensao = validar(arquivo, nome);
			Analise analise = analisarEImportar(arquivo, nome, extensao);
			registrar(Telemetria.UPLOAD_PLANILHA, true, analise.getLinhasValidas(), inicio,
					nome + ": " + analise.getClientesCriados() + " cliente(s) novo(s), "
							+ analise.getClientesAtualizados() + " atualizado(s)");
			return analise;
		}
		catch (RuntimeException ex) {
			registrar(Telemetria.UPLOAD_PLANILHA, false, null, inicio, nome + ": " + ex.getMessage());
			throw ex;
		}
	}

	private String validar(MultipartFile arquivo, String nome) {
		if (arquivo == null || arquivo.isEmpty()) {
			throw new ExcelInvalidoException("O arquivo enviado está vazio");
		}
		String extensao = StringUtils.getFilenameExtension(nome);
		extensao = extensao == null ? "" : "." + extensao.toLowerCase(Locale.ROOT);
		if (!FORMATOS.contains(extensao)) {
			throw new ExcelInvalidoException(
					"Formato " + (extensao.isEmpty() ? "desconhecido" : extensao) + " não aceito; envie .xlsx, .xls ou .csv");
		}
		return extensao;
	}

	private Analise analisarEImportar(MultipartFile arquivo, String nome, String extensao) {
		Path pasta = criarPastaTemporaria();
		try {
			// Nome fixo no disco: o nome enviado pelo usuario nunca vira caminho.
			Path planilha = pasta.resolve("planilha" + extensao);
			arquivo.transferTo(planilha);
			ResultadoAnalise resultado = executarScript(planilha, pasta.resolve("saida"));
			return importacao.importar(nome, resultado);
		}
		catch (IOException ex) {
			throw new ProcessamentoPlanilhaException("Não foi possível salvar o arquivo recebido", ex);
		}
		finally {
			apagar(pasta);
		}
	}

	private ResultadoAnalise executarScript(Path planilha, Path saida) {
		long inicio = System.nanoTime();
		try {
			ResultadoAnalise resultado = analisador.analisar(planilha, saida);
			registrar(Telemetria.EXECUCAO_SCRIPT, true, resultado.linhasLidas(), inicio,
					resultado.linhasValidas() + " linha(s) valida(s), " + resultado.linhasRejeitadas() + " recusada(s)");
			return resultado;
		}
		catch (RuntimeException ex) {
			registrar(Telemetria.EXECUCAO_SCRIPT, false, null, inicio, ex.getMessage());
			throw ex;
		}
	}

	private void registrar(String evento, boolean sucesso, Integer registros, long inicio, String mensagem) {
		long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;
		String texto = mensagem == null ? null : mensagem.substring(0, Math.min(mensagem.length(), 500));
		telemetria.save(new Telemetria(evento, sucesso, registros, duracaoMs, texto));
	}

	private static String nomeOriginal(MultipartFile arquivo) {
		String nome = arquivo == null ? null : StringUtils.getFilename(arquivo.getOriginalFilename());
		return StringUtils.hasText(nome) ? nome : "planilha";
	}

	private static Path criarPastaTemporaria() {
		try {
			return Files.createTempDirectory("cti-planilha-");
		}
		catch (IOException ex) {
			throw new ProcessamentoPlanilhaException("Não foi possível criar a pasta de trabalho", ex);
		}
	}

	private static void apagar(Path pasta) {
		try (Stream<Path> caminhos = Files.walk(pasta)) {
			caminhos.sorted(Comparator.reverseOrder()).forEach(caminho -> caminho.toFile().delete());
		}
		catch (IOException ex) {
			// pasta temporaria: o sistema operacional limpa depois
		}
	}

}
