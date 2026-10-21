package br.com.cti.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cti.insights.model.EstatisticaLote;
import br.com.cti.insights.model.StatusLote;
import br.com.cti.insights.model.UploadLote;
import br.com.cti.insights.repository.EstatisticaLoteRepository;
import br.com.cti.insights.repository.UploadLinhaRepository;
import br.com.cti.insights.repository.UploadLoteRepository;
import com.jayway.jsonpath.JsonPath;
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
	private UploadLinhaRepository linhas;

	@Autowired
	private EstatisticaLoteRepository estatisticas;

	@BeforeEach
	void limpar() {
		estatisticas.deleteAll();
		linhas.deleteAll();
		lotes.deleteAll();
	}

	private long enviar() throws Exception {
		String resposta = mockMvc
			.perform(post("/api/uploads/validadas").param("arquivo", "dados.xlsx")
				.param("recebidas", "38")
				.contentType(MediaType.APPLICATION_JSON)
				.content(DUAS_LINHAS))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(resposta, "$.id")).longValue();
	}

	@Test
	void enviarValidadasGravaOLoteAguardandoOEtl() throws Exception {
		mockMvc
			.perform(post("/api/uploads/validadas").param("arquivo", "dados.xlsx")
				.param("recebidas", "38")
				.contentType(MediaType.APPLICATION_JSON)
				.content(DUAS_LINHAS))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/dashboard?loteId=")))
			.andExpect(jsonPath("$.status").value("AGUARDANDO_ETL"))
			.andExpect(jsonPath("$.arquivoNome").value("dados.xlsx"))
			.andExpect(jsonPath("$.enviadoPor").value("user"))
			.andExpect(jsonPath("$.totalLinhas").value(38))
			.andExpect(jsonPath("$.linhasValidas").value(2))
			.andExpect(jsonPath("$.linhasInvalidas").value(36));

		// As linhas ficam padronizadas pelo Service da aula 10.
		var gravadas = linhas.findAll();
		assertThat(gravadas).extracting("codigoCliente").containsExactlyInAnyOrder("CTI001", "CTI002");
		assertThat(gravadas).extracting("consultor").containsExactlyInAnyOrder("Ana Souza", "Carlos Lima");
		assertThat(gravadas).extracting("segmento").containsExactlyInAnyOrder("Indústria", "Comércio");
	}

	// Teste de falha do PASSO 12: sem o Python no ar o lote fica com ERRO e a mensagem explica.
	@Test
	void pythonDesligadoDeixaOLoteComErro() throws Exception {
		long id = enviar();

		UploadLote lote = PythonEtlServiceTests.esperarStatus(lotes, id, StatusLote.ERRO);
		assertThat(lote.getMensagem()).startsWith("Não foi possível falar com o serviço Python em");
	}

	@Test
	void codigoRepetidoNaListaDevolve400() throws Exception {
		mockMvc
			.perform(post("/api/uploads/validadas").contentType(MediaType.APPLICATION_JSON)
				.content(DUAS_LINHAS.replace("CTI002", "CTI001")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erro").value("Código repetido na planilha: CTI001"));
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
			.andExpect(jsonPath("$.erro").value("Nenhuma linha válida foi enviada"));
	}

	@Test
	void listarTrazOMaisRecentePrimeiro() throws Exception {
		long primeiro = enviar();
		long segundo = enviar();

		mockMvc.perform(get("/api/uploads"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(segundo))
			.andExpect(jsonPath("$[1].id").value(primeiro));
	}

	@Test
	void dashboardSemLoteDevolve404() throws Exception {
		mockMvc.perform(get("/api/dashboard")).andExpect(status().isNotFound());
	}

	@Test
	void dashboardDevolveOJsonQueOPythonGravou() throws Exception {
		long id = enviar();
		UploadLote lote = lotes.findById(id).orElseThrow();

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
