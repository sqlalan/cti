package com.synthetix.cti.analise;

import com.synthetix.cti.exception.ColunaObrigatoriaException;
import com.synthetix.cti.exception.ExcelInvalidoException;
import com.synthetix.cti.exception.ProcessamentoPlanilhaException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Roda CienciaD/main.py com ProcessBuilder e le os arquivos que ele grava.
 *
 * Contrato do script: a ultima linha do stdout e um JSON de resumo; codigo de saida 0 = sucesso,
 * 2 = planilha invalida (tipo EXCEL_INVALIDO ou COLUNA_OBRIGATORIA), 1 = falha interna.
 */
@Component
public class AnalisadorPython implements AnalisadorPlanilha {

	private final JsonMapper json;

	private final String comando;

	private final Path script;

	private final Duration tempoLimite;

	public AnalisadorPython(JsonMapper json, @Value("${cti.python.comando:}") String comando,
			@Value("${cti.python.script}") String script, @Value("${cti.python.timeout:120s}") Duration tempoLimite) {
		this.json = json;
		this.comando = StringUtils.hasText(comando) ? comando : comandoPadrao();
		this.script = Path.of(script).toAbsolutePath().normalize();
		this.tempoLimite = tempoLimite;
	}

	// No Windows o "python" costuma ser o atalho da Microsoft Store; o lancador "py" e o confiavel.
	private static String comandoPadrao() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win") ? "py" : "python3";
	}

	@Override
	public ResultadoAnalise analisar(Path planilha, Path pastaSaida) {
		if (!Files.isRegularFile(script)) {
			throw new ProcessamentoPlanilhaException("Script de analise nao encontrado em " + script);
		}
		JsonNode resumo = executar(planilha, pastaSaida);

		if (!resumo.path("sucesso").asBoolean()) {
			String mensagem = resumo.path("mensagem").asString("Falha no processamento da planilha");
			switch (resumo.path("tipo").asString("")) {
				case "COLUNA_OBRIGATORIA" -> throw new ColunaObrigatoriaException(colunas(resumo));
				case "EXCEL_INVALIDO" -> throw new ExcelInvalidoException(mensagem);
				default -> throw new ProcessamentoPlanilhaException(mensagem);
			}
		}

		try {
			return new ResultadoAnalise(resumo.path("linhas_lidas").asInt(), resumo.path("linhas_validas").asInt(),
					resumo.path("linhas_rejeitadas").asInt(), resumo.path("avisos").asInt(),
					json.readValue(pastaSaida.resolve("clientes_tratados.json").toFile(), new TypeReference<>() {
					}), json.readValue(pastaSaida.resolve("insights.json").toFile(), new TypeReference<>() {
					}), ler(pastaSaida.resolve("indicadores.json")), ler(pastaSaida.resolve("problemas.json")));
		}
		catch (JacksonException ex) {
			throw new ProcessamentoPlanilhaException("Resultado da analise em formato inesperado", ex);
		}
	}

	private JsonNode executar(Path planilha, Path pastaSaida) {
		Path stdout = pastaSaida.resolveSibling("python-stdout.txt");
		Path stderr = pastaSaida.resolveSibling("python-stderr.txt");
		List<String> linhaDeComando = List.of(comando, script.toString(), planilha.toAbsolutePath().toString(),
				"--saida", pastaSaida.toAbsolutePath().toString(), "--sem-graficos");

		// Saidas em arquivo, nao em pipe: com pipe cheio o Python trava esperando alguem ler.
		ProcessBuilder construtor = new ProcessBuilder(linhaDeComando).directory(script.getParent().toFile())
			.redirectOutput(stdout.toFile())
			.redirectError(stderr.toFile());
		construtor.environment().put("PYTHONIOENCODING", "utf-8");

		Process processo;
		try {
			processo = construtor.start();
		}
		catch (IOException ex) {
			throw new ProcessamentoPlanilhaException("Nao foi possivel iniciar o Python (comando '" + comando
					+ "'). Defina PYTHON_CMD com o executavel correto.", ex);
		}

		try {
			if (!processo.waitFor(tempoLimite.toMillis(), TimeUnit.MILLISECONDS)) {
				processo.destroyForcibly();
				throw new ProcessamentoPlanilhaException(
						"A analise passou de " + tempoLimite.toSeconds() + " segundos e foi interrompida");
			}
		}
		catch (InterruptedException ex) {
			processo.destroyForcibly();
			Thread.currentThread().interrupt();
			throw new ProcessamentoPlanilhaException("Analise interrompida", ex);
		}

		try {
			JsonNode resumo = json.readTree(ultimaLinha(stdout));
			if (resumo != null && resumo.isObject()) {
				return resumo;
			}
		}
		catch (JacksonException ex) {
			// cai na mensagem abaixo, que traz o stderr do Python
		}
		throw new ProcessamentoPlanilhaException("O script terminou com codigo " + processo.exitValue()
				+ " sem resumo valido. Erro: " + ultimasLinhas(stderr));
	}

	private String colunas(JsonNode resumo) {
		List<String> nomes = new ArrayList<>();
		resumo.path("colunas").forEach(coluna -> nomes.add(coluna.asString()));
		return String.join(", ", nomes);
	}

	private String ler(Path arquivo) {
		try {
			return Files.readString(arquivo, StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new ProcessamentoPlanilhaException("Arquivo de resultado ausente: " + arquivo.getFileName(), ex);
		}
	}

	private String ultimaLinha(Path arquivo) {
		List<String> linhas = linhas(arquivo);
		for (int i = linhas.size() - 1; i >= 0; i--) {
			if (!linhas.get(i).isBlank()) {
				return linhas.get(i);
			}
		}
		return "";
	}

	private String ultimasLinhas(Path arquivo) {
		List<String> linhas = linhas(arquivo);
		return String.join(" | ", linhas.subList(Math.max(0, linhas.size() - 3), linhas.size()));
	}

	private List<String> linhas(Path arquivo) {
		try {
			return Files.readAllLines(arquivo, StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			return List.of();
		}
	}

}
