package br.com.cti.insights.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracoes "app.*" do application.properties. etl.url e etl.key vem de ETL_URL e ETL_KEY.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Etl etl) {

	public record Etl(String url, String key) {
	}

}
