package com.synthetix.cti.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModeloDominioTests {

	private static Cliente clienteComFaturamento(String valor) {
		Cliente cliente = new Cliente("cti-1", "Cliente", new Consultor("Ana"));
		cliente.setFaturamentoAnual(new BigDecimal(valor));
		return cliente;
	}

	@Test
	void calculaFaixaDeFaturamentoNosLimites() {
		assertThat(clienteComFaturamento("360000").calcularFaixaFaturamento()).isEqualTo(FaixaFaturamento.MICRO);
		assertThat(clienteComFaturamento("360000.01").calcularFaixaFaturamento()).isEqualTo(FaixaFaturamento.PEQUENA);
		assertThat(clienteComFaturamento("4800000").calcularFaixaFaturamento()).isEqualTo(FaixaFaturamento.PEQUENA);
		assertThat(clienteComFaturamento("300000000").calcularFaixaFaturamento()).isEqualTo(FaixaFaturamento.MEDIA);
		assertThat(clienteComFaturamento("300000001").calcularFaixaFaturamento()).isEqualTo(FaixaFaturamento.GRANDE);
	}

	@Test
	void encapsulamentoRejeitaFaturamentoNegativo() {
		Cliente cliente = clienteComFaturamento("10");
		assertThatIllegalArgumentException().isThrownBy(() -> cliente.setFaturamentoAnual(new BigDecimal("-1")));
		assertThat(cliente.getFaturamentoAnual()).isEqualByComparingTo("10");
	}

	@Test
	void trocaDeConsultorAtualizaOsDoisLados() {
		Consultor ana = new Consultor("Ana");
		Consultor bruno = new Consultor("Bruno");
		Cliente cliente = new Cliente("CTI-1", "Cliente", ana);

		cliente.setConsultor(bruno);

		assertThat(ana.getClientes()).isEmpty();
		assertThat(bruno.getClientes()).containsExactly(cliente);
	}

	@Test
	void contratoNasceVinculadoAoCliente() {
		Cliente cliente = new Cliente("CTI-1", "Cliente", new Consultor("Ana"));
		Contrato contrato = cliente.contratar(new Servico("MPLS"), LocalDate.of(2025, 3, 2));

		assertThat(contrato.getCliente()).isSameAs(cliente);
		assertThat(cliente.getContratos()).containsExactly(contrato);
	}

	@Test
	void subclassesDeInsightHerdamDaBase() {
		List<Insight> insights = List.of(new InsightSegmento("Industria", 3, new BigDecimal("900")),
				new InsightFaturamento(FaixaFaturamento.MICRO, 2, new BigDecimal("500")));

		assertThat(insights.get(0).gerarResumo()).startsWith("Segmento Industria");
		assertThat(insights.get(1).gerarResumo()).startsWith("Microempresa");
	}

}
