package br.com.cti.insights;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * POST /api/clientes/validar: JSON valido e invalido passando por DTO + Service + ErroHandler.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class ClienteValidacaoControllerTests {

	private static final String VALIDO = """
			{"codigoCliente":"CTI001","nomeCliente":"Metalúrgica Horizonte","consultor":"Ana Souza","segmento":"IND.",
			 "nivelCliente":"A","faturamentoAnual":1850000,"servicosContratados":"Internet Dedicada;Firewall",
			 "dataContratacao":"2025-01-15","cidade":"Campinas","uf":"SP"}
			""";

	@Autowired
	private MockMvc mockMvc;

	private ResultActions validar(String json) throws Exception {
		return this.mockMvc
			.perform(post("/api/clientes/validar").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	@Test
	void jsonValidoVoltaPadronizado() throws Exception {
		validar("[" + VALIDO + "]").andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].codigoCliente").value("CTI001"))
			.andExpect(jsonPath("$[0].segmento").value("Indústria"))
			.andExpect(jsonPath("$[0].dataContratacao").value("2025-01-15"));
	}

	@Test
	void jsonInvalidoApontaOPrimeiroCampo() throws Exception {
		validar("""
				[{"codigoCliente":"CTI002","nomeCliente":"","consultor":"Carlos Lima","segmento":"Comercio",
				  "nivelCliente":"D","faturamentoAnual":-10,"servicosContratados":"","dataContratacao":"2025-01-30",
				  "cidade":"Hortolândia","uf":"SPP"}]
				""").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("Dados inválidos"))
			.andExpect(jsonPath("$.campo").value("nomeCliente"))
			.andExpect(jsonPath("$.erro").value("O nome do cliente é obrigatório"))
			.andExpect(jsonPath("$.posicao").value(1))
			.andExpect(jsonPath("$.campos['[0].nivelCliente']").value("O nível deve ser A, B ou C"))
			.andExpect(jsonPath("$.campos['[0].faturamentoAnual']").value("O faturamento anual deve ser maior que zero"))
			.andExpect(jsonPath("$.campos['[0].servicosContratados']").value("Os serviços contratados são obrigatórios"))
			.andExpect(jsonPath("$.campos['[0].uf']").value("A UF deve possuir duas letras"));
	}

	@Test
	void erroNaSegundaLinhaInformaAPosicao() throws Exception {
		String semData = VALIDO.replace("CTI001", "CTI009").replace("\"dataContratacao\":\"2025-01-15\",", "");
		validar("[" + VALIDO + "," + semData + "]").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campo").value("dataContratacao"))
			.andExpect(jsonPath("$.erro").value("A data de contratação é obrigatória"))
			.andExpect(jsonPath("$.posicao").value(2));
	}

	@Test
	void codigoRepetidoNoLoteVemDoService() throws Exception {
		String repetido = VALIDO.replace("CTI001", " cti001 ");
		validar("[" + VALIDO + "," + repetido + "]").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("Regra de validação não atendida"))
			.andExpect(jsonPath("$.erro").value("Código repetido na planilha: CTI001"));
	}

	@Test
	void serviceLimpaEPadronizaOsTextos() throws Exception {
		validar("""
				[{"codigoCliente":" cti004 ","nomeCliente":"  indústria ALFA ","consultor":"ANA SOUZA","segmento":"INDUSTRIA",
				  "nivelCliente":"b","faturamentoAnual":3100000,"servicosContratados":"mpls, FIREWALL / nuvem",
				  "dataContratacao":"2025-03-05","cidade":"sumaré","uf":"sp"}]
				""").andExpect(status().isOk())
			.andExpect(jsonPath("$[0].codigoCliente").value("CTI004"))
			.andExpect(jsonPath("$[0].nomeCliente").value("Indústria Alfa"))
			.andExpect(jsonPath("$[0].consultor").value("Ana Souza"))
			.andExpect(jsonPath("$[0].segmento").value("Indústria"))
			.andExpect(jsonPath("$[0].nivelCliente").value("B"))
			.andExpect(jsonPath("$[0].servicosContratados").value("MPLS;Firewall;Cloud"))
			.andExpect(jsonPath("$[0].cidade").value("Sumaré"))
			.andExpect(jsonPath("$[0].uf").value("SP"));
	}

	@Test
	void cidadeEUfSaoOpcionais() throws Exception {
		validar("[" + VALIDO.replace("\"cidade\":\"Campinas\",\"uf\":\"SP\"", "\"cidade\":null,\"uf\":\"\"") + "]")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].cidade").value(""))
			.andExpect(jsonPath("$[0].uf").value(""));
	}

	@Test
	void dataForaDoFormatoDevolveJsonInvalido() throws Exception {
		validar("[" + VALIDO.replace("2025-01-15", "15/01/2025") + "]").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.mensagem").value("JSON inválido"));
	}

	@Test
	void exigeLogin() throws Exception {
		this.mockMvc
			.perform(post("/api/clientes/validar").with(anonymous())
				.contentType(MediaType.APPLICATION_JSON)
				.content("[" + VALIDO + "]"))
			.andExpect(status().isUnauthorized());
	}

}
