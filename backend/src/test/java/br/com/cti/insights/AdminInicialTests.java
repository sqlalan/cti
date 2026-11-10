package br.com.cti.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cti.insights.model.Perfil;
import br.com.cti.insights.model.StatusUsuario;
import br.com.cti.insights.model.Usuario;
import br.com.cti.insights.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ADMIN_EMAIL e ADMIN_PASSWORD criam o primeiro administrador ao subir, ja aprovado.
 */
@SpringBootTest(properties = { "app.admin.email=Chefe@CTI.com", "app.admin.senha=chefe12345" })
@AutoConfigureMockMvc
class AdminInicialTests {

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void criaOAdministradorAprovadoQueConsegueEntrar() throws Exception {
		Usuario admin = usuarios.findByEmailIgnoreCase("chefe@cti.com").orElseThrow();
		assertThat(admin.getPerfil()).isEqualTo(Perfil.ADMIN);
		assertThat(admin.getStatus()).isEqualTo(StatusUsuario.APROVADO);

		mockMvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"chefe@cti.com\",\"senha\":\"chefe12345\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.usuario.perfil").value("ADMIN"));
	}

}
