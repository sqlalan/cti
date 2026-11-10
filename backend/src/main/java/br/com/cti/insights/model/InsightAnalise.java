package br.com.cti.insights.model;

import jakarta.persistence.*;

/**
 * Insight em texto produzido pelo script Python (insights.json). O tipo (SEGMENTO, SERVICO,
 * QUALIDADE...) e o texto vem do proprio script.
 */
@Entity
@DiscriminatorValue("ANALISE")
public class InsightAnalise extends Insight {

	@Column(length = 120)
	private String titulo;

	public InsightAnalise() {
	}

	public InsightAnalise(String tipo, String titulo, String descricao) {
		super(tipo);
		this.titulo = titulo;
		setDescricao(descricao);
	}

	@Override
	public String getOrigem() {
		return "ANALISE";
	}

	@Override
	public String gerarResumo() {
		return getDescricao();
	}

	@Override
	public String getTitulo() {
		return titulo;
	}

	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

}
