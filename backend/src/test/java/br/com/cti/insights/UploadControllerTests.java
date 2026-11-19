package br.com.cti.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cti.insights.model.EstatisticaLote;
import br.com.cti.insights.model.Telemetria;
import br.com.cti.insights.model.StatusLote;
import br.com.cti.insights.model.UploadLote;
import br.com.cti.insights.repository.EstatisticaLoteRepository;
import br.com.cti.insights.repository.TelemetriaRepository;
import br.com.cti.insights.repository.UploadLoteRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class UploadControllerTests {

	static final String DUAS_LINHAS = """
			[{"codigoCliente":"cti001","nomeCliente":"Metalúrgica Horizonte","consultor":"ana souza",
			  "segmento":"IND.","nivelCliente":"a","faturamentoAnual":1850000,
			  "servicosContratados":"Internet Dedicada;Firewall","dataContratacao":"2025-01-15",
			  "cidade":"Campinas","uf":"sp"},
			 {"codigoCliente":"CTI002","nomeCliente":"Mercado Central","consultor":"carlos lima",
			  "segmento":"Comercio","nivelCliente":"B","faturamentoAnual":420000,
			  "servicosContratados":"Backup;Firewall","dataContratacao":"2025-01-30","cidade":"","uf":""}]
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UploadLoteRepository lotes;

	@Autowired
	private EstatisticaLoteRepository estatisticas;

	@Autowired
	private TelemetriaRepository telemetria;

	@BeforeEach
	void limpar() {
		telemetria.deleteAll();
		estatisticas.deleteAll();
		lotes.deleteAll();
	}

	// O que o banco.py faz no criar_lote (o Java nao cria lotes).
	private UploadLote loteDoPython(String arquivo) {
		return lotes.save(new UploadLote(arquivo, 38, 2));
	}

	// Teste de falha: sem o Python no ar (porta 1, application.properties de teste) nada e gravado e a
	// tela recebe 503 com uma mensagem sem nome de tecnologia. O motivo tecnico vai para a telemetria.
	@Test
	void pythonDesligadoDevolve503ENaoGravaLote() throws Exception {
		mockMvc
			.perform(post("/api/uploads/validadas").param("arquivo", "dados.xlsx")
				.param("recebidas", "38")
				.contentType(MediaType.APPLICATION_JSON)
				.content(DUAS_LINHAS))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.mensagem")
				.value("O processamento está indisponível no momento. Tente enviar de novo em alguns minutos."));
		assertThat(lotes.count()).isZero();

		assertThat(telemetria.findAll()).anySatisfy(t -> {
			assertThat(t.getEvento()).isEqualTo(Telemetria.EXECUCAO_SCRIPT);
			assertThat(t.isSucesso()).isFalse();
			assertThat(t.getMensagem()).startsWith("Não foi possível falar com o serviço Python em");
		});
		assertThat(telemetria.findAll()).anySatisfy(t -> {
			assertThat(t.getEvento()).isEqualTo(Telemetria.UPLOAD_PLANILHA);
			assertThat(t.isSucesso()).isFalse();
		});
	}

	@Test
	void codigoRepetidoNaListaDevolve400() throws Exception {
		mockMvc
			.perform(post("/api/uploads/validadas").contentType(MediaType.APPLICATION_JSON)
				.content(DUAS_LINHAS.replace("CTI002", "CTI001")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erro").value("Código repetido na planilha: CTI001"));
		assertThat(lotes.count()).isZero();

		// A falha tambem fica registrada, mesmo com o envio desfeito.
		assertThat(telemetria.findAll()).anySatisfy(t -> {
			assertThat(t.getEvento()).isEqualTo(Telemetria.UPLOAD_PLANILHA);
			assertThat(t.isSucesso()).isFalse();
			assertThat(t.getMensagem()).contains("Código repetido");
		});
	}

	@Test
	void formatoDeArquivoNaoAceitoDevolve400() throws Exception {
		mockMvc
			.perform(post("/api/uploads/validadas").param("arquivo", "dados.pdf")
				.contentType(MediaType.APPLICATION_JSON)
				.content(DUAS_LINHAS))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("Formato .pdf não aceito; envie .xlsx, .xls ou .csv"));
		assertThat(lotes.count()).isZero();
	}

	// Campo vazio em todas as linhas = a planilha nao tem a coluna.
	@Test
	void colunaObrigatoriaAusenteDevolve422() throws Exception {
		mockMvc
			.perform(post("/api/uploads/validadas").contentType(MediaType.APPLICATION_JSON)
				.content(DUAS_LINHAS.replace("\"segmento\":\"IND.\",", "").replace("\"segmento\":\"Comercio\",", "")))
			.andExpect(status().is(422))
			.andExpect(jsonPath("$.coluna").value("segmento"))
			.andExpect(jsonPath("$.mensagem").value("Coluna obrigatória ausente na planilha: segmento"));
		assertThat(lotes.count()).isZero();
	}

	@Test
	void linhaInvalidaDevolve400ComAPosicao() throws Exception {
		mockMvc
			.perform(post("/api/uploads/validadas").contentType(MediaType.APPLICATION_JSON)
				.content(DUAS_LINHAS.replace("\"nivelCliente\":\"B\"", "\"nivelCliente\":\"F\"")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.posicao").value(2))
			.andExpect(jsonPath("$.erro").value("O nível deve ser A, B ou C"));
		assertThat(lotes.count()).isZero();
	}

	@Test
	void listaVaziaDevolve400() throws Exception {
		mockMvc.perform(post("/api/uploads/validadas").contentType(MediaType.APPLICATION_JSON).content("[]"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("A planilha não tem linhas válidas para enviar"));
	}

	@Test
	void listarTrazOMaisRecentePrimeiro() throws Exception {
		long primeiro = loteDoPython("a.xlsx").getId();
		long segundo = loteDoPython("b.xlsx").getId();

		mockMvc.perform(get("/api/uploads"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(segundo))
			.andExpect(jsonPath("$[1].id").value(primeiro));
	}

	@Test
	void dashboardSemLoteDevolve404() throws Exception {
		mockMvc.perform(get("/api/dashboard"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.mensagem")
				.value("Nenhuma planilha foi processada ainda. Envie uma planilha para ver os gráficos."));
	}

	@Test
	void dashboardLotesTrazOs20MaisRecentes() throws Exception {
		long ultimo = 0;
		for (int i = 1; i <= 21; i++) {
			ultimo = loteDoPython("planilha" + i + ".xlsx").getId();
		}
		mockMvc.perform(get("/api/dashboard/lotes"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(20))
			.andExpect(jsonPath("$[0].id").value(ultimo))
			.andExpect(jsonPath("$[0].arquivoNome").value("planilha21.xlsx"));
	}

	@Test
	void dashboardDevolveOJsonQueOPythonGravou() throws Exception {
		UploadLote lote = loteDoPython("dados.xlsx");
		long id = lote.getId();

		// Antes do Python terminar: o lote vem, as estatisticas ainda nao.
		mockMvc.perform(get("/api/dashboard").param("loteId", String.valueOf(id)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lote.id").value(id))
			.andExpect(jsonPath("$.estatisticas").doesNotExist());

		// O que o banco.py faria: grava o JSON e marca PROCESSADO.
		EstatisticaLote resultado = new EstatisticaLote();
		resultado.setLote(lote);
		resultado.setGeradoEm(LocalDateTime.of(2026, 10, 21, 19, 10));
		resultado.setResultado("{\"resumo\":{\"totalClientes\":2,\"media\":1135000.0},\"porSegmento\":[]}");
		estatisticas.save(resultado);
		lote.setStatus(StatusLote.PROCESSADO);
		lotes.save(lote);

		// Sem loteId: o ultimo PROCESSADO.
		mockMvc.perform(get("/api/dashboard"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lote.status").value("PROCESSADO"))
			.andExpect(jsonPath("$.geradoEm").value("2026-10-21T19:10:00Z"))
			.andExpect(jsonPath("$.estatisticas.resumo.totalClientes").value(2))
			.andExpect(jsonPath("$.estatisticas.resumo.media").value(1135000.0));
	}

}
