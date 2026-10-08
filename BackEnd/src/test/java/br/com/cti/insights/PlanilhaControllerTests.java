package br.com.cti.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cti.insights.model.Servico;
import br.com.cti.insights.model.Telemetria;
import br.com.cti.insights.repository.ClienteRepository;
import br.com.cti.insights.repository.ServicoRepository;
import br.com.cti.insights.repository.TelemetriaRepository;
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
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Upload de ponta a ponta: Spring Boot -> Python (CienciaD/main.py) -> banco. Precisa de Python com
 * pandas; sem ele os testes sao pulados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
@EnabledIf("pythonDisponivel")
class PlanilhaControllerTests {

	private static final Path PLANILHA_MODELO = Path.of("../FrontEnd/planilhas-exemplo/dados.xlsx");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ClienteRepository clienteRepository;

	@Autowired
	private TelemetriaRepository telemetriaRepository;

	@Autowired
	private ServicoRepository servicoRepository;

	@Autowired
	private TransactionTemplate transacao;

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
			.andExpect(jsonPath("$.insights[0].tipo").exists())
			.andExpect(jsonPath("$.insights[?(@.tipo == 'SEGMENTO' && @.titulo == 'Segmento Indústria')]").exists());

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
	void importacaoGravaOsRelacionamentosDoModel() throws Exception {
		enviar("dados.xlsx", Files.readAllBytes(PLANILHA_MODELO)).andExpect(status().isCreated());

		transacao.executeWithoutResult(estado -> {
			// 1 Servico -- N Contratos, lido pelo lado do servico.
			Servico firewall = servicoRepository.findByNomeIgnoreCase("Firewall").orElseThrow();
			assertThat(firewall.getCategoria()).isEqualTo("Segurança");
			assertThat(firewall.getContratos()).isNotEmpty().allSatisfy(contrato -> {
				assertThat(contrato.getAtivo()).isTrue();
				assertThat(contrato.getDataInicio()).isNotNull();
			});
		});

		// Insight com tipo e descricao gravados; Telemetria com evento, dataHora e mensagem.
		this.mockMvc.perform(get("/api/insights"))
			.andExpect(jsonPath("$[?(@.titulo == 'Segmento Indústria')].descricao")
				.value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.startsWith("Segmento Indústria:"))));
		assertThat(telemetriaRepository.findAll()).anySatisfy(t -> {
			assertThat(t.getEvento()).isEqualTo(Telemetria.UPLOAD_PLANILHA);
			assertThat(t.getDataHora()).isNotNull();
			assertThat(t.getMensagem()).isNotBlank();
		});
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
			.andExpect(jsonPath("$[?(@.tipo == 'QUALIDADE')].descricao").exists())
			.andExpect(jsonPath("$[?(@.tipo == 'QUALIDADE')].origem").value("ANALISE"))
			.andExpect(jsonPath("$[?(@.titulo == 'Segmento Indústria')].origem").value("RESUMO"));
		this.mockMvc.perform(get("/api/analises/ultima"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.linhasValidas").value(31));
	}

	@Test
	void indicadoresEProblemasDeCadaEnvioFicamGuardados() throws Exception {
		byte[] planilha = Files.readAllBytes(PLANILHA_MODELO);
		String primeiro = enviar("dados.xlsx", planilha).andReturn().getResponse().getHeader("Location");
		enviar("dados.xlsx", planilha).andExpect(status().isCreated());

		// O envio antigo continua consultavel depois do novo.
		this.mockMvc.perform(get(primeiro + "/indicadores"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.qualidade.linhas_lidas").value(39))
			.andExpect(jsonPath("$.faturamento.geral.soma").isNumber());
		this.mockMvc.perform(get(primeiro + "/problemas"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.erros.length()").value(8));
		this.mockMvc.perform(get("/api/analises/999999/indicadores")).andExpect(status().isNotFound());
	}

	@Test
	void colunaObrigatoriaAusenteDevolve422ERegistraTelemetria() throws Exception {
		byte[] csv = "codigo;nome;consultor\nCTI900;Cliente;Ana\n".getBytes(StandardCharsets.UTF_8);

		enviar("incompleta.csv", csv).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.startsWith("Coluna obrigatória ausente")))
			.andExpect(jsonPath("$.coluna").value(org.hamcrest.Matchers.containsString("Segmento")));

		List<Telemetria> falhas = telemetriaRepository.findAll()
			.stream()
			.filter(t -> !t.isSucesso() && Telemetria.EXECUCAO_SCRIPT.equals(t.getEvento()))
			.toList();
		assertThat(falhas).isNotEmpty();
	}

	@Test
	void arquivoCorrompidoDevolve400() throws Exception {
		enviar("falso.xlsx", "isto nao e um excel".getBytes(StandardCharsets.UTF_8)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").exists());
	}

	@Test
	void formatoNaoAceitoNemChegaAoPython() throws Exception {
		enviar("notas.txt", "qualquer".getBytes(StandardCharsets.UTF_8)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString(".txt")));
	}

	@Test
	void arquivoVazioDevolve400() throws Exception {
		enviar("vazia.xlsx", new byte[0]).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("O arquivo enviado está vazio"));
	}

	@Test
	void pedidoSemArquivoDevolve400() throws Exception {
		this.mockMvc.perform(multipart("/api/planilhas")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("campo 'arquivo'")));
	}

}
