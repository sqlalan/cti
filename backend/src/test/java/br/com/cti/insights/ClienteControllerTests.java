package br.com.cti.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cti.insights.model.Cliente;
import br.com.cti.insights.repository.ClienteRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class ClienteControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ClienteRepository clienteRepository;

	@Autowired
	private TransactionTemplate transacao;

	private static String cliente(String codigo, String nivel, String faturamento) {
		return """
				{"codigo":"%s","nome":"Metalurgica Silva","consultor":"Ana Souza","segmento":"Industria",
				 "nivel":"%s","faturamentoAnual":%s,"cidade":"Joinville","uf":"sc"}
				""".formatted(codigo, nivel, faturamento);
	}

	private long criar(String codigo) throws Exception {
		String corpo = this.mockMvc
			.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
				.content(cliente(codigo, "B", "1850000")))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(corpo, "$.id")).longValue();
	}

	@Test
	void criaClienteECalculaFaixa() throws Exception {
		this.mockMvc
			.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
				.content(cliente("cti-100", "A", "1850000")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.codigo").value("CTI-100"))
			.andExpect(jsonPath("$.uf").value("SC"))
			.andExpect(jsonPath("$.consultor").value("Ana Souza"))
			.andExpect(jsonPath("$.faixaFaturamento").value("PEQUENA"));
	}

	@Test
	void codigoRepetidoDevolve409() throws Exception {
		criar("CTI-200");
		this.mockMvc
			.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
				.content(cliente("cti-200", "A", "10")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.startsWith("Já existe um cliente com o código CTI")))
			.andExpect(jsonPath("$.codigo").value("CTI-200"));
	}

	@Test
	void payloadInvalidoListaOsCampos() throws Exception {
		this.mockMvc
			.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
				.content(cliente("", "A", "-5")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.codigo").exists())
			.andExpect(jsonPath("$.campos.faturamentoAnual").exists());
	}

	@Test
	void substituiEAtualizaNivelParcialmente() throws Exception {
		long id = criar("CTI-300");

		this.mockMvc
			.perform(put("/api/clientes/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(cliente("CTI-300", "C", "500000000")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.faixaFaturamento").value("GRANDE"));

		this.mockMvc
			.perform(patch("/api/clientes/" + id + "/nivel").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nivel\":\"A\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nivel").value("A"))
			.andExpect(jsonPath("$.faturamentoAnual").value(500000000));
	}

	@Test
	void setterDaListaDeContratosFuncionaNoBanco() throws Exception {
		long id = criar("CTI-500");
		this.mockMvc
			.perform(post("/api/clientes/" + id + "/contratos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"servico\":\"MPLS\",\"dataInicio\":\"2025-03-01\"}"))
			.andExpect(status().isCreated());

		// Trocar a lista de um cliente ja salvo nao pode dar erro de orphanRemoval no Hibernate.
		transacao.executeWithoutResult(estado -> {
			Cliente cliente = clienteRepository.findById(id).orElseThrow();
			assertThat(cliente.getContratos()).hasSize(1);
			cliente.setContratos(List.of());
			clienteRepository.flush();
		});

		this.mockMvc.perform(get("/api/clientes/" + id + "/contratos")).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void contrataServicoERemoveContratoECliente() throws Exception {
		long id = criar("CTI-400");

		String corpo = this.mockMvc
			.perform(post("/api/clientes/" + id + "/contratos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"servico\":\"Firewall\",\"dataInicio\":\"2025-01-15\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.servico").value("Firewall"))
			.andExpect(jsonPath("$.categoria").value("Segurança"))
			.andExpect(jsonPath("$.ativo").value(true))
			.andExpect(jsonPath("$.clienteCodigo").value("CTI-400"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		long contratoId = ((Number) JsonPath.read(corpo, "$.id")).longValue();

		this.mockMvc.perform(patch("/api/contratos/" + contratoId + "/cancelamento"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ativo").value(false));
		this.mockMvc.perform(patch("/api/contratos/99999/cancelamento")).andExpect(status().isNotFound());

		this.mockMvc.perform(get("/api/clientes/" + id + "/contratos"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1));

		this.mockMvc.perform(delete("/api/contratos/" + contratoId)).andExpect(status().isNoContent());
		this.mockMvc.perform(get("/api/clientes/" + id + "/contratos")).andExpect(jsonPath("$.length()").value(0));

		this.mockMvc
			.perform(post("/api/clientes/" + id + "/contratos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"servico\":\"Backup\",\"dataInicio\":\"2025-02-01\"}"))
			.andExpect(status().isCreated());

		this.mockMvc.perform(delete("/api/clientes/" + id)).andExpect(status().isNoContent());
		this.mockMvc.perform(get("/api/clientes/" + id)).andExpect(status().isNotFound());
	}

}
