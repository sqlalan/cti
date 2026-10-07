package br.com.cti.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import br.com.cti.insights.model.Consultor;
import br.com.cti.insights.model.Perfil;
import br.com.cti.insights.model.Usuario;
import br.com.cti.insights.repository.ConsultorRepository;
import br.com.cti.insights.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
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

	@Autowired
	private JwtAuthenticationConverter jwtAuthenticationConverter;

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
	void senhaPrecisaDeLetrasENumerosEDe8A72Caracteres() throws Exception {
		for (String senha : List.of("abcdefgh", "12345678", "a1", "a1".repeat(37))) {
			this.mockMvc
				.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
						{"nome":"Beto","email":"beto@exemplo.com","senha":"%s","consultor":"Beto Reis"}
						""".formatted(senha)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("Dados inválidos"))
				.andExpect(jsonPath("$.campos.senha").value("A senha deve ter de 8 a 72 caracteres, com letras e números"));
		}
		assertThat(usuarioRepository.findByEmailIgnoreCase("beto@exemplo.com")).isEmpty();
	}

	@Test
	void senhaAcentuadaQuePassaDe72BytesDevolve400ENao500() throws Exception {
		// 42 caracteres, mas 82 bytes: o BCrypt recusaria com erro.
		String senha = "a1" + "á".repeat(40);
		this.mockMvc
			.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
					{"nome":"Caio","email":"caio.acento@exemplo.com","senha":"%s","consultor":"Caio Acento"}
					""".formatted(senha)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.senha").value(org.hamcrest.Matchers.containsString("BCrypt")));
	}

	@Test
	void emailEhGravadoEmMinusculasEMaiusculasNaoCriamOutraConta() throws Exception {
		this.mockMvc
			.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
					{"nome":"Joana","email":"Joana@Exemplo.COM","senha":"segredo123","consultor":"Joana Prado"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value("joana@exemplo.com"));

		this.mockMvc
			.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
					{"nome":"Joana 2","email":"JOANA@exemplo.com","senha":"segredo123","consultor":"Joana Dois"}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado"));

		this.mockMvc
			.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content("""
					{"email":"JOANA@EXEMPLO.COM","senha":"segredo123"}
					"""))
			.andExpect(status().isOk());
	}

	@Test
	void senhaVaiParaOBancoComoBcryptPuro() throws Exception {
		cadastrar("kiko@exemplo.com");
		String hash = usuarioRepository.findByEmailIgnoreCase("kiko@exemplo.com").orElseThrow().getSenha();
		// O que o GUIA 3 confere no DBeaver: comeca com $2a$10$ e tem 60 caracteres.
		assertThat(hash).startsWith("$2a$10$").hasSize(60);
	}

	@Test
	void hashAntigoComPrefixoContinuaEntrando() throws Exception {
		Usuario antigo = new Usuario();
		antigo.setNome("Lia");
		antigo.setEmail("lia@exemplo.com");
		antigo.setSenha(PasswordEncoderFactories.createDelegatingPasswordEncoder().encode("segredo123"));
		usuarioRepository.save(antigo);
		assertThat(antigo.getSenha()).startsWith("{bcrypt}");

		this.mockMvc
			.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content("""
					{"email":"lia@exemplo.com","senha":"segredo123"}
					"""))
			.andExpect(status().isOk());
	}

	@Test
	void cadastroNasceComPerfilUser() throws Exception {
		this.mockMvc
			.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
					{"nome":"Nina","email":"nina@exemplo.com","senha":"segredo123","consultor":"Nina Rocha"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.perfil").value("USER"));
	}

	@Test
	void adminDefinidoNoBancoRecebeOPerfilNoTokenENoMe() throws Exception {
		cadastrar("otto@exemplo.com");
		Usuario otto = usuarioRepository.findByEmailIgnoreCase("otto@exemplo.com").orElseThrow();
		// O GUIA promove pelo DBeaver: UPDATE usuarios SET perfil = 'ADMIN' WHERE email = '...'
		otto.setPerfil(Perfil.ADMIN);
		usuarioRepository.save(otto);

		String token = JsonPath.read(this.mockMvc
			.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content("""
					{"email":"otto@exemplo.com","senha":"segredo123"}
					"""))
			.andExpect(jsonPath("$.usuario.perfil").value("ADMIN"))
			.andReturn()
			.getResponse()
			.getContentAsString(), "$.token");

		String corpoDoToken = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
		assertThat(corpoDoToken).contains("\"perfil\":\"ADMIN\"");
		this.mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + token))
			.andExpect(jsonPath("$.perfil").value("ADMIN"));
	}

	@Test
	void perfilDoTokenViraRoleNoSpringSecurity() {
		Jwt token = Jwt.withTokenValue("x").header("alg", "HS256").subject("a@a.com").claim("perfil", "ADMIN").build();
		// O Spring Security 7 acrescenta FACTOR_BEARER (como a pessoa entrou); aqui interessa so o perfil.
		assertThat(jwtAuthenticationConverter.convert(token).getAuthorities())
			.extracting(GrantedAuthority::getAuthority)
			.filteredOn(autoridade -> autoridade.startsWith("ROLE_"))
			.containsExactly("ROLE_ADMIN");
	}

	@Test
	void meDevolveQuemEstaLogadoPeloToken() throws Exception {
		cadastrar("mel@exemplo.com");
		String token = JsonPath.read(this.mockMvc
			.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content("""
					{"email":"mel@exemplo.com","senha":"segredo123"}
					"""))
			.andReturn()
			.getResponse()
			.getContentAsString(), "$.token");

		this.mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("mel@exemplo.com"))
			.andExpect(jsonPath("$.senha").doesNotExist());
		this.mockMvc.perform(get("/api/usuarios/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void mensagensDeValidacaoSaemEmPortuguesMesmoComNavegadorEmIngles() throws Exception {
		this.mockMvc
			.perform(post("/api/clientes").with(user("ana"))
				.header("Accept-Language", "en-US")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"codigo\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.campos.codigo").value("não deve estar em branco"));
	}

	@Test
	void matriculaOpcionalEUnicaEntreConsultores() throws Exception {
		this.mockMvc
			.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
					{"nome":"Helena","email":"helena@exemplo.com","senha":"segredo123","consultor":"Helena Dias",
					 "matricula":" ct-777 "}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.matricula").value("CT-777"));

		this.mockMvc
			.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
					{"nome":"Igor","email":"igor@exemplo.com","senha":"segredo123","consultor":"Igor Melo",
					 "matricula":"CT-777"}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.mensagem").value("Matrícula já pertence a outro consultor: CT-777"));
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
	void loginComSenhaCorretaDevolveTokenEUsuario() throws Exception {
		cadastrar("bia@exemplo.com");
		this.mockMvc
			.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content("""
					{"email":"bia@exemplo.com","senha":"segredo123"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andExpect(jsonPath("$.tipo").value("Bearer"))
			.andExpect(jsonPath("$.expiraEm").exists())
			.andExpect(jsonPath("$.usuario.email").value("bia@exemplo.com"))
			.andExpect(jsonPath("$.usuario.senha").doesNotExist());
	}

	@Test
	void tokenDoLoginLiberaAsRotasProtegidas() throws Exception {
		cadastrar("fabio@exemplo.com");
		String token = JsonPath.read(this.mockMvc
			.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content("""
					{"email":"fabio@exemplo.com","senha":"segredo123"}
					"""))
			.andReturn()
			.getResponse()
			.getContentAsString(), "$.token");

		this.mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk());
	}

	@Test
	void tokenInvalidoOuAdulteradoDevolve401() throws Exception {
		this.mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer nao.e.um-token"))
			.andExpect(status().isUnauthorized());

		// Assinado com outra chave: o cabecalho e o corpo sao validos, a assinatura nao.
		String forjado = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJjdGktYXBpIiwic3ViIjoieEB4LmNvbSIsImV4cCI6NDEwMjQ0NDgwMH0."
				+ "Zm9yamFkbw";
		this.mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + forjado))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void basicAuthNaoEMaisAceito() throws Exception {
		cadastrar("gil@exemplo.com");
		this.mockMvc.perform(get("/api/usuarios").with(httpBasic("gil@exemplo.com", "segredo123")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void loginComSenhaErradaDevolve401() throws Exception {
		cadastrar("caio@exemplo.com");
		this.mockMvc
			.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content("""
					{"email":"caio@exemplo.com","senha":"errada123"}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
	}

	@Test
	void loginComEmailInexistenteDevolve401() throws Exception {
		this.mockMvc
			.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content("""
					{"email":"ninguem@exemplo.com","senha":"segredo123"}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
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
