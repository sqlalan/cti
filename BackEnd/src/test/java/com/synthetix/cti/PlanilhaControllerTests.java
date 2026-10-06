package com.synthetix.cti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synthetix.cti.model.Telemetria;
import com.synthetix.cti.model.TipoEventoTelemetria;
import com.synthetix.cti.repository.ClienteRepository;
import com.synthetix.cti.repository.TelemetriaRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Upload de ponta a ponta: Spring Boot -> Python (CienciaD/main.py) -> banco. Precisa de Python com
 * pandas; sem ele os testes sao pulados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
@EnabledIf("pythonDisponivel")
class PlanilhaControllerTests {

	private static final Path PLANILHA_MODELO = Path.of("../FrontEnd/planilhas-exemplo/CTI_Insights_modelo_upload_aula.xlsx");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ClienteRepository clienteRepository;

	@Autowired
	private TelemetriaRepository telemetriaRepository;

	static boolean pythonDisponivel() {
		String comando = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "py" : "python3";
		try {
			Process processo = new ProcessBuilder(comando, "-c", "import pandas, scipy, openpyxl").start();
			return processo.waitFor(60, TimeUnit.SECONDS) && processo.exitValue() == 0;
		}
		catch (Exception ex) {
			return false;
		}
	}

	private ResultActions enviar(String nome, byte[] conteudo) throws Exception {
		return this.mockMvc.perform(multipart("/api/planilhas").file(new MockMultipartFile("arquivo", nome,
				"application/octet-stream", conteudo)));
	}

	@Test
	void processaPlanilhaModeloEGravaClientesEInsights() throws Exception {
		byte[] planilha = Files.readAllBytes(PLANILHA_MODELO);

		enviar("dados.xlsx", planilha).andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/analises/")))
			.andExpect(jsonPath("$.arquivo").value("dados.xlsx"))
			.andExpect(jsonPath("$.linhasLidas").value(39))
			.andExpect(jsonPath("$.linhasValidas").value(31))
			.andExpect(jsonPath("$.linhasRejeitadas").value(8))
			.andExpect(jsonPath("$.insights[0].categoria").exists())
			.andExpect(jsonPath("$.insights[?(@.categoria == 'SEGMENTO' && @.titulo == 'Segmento Indústria')]").exists());

		// "Cti004" / "Industria" na planilha chegam padronizados.
		this.mockMvc.perform(get("/api/clientes"))
			.andExpect(jsonPath("$[?(@.codigo == 'CTI004')].segmento").value("Indústria"))
			.andExpect(jsonPath("$[?(@.codigo == 'CTI003')].consultor").value("Ana Souza"));
		// Linha recusada pelo Python (nivel "F") nao entra no banco.
		assertThat(clienteRepository.findByCodigo("CTI001")).isEmpty();

		// Reenvio: os mesmos 31 clientes sao atualizados, nenhum duplicado.
		enviar("dados.xlsx", planilha).andExpect(status().isCreated())
			.andExpect(jsonPath("$.clientesCriados").value(0))
			.andExpect(jsonPath("$.clientesAtualizados").value(31));
	}

	@Test
	void dashboardLeOsResultadosDaUltimaPlanilha() throws Exception {
		enviar("dados.xlsx", Files.readAllBytes(PLANILHA_MODELO)).andExpect(status().isCreated());

		this.mockMvc.perform(get("/api/indicadores"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.qualidade.linhas_validas").value(31))
			.andExpect(jsonPath("$.frequencias.nivel.length()").value(3))
			.andExpect(jsonPath("$.testes.normalidade_faturamento.teste").value("Shapiro-Wilk"));
		this.mockMvc.perform(get("/api/insights"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.categoria == 'QUALIDADE')].texto").exists())
			.andExpect(jsonPath("$[?(@.categoria == 'QUALIDADE')].origem").value("ANALISE"))
			.andExpect(jsonPath("$[?(@.titulo == 'Segmento Indústria')].origem").value("RESUMO"));
		this.mockMvc.perform(get("/api/analises/ultima"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.linhasValidas").value(31));
	}

	@Test
	void colunaObrigatoriaAusenteDevolve422ERegistraTelemetria() throws Exception {
		byte[] csv = "codigo;nome;consultor\nCTI900;Cliente;Ana\n".getBytes(StandardCharsets.UTF_8);

		enviar("incompleta.csv", csv).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.title").value("Coluna obrigatoria ausente"))
			.andExpect(jsonPath("$.coluna").value(org.hamcrest.Matchers.containsString("Segmento")));

		List<Telemetria> falhas = telemetriaRepository.findAll()
			.stream()
			.filter(t -> !t.isSucesso() && t.getTipoEvento() == TipoEventoTelemetria.EXECUCAO_SCRIPT)
			.toList();
		assertThat(falhas).isNotEmpty();
	}

	@Test
	void arquivoCorrompidoDevolve400() throws Exception {
		enviar("falso.xlsx", "isto nao e um excel".getBytes(StandardCharsets.UTF_8)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Arquivo Excel invalido"));
	}

	@Test
	void formatoNaoAceitoNemChegaAoPython() throws Exception {
		enviar("notas.txt", "qualquer".getBytes(StandardCharsets.UTF_8)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(".txt")));
	}

	@Test
	void arquivoVazioDevolve400() throws Exception {
		enviar("vazia.xlsx", new byte[0]).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("O arquivo enviado esta vazio"));
	}

	@Test
	void pedidoSemArquivoDevolve400() throws Exception {
		this.mockMvc.perform(multipart("/api/planilhas")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Arquivo ausente"));
	}

}
