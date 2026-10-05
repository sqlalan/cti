package com.synthetix.cti.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Resultado do processamento de uma planilha: contagens, indicadores calculados pelo Python e os
 * insights gerados. Cada upload cria uma; o dashboard mostra a mais recente.
 */
@Entity
@Table(name = "analises")
public class Analise {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String arquivo;

	@Column(name = "gerado_em", nullable = false, updatable = false)
	private Instant geradoEm;

	@Column(name = "linhas_lidas", nullable = false)
	private int linhasLidas;

	@Column(name = "linhas_validas", nullable = false)
	private int linhasValidas;

	@Column(name = "linhas_rejeitadas", nullable = false)
	private int linhasRejeitadas;

	@Column(nullable = false)
	private int avisos;

	@Column(name = "clientes_criados", nullable = false)
	private int clientesCriados;

	@Column(name = "clientes_atualizados", nullable = false)
	private int clientesAtualizados;

	// indicadores.json do Python, guardado inteiro: o dashboard le direto, sem remontar no Java.
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private String indicadores;

	// Linhas recusadas e avisos de correcao (problemas.json).
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private String problemas;

	// Analise 1 -- N Insight
	@OneToMany(mappedBy = "analise", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<Insight> insights = new ArrayList<>();

	protected Analise() {
	}

	public Analise(String arquivo, int linhasLidas, int linhasValidas, int linhasRejeitadas, int avisos,
			String indicadores, String problemas) {
		this.arquivo = arquivo;
		this.linhasLidas = linhasLidas;
		this.linhasValidas = linhasValidas;
		this.linhasRejeitadas = linhasRejeitadas;
		this.avisos = avisos;
		this.indicadores = indicadores;
		this.problemas = problemas;
	}

	@PrePersist
	void aoGerar() {
		if (geradoEm == null) {
			geradoEm = Instant.now();
		}
	}

	public void adicionarInsight(Insight insight) {
		insight.setAnalise(this);
		insights.add(insight);
	}

	public void registrarImportacao(int criados, int atualizados) {
		this.clientesCriados = criados;
		this.clientesAtualizados = atualizados;
	}

	public Long getId() {
		return id;
	}

	public String getArquivo() {
		return arquivo;
	}

	public Instant getGeradoEm() {
		return geradoEm;
	}

	public int getLinhasLidas() {
		return linhasLidas;
	}

	public int getLinhasValidas() {
		return linhasValidas;
	}

	public int getLinhasRejeitadas() {
		return linhasRejeitadas;
	}

	public int getAvisos() {
		return avisos;
	}

	public int getClientesCriados() {
		return clientesCriados;
	}

	public int getClientesAtualizados() {
		return clientesAtualizados;
	}

	public String getIndicadores() {
		return indicadores;
	}

	public String getProblemas() {
		return problemas;
	}

	public List<Insight> getInsights() {
		return Collections.unmodifiableList(insights);
	}

}
