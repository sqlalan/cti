package br.com.cti.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cti.insights.model.StatusLote;
import br.com.cti.insights.model.UploadLote;
import br.com.cti.insights.repository.UploadLoteRepository;
import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Um servidor HTTP falso faz o papel da API do Python (POST /etl, aula 12): guarda o que o Spring
 * enviou e, como o banco.py, cria o lote e devolve {"ok": true, "loteId": ...}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class PythonEtlServiceTests {

	private static final BlockingQueue<Pedido> PEDIDOS = new ArrayBlockingQueue<>(10);

	// Status que o "Python" responde no proximo pedido.
	private static volatile int resposta = 200;

	// Repositorio que o "Python" usa para criar o lote (preenchido antes de cada teste).
	private static volatile UploadLoteRepository banco;

	private static final HttpServer PYTHON = iniciarPython();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UploadLoteRepository lotes;

	record Pedido(String chave, String corpo) {
	}

	private static HttpServer iniciarPython() {
		try {
			HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			servidor.createContext("/etl", troca -> {
				String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
				PEDIDOS.add(new Pedido(troca.getRequestHeaders().getFirst("X-ETL-Key"), corpo));
				String saida;
				if (resposta == 200) {
					UploadLote lote = new UploadLote(JsonPath.read(corpo, "$.arquivoNome"),
							JsonPath.<Integer>read(corpo, "$.recebidas"), JsonPath.<Integer>read(corpo, "$.linhas.length()"));
					lote.setStatus(StatusLote.PROCESSADO);
					lote.setMensagem("ETL concluído: 2 clientes tratados.");
					saida = "{\"ok\":true,\"loteId\":" + banco.save(lote).getId() + "}";
				}
				else {
					saida = "{\"detail\":\"Falha no ETL: coluna inesperada\"}";
				}
				byte[] bytes = saida.getBytes(StandardCharsets.UTF_8);
				troca.sendResponseHeaders(resposta, bytes.length);
				troca.getResponseBody().write(bytes);
				troca.close();
			});
			servidor.start();
			return servidor;
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@DynamicPropertySource
	static void apontarParaOPythonFalso(DynamicPropertyRegistry registro) {
		registro.add("app.etl.url", () -> "http://localhost:" + PYTHON.getAddress().getPort() + "/etl");
		registro.add("app.etl.key", () -> "segredo");
	}

	@AfterAll
	static void desligarPython() {
		PYTHON.stop(0);
	}

	@BeforeEach
	void preparar() {
		banco = lotes;
		PEDIDOS.clear();
	}

	@Test
	void enviaAsLinhasValidasComAChaveEDevolveOLoteDoPython() throws Exception {
		resposta = 200;
		String corpo = mockMvc
			.perform(post("/api/uploads/validadas").param("arquivo", "dados.xlsx")
				.param("recebidas", "38")
				.param("comAviso", "1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(UploadControllerTests.DUAS_LINHAS))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/dashboard?loteId=")))
			.andExpect(jsonPath("$.status").value("PROCESSADO"))
			.andExpect(jsonPath("$.arquivoNome").value("dados.xlsx"))
			.andExpect(jsonPath("$.totalLinhas").value(38))
			.andExpect(jsonPath("$.linhasValidas").value(2))
			.andExpect(jsonPath("$.linhasInvalidas").value(36))
			.andReturn()
			.getResponse()
			.getContentAsString();
		long id = ((Number) JsonPath.read(corpo, "$.id")).longValue();
		assertThat(lotes.findById(id)).isPresent();

		Pedido pedido = PEDIDOS.poll();
		assertThat(pedido).as("o Spring nao chamou o POST /etl").isNotNull();
		assertThat(pedido.chave()).isEqualTo("segredo");

		// Os campos do Pedido da aula 12, com as linhas padronizadas pelo Service da aula 10.
		String json = pedido.corpo();
		assertThat((String) JsonPath.read(json, "$.arquivoNome")).isEqualTo("dados.xlsx");
		assertThat((Integer) JsonPath.read(json, "$.recebidas")).isEqualTo(38);
		assertThat((Integer) JsonPath.read(json, "$.invalidas")).isEqualTo(36);
		assertThat((Integer) JsonPath.read(json, "$.comAviso")).isEqualTo(1);
		assertThat((Integer) JsonPath.read(json, "$.linhas.length()")).isEqualTo(2);
		assertThat((String) JsonPath.read(json, "$.linhas[0].codigo_cliente")).isEqualTo("CTI001");
		assertThat((String) JsonPath.read(json, "$.linhas[0].consultor")).isEqualTo("Ana Souza");
		assertThat((String) JsonPath.read(json, "$.linhas[0].segmento")).isEqualTo("Indústria");
		assertThat((String) JsonPath.read(json, "$.linhas[0].nivel_cliente")).isEqualTo("A");
		assertThat(((Number) JsonPath.read(json, "$.linhas[0].faturamento_anual")).doubleValue()).isEqualTo(1850000.0);
		assertThat((String) JsonPath.read(json, "$.linhas[0].servicos_contratados"))
			.isEqualTo("Internet Dedicada;Firewall");
		assertThat((String) JsonPath.read(json, "$.linhas[0].data_contratacao")).isEqualTo("2025-01-15");
		assertThat((Object) JsonPath.read(json, "$.linhas[1].cidade")).isNull();
	}

	@Test
	void respostaDeErroDoPythonDevolve502ComMensagemParaATela() throws Exception {
		resposta = 500;
		mockMvc
			.perform(post("/api/uploads/validadas").param("arquivo", "dados.xlsx")
				.param("recebidas", "38")
				.contentType(MediaType.APPLICATION_JSON)
				.content(UploadControllerTests.DUAS_LINHAS))
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.mensagem").value(
					"Não foi possível processar esta planilha. Tente enviar de novo em alguns minutos; se continuar, avise o suporte."));
	}

}
