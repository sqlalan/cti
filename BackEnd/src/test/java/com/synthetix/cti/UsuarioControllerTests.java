package com.synthetix.cti;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void criaUsuarioSemAutenticacao() throws Exception {
		this.mockMvc
			.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
					{"nome":"Ana","email":"ana@exemplo.com","senha":"segredo123"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").exists())
			.andExpect(jsonPath("$.email").value("ana@exemplo.com"))
			.andExpect(jsonPath("$.senha").doesNotExist());
	}

	@Test
	void rejeitaPayloadInvalido() throws Exception {
		this.mockMvc
			.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
					{"nome":"","email":"nao-e-email","senha":"123"}
					"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void exigeAutenticacaoParaListar() throws Exception {
		this.mockMvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser
	void retorna404ParaUsuarioInexistente() throws Exception {
		this.mockMvc.perform(get("/api/usuarios/99999")).andExpect(status().isNotFound());
	}

}
