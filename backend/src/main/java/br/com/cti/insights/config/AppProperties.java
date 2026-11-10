package br.com.cti.insights.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracoes "app.*" do application.properties. etl.url e etl.key vem de ETL_URL e ETL_KEY;
 * admin.email e admin.senha, de ADMIN_EMAIL e ADMIN_PASSWORD.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Etl etl, Admin admin) {

	public record Etl(String url, String key) {
	}

	public record Admin(String email, String senha) {
	}

}
