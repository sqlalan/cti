package com.synthetix.cti.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@DiscriminatorValue("SEGMENTO")
public class InsightSegmento extends Insight {

	@Column(length = 60)
	private String segmento;

	@Column(name = "quantidade_clientes")
	private Integer quantidadeClientes;

	@Column(name = "faturamento_total", precision = 17, scale = 2)
	private BigDecimal faturamentoTotal;

	protected InsightSegmento() {
	}

	public InsightSegmento(String segmento, int quantidadeClientes, BigDecimal faturamentoTotal) {
		this.segmento = segmento;
		this.quantidadeClientes = quantidadeClientes;
		this.faturamentoTotal = faturamentoTotal;
	}

	@Override
	public String gerarResumo() {
		return "Segmento " + segmento + ": " + quantidadeClientes + " cliente(s), faturamento total de R$ "
				+ faturamentoTotal.toPlainString();
	}

	public String getSegmento() {
		return segmento;
	}

	public Integer getQuantidadeClientes() {
		return quantidadeClientes;
	}

	public BigDecimal getFaturamentoTotal() {
		return faturamentoTotal;
	}

}
