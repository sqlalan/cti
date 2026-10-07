package br.com.cti.insights;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Mantem database/schema.sql igual as entidades JPA. O Hibernate escreve o DDL no dialeto do
 * PostgreSQL (o banco de producao) sem executar nada.
 *
 * Mudou uma entidade? Regere com: ./mvnw test -Dtest=SchemaSqlTests -DatualizarSchema=true
 */
@SpringBootTest(properties = { "spring.jpa.hibernate.ddl-auto=none",
		"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
		"spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create",
		"spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=target/schema-gerado.sql",
		"spring.jpa.properties.hibernate.hbm2ddl.delimiter=;", "spring.jpa.properties.hibernate.format_sql=true" })
class SchemaSqlTests {

	private static final Path GERADO = Path.of("target/schema-gerado.sql");

	private static final Path VERSIONADO = Path.of("../database/schema.sql");

	private static final String CABECALHO = """
			-- CTI Insights: estrutura do banco (PostgreSQL).
			-- Gerado a partir das entidades JPA por SchemaSqlTests; nao edite a mao.
			-- Para regerar: cd BackEnd && ./mvnw test -Dtest=SchemaSqlTests -DatualizarSchema=true
			""";

	// O Hibernate acrescenta ao arquivo em vez de sobrescrever.
	@BeforeAll
	static void apagarGerado() throws IOException {
		Files.deleteIfExists(GERADO);
	}

	@Test
	void schemaSqlEstaIgualAsEntidades() throws IOException {
		// O format_sql recua tudo em 4 espacos menos a primeira linha; tira o recuo para alinhar.
		String ddl = Files.readString(GERADO, StandardCharsets.UTF_8)
			.lines()
			.map(linha -> linha.startsWith("    ") ? linha.substring(4) : linha)
			.map(String::stripTrailing)
			.collect(Collectors.joining("\n"))
			.strip();
		String esperado = CABECALHO + "\n" + ddl + "\n";

		if (Boolean.getBoolean("atualizarSchema")) {
			Files.createDirectories(VERSIONADO.getParent());
			Files.writeString(VERSIONADO, esperado, StandardCharsets.UTF_8);
		}

		assertThat(Files.exists(VERSIONADO)).as("database/schema.sql nao existe; gere com -DatualizarSchema=true")
			.isTrue();
		assertThat(Files.readString(VERSIONADO, StandardCharsets.UTF_8).replace("\r\n", "\n"))
			.as("database/schema.sql desatualizado; regere com -DatualizarSchema=true")
			.isEqualTo(esperado);
	}

}
