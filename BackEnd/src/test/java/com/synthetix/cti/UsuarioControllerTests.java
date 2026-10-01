package com.synthetix.cti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.synthetix.cti.model.Consultor;
import com.synthetix.cti.repository.ConsultorRepository;
import com.synthetix.cti.repository.UsuarioRepository;
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

	@Autowired
	private ConsultorRepository consultorRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Test
	void criaUsuarioSemAutenticacao() throws Exception {
		this.mockMvc
			.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
					{"nome":"Ana","email":"ana@exemplo.com","senha":"segredo123","consultor":"Ana Lima"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").exists())
			.andExpect(jsonPath("$.email").value("ana@exemplo.com"))
			.andExpect(jsonPath("$.consultor").value("Ana Lima"))
			.andExpect(jsonPath("$.senha").doesNotExist());
	}

	@Test
	void consultorExistenteEhReaproveitadoPeloNome() throws Exception {
		cadastrar("duda@exemplo.com");
		Consultor consultor = consultorRepository.findByNomeIgnoreCase("duda@exemplo.com").orElseThrow();
		assertThat(usuarioRepository.findByConsultor(consultor)).isPresent();
	}

	@Test
	void recusaSegundoUsuarioParaMesmoConsultor() throws Exception {
		cadastrar("edu@exemplo.com");
		this.mockMvc
			.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
					{"nome":"Outro","email":"outro@exemplo.com","senha":"segredo123","consultor":"EDU@exemplo.com"}
					"""))
			.andExpect(status().isConflict());
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
	void loginComSenhaCorretaDevolveUsuario() throws Exception {
		cadastrar("bia@exemplo.com");
		this.mockMvc
			.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content("""
					{"email":"bia@exemplo.com","senha":"segredo123"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("bia@exemplo.com"))
			.andExpect(jsonPath("$.senha").doesNotExist());
	}

	@Test
	void loginComSenhaErradaDevolve401() throws Exception {
		cadastrar("caio@exemplo.com");
		this.mockMvc
			.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content("""
					{"email":"caio@exemplo.com","senha":"errada123"}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("E-mail ou senha invalidos"));
	}

	@Test
	void loginComEmailInexistenteDevolve401() throws Exception {
		this.mockMvc
			.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content("""
					{"email":"ninguem@exemplo.com","senha":"segredo123"}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("E-mail ou senha invalidos"));
	}

	@Test
	void liberaCorsParaOFrontEnd() throws Exception {
		this.mockMvc
			.perform(options("/api/usuarios/login").header("Origin", "http://localhost:5173")
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "Content-Type"))
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
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

	private void cadastrar(String email) throws Exception {
		this.mockMvc
			.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
					{"nome":"Teste","email":"%s","senha":"segredo123","consultor":"%s"}
					""".formatted(email, email)))
			.andExpect(status().isCreated());
	}

}
