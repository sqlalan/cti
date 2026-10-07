package br.com.cti.insights.model;

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

	public InsightFaturamento() {
		super("FATURAMENTO");
	}

	public InsightFaturamento(FaixaFaturamento faixa, int quantidadeClientes, BigDecimal faturamentoTotal) {
		super("FATURAMENTO");
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
	public String getTitulo() {
		return faixa.getDescricao();
	}

	public FaixaFaturamento getFaixa() {
		return faixa;
	}

	public void setFaixa(FaixaFaturamento faixa) {
		this.faixa = faixa;
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
