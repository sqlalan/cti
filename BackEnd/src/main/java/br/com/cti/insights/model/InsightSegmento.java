package br.com.cti.insights.model;

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

	public InsightSegmento() {
		super("SEGMENTO");
	}

	public InsightSegmento(String segmento, int quantidadeClientes, BigDecimal faturamentoTotal) {
		super("SEGMENTO");
		this.segmento = segmento;
		this.quantidadeClientes = quantidadeClientes;
		this.faturamentoTotal = faturamentoTotal;
	}

	@Override
	public String gerarResumo() {
		return "Segmento " + segmento + ": " + quantidadeClientes + " cliente(s), faturamento total de R$ "
				+ faturamentoTotal.toPlainString();
	}

	@Override
	public String getTitulo() {
		return "Segmento " + segmento;
	}

	public String getSegmento() {
		return segmento;
	}

	public void setSegmento(String segmento) {
		this.segmento = segmento;
	}

	public Integer getQuantidadeClientes() {
		return quantidadeClientes;
	}

	public void setQuantidadeClientes(Integer quantidadeClientes) {
		this.quantidadeClientes = quantidadeClientes;
	}

	public BigDecimal getFaturamentoTotal() {
		return faturamentoTotal;
	}

	public void setFaturamentoTotal(BigDecimal faturamentoTotal) {
		this.faturamentoTotal = faturamentoTotal;
	}

}
