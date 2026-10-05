package com.synthetix.cti.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@DiscriminatorValue("FATURAMENTO")
public class InsightFaturamento extends Insight {

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private FaixaFaturamento faixa;

	@Column(name = "quantidade_clientes")
	private Integer quantidadeClientes;

	@Column(name = "faturamento_total", precision = 17, scale = 2)
	private BigDecimal faturamentoTotal;

	protected InsightFaturamento() {
	}

	public InsightFaturamento(FaixaFaturamento faixa, int quantidadeClientes, BigDecimal faturamentoTotal) {
		this.faixa = faixa;
		this.quantidadeClientes = quantidadeClientes;
		this.faturamentoTotal = faturamentoTotal;
	}

	@Override
	public String gerarResumo() {
		return faixa.getDescricao() + ": " + quantidadeClientes + " cliente(s) somando R$ "
				+ faturamentoTotal.toPlainString() + " por ano";
	}

	@Override
	public String getCategoria() {
		return "FATURAMENTO";
	}

	@Override
	public String getTitulo() {
		return faixa.getDescricao();
	}

	public FaixaFaturamento getFaixa() {
		return faixa;
	}

	public Integer getQuantidadeClientes() {
		return quantidadeClientes;
	}

	public BigDecimal getFaturamentoTotal() {
		return faturamentoTotal;
	}

}
