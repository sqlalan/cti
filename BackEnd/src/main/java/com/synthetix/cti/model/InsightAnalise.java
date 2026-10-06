package com.synthetix.cti.model;

import jakarta.persistence.*;

/**
 * Insight em texto produzido pelo script Python (insights.json). A categoria (SEGMENTO, SERVICO,
 * QUALIDADE...) vem do proprio script.
 */
@Entity
@DiscriminatorValue("ANALISE")
public class InsightAnalise extends Insight {

	@Column(length = 30)
	private String categoria;

	@Column(length = 120)
	private String titulo;

	@Column(length = 1000)
	private String texto;

	protected InsightAnalise() {
	}

	public InsightAnalise(String categoria, String titulo, String texto) {
		this.categoria = categoria;
		this.titulo = titulo;
		this.texto = texto;
	}

	@Override
	public String getCategoria() {
		return categoria;
	}

	@Override
	public String getTitulo() {
		return titulo;
	}

	@Override
	public String getOrigem() {
		return "ANALISE";
	}

	@Override
	public String gerarResumo() {
		return texto;
	}

}
