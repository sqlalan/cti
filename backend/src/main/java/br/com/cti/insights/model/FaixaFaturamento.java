package br.com.cti.insights.model;

import java.math.BigDecimal;

/**
 * Porte da empresa pela receita bruta anual (limites do Simples Nacional e do BNDES).
 */
public enum FaixaFaturamento {

	MICRO("Microempresa", new BigDecimal("360000")),

	PEQUENA("Pequena empresa", new BigDecimal("4800000")),

	MEDIA("Media empresa", new BigDecimal("300000000")),

	GRANDE("Grande empresa", null);

	private final String descricao;

	private final BigDecimal limiteSuperior;

	FaixaFaturamento(String descricao, BigDecimal limiteSuperior) {
		this.descricao = descricao;
		this.limiteSuperior = limiteSuperior;
	}

	public String getDescricao() {
		return descricao;
	}

	static FaixaFaturamento para(BigDecimal faturamentoAnual) {
		for (FaixaFaturamento faixa : values()) {
			if (faixa.limiteSuperior == null || faturamentoAnual.compareTo(faixa.limiteSuperior) <= 0) {
				return faixa;
			}
		}
		return GRANDE;
	}

}
