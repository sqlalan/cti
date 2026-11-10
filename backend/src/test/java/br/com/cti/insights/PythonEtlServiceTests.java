package br.com.cti.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cti.insights.model.StatusLote;
import br.com.cti.insights.model.Telemetria;
import br.com.cti.insights.model.UploadLote;
import br.com.cti.insights.repository.TelemetriaRepository;
import br.com.cti.insights.repository.UploadLoteRepository;
import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
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
 * Um servidor HTTP falso faz o papel da API do Python (POST /etl) e guarda o que o Spring enviou.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class PythonEtlServiceTests {

	private static final BlockingQueue<Pedido> PEDIDOS = new ArrayBlockingQueue<>(10);

	// Status que o "Python" responde no proximo pedido.
	private static volatile int resposta = 200;

	private static final HttpServer PYTHON = iniciarPython();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UploadLoteRepository lotes;

	@Autowired
	private TelemetriaRepository telemetria;

	record Pedido(String chave, String corpo) {
	}

	private static HttpServer iniciarPython() {
		try {
			HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			servidor.createContext("/etl", troca -> {
				String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
				PEDIDOS.add(new Pedido(troca.getRequestHeaders().getFirst("X-ETL-Key"), corpo));
				byte[] saida = (resposta == 200 ? "{\"ok\":true}" : "{\"detail\":\"Falha no ETL (veja o terminal do Python).\"}")
					.getBytes(StandardCharsets.UTF_8);
				troca.sendResponseHeaders(resposta, saida.length);
				troca.getResponseBody().write(saida);
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

	private long enviar() throws Exception {
		String corpo = mockMvc
			.perform(post("/api/uploads/validadas").param("arquivo", "dados.xlsx")
				.param("recebidas", "38")
				.contentType(MediaType.APPLICATION_JSON)
				.content(UploadControllerTests.DUAS_LINHAS))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(corpo, "$.id")).longValue();
	}

	@Test
	void enviaAsLinhasValidasComAChave() throws Exception {
		PEDIDOS.clear();
		resposta = 200;
		long id = enviar();

		Pedido pedido = PEDIDOS.poll(10, TimeUnit.SECONDS);
		assertThat(pedido).as("o Spring nao chamou o POST /etl").isNotNull();
		assertThat(pedido.chave()).isEqualTo("segredo");

		String json = pedido.corpo();
		assertThat(((Number) JsonPath.read(json, "$.loteId")).longValue()).isEqualTo(id);
		assertThat((Integer) JsonPath.read(json, "$.recebidas")).isEqualTo(38);
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

		// Quem muda o status para PROCESSADO e o Python; com 200 o Spring nao mexe no lote.
		assertThat(lotes.findById(id).orElseThrow().getStatus()).isEqualTo(StatusLote.AGUARDANDO_ETL);

		// Plano PI: a Telemetria registra a execucao do Python.
		esperarTelemetria("Lote " + id + " processado pelo Python", true);
	}

	@Test
	void respostaDeErroDoPythonDeixaOLoteComErro() throws Exception {
		PEDIDOS.clear();
		resposta = 500;
		long id = enviar();

		UploadLote lote = esperarStatus(lotes, id, StatusLote.ERRO);
		assertThat(lote.getMensagem()).startsWith("O serviço Python respondeu 500:");
		esperarTelemetria("Lote " + id + ": O serviço Python respondeu 500", false);
	}

	private void esperarTelemetria(String inicioMensagem, boolean sucesso) throws InterruptedException {
		long limite = System.currentTimeMillis() + 10_000;
		while (System.currentTimeMillis() < limite) {
			boolean achou = telemetria.findAll()
				.stream()
				.anyMatch(t -> t.getEvento().equals(Telemetria.EXECUCAO_SCRIPT) && t.isSucesso() == sucesso
						&& t.getMensagem().startsWith(inicioMensagem));
			if (achou) {
				return;
			}
			Thread.sleep(100);
		}
		throw new AssertionError("Telemetria de EXECUCAO_SCRIPT nao registrada: " + inicioMensagem);
	}

	static UploadLote esperarStatus(UploadLoteRepository lotes, long id, StatusLote esperado)
			throws InterruptedException {
		long limite = System.currentTimeMillis() + 10_000;
		UploadLote lote = lotes.findById(id).orElseThrow();
		while (lote.getStatus() != esperado && System.currentTimeMillis() < limite) {
			Thread.sleep(100);
			lote = lotes.findById(id).orElseThrow();
		}
		assertThat(lote.getStatus()).isEqualTo(esperado);
		return lote;
	}

}
