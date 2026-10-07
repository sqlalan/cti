package br.com.cti.insights.model;

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
	void contratoNasceAtivoECancelarSoDesativa() {
		Cliente cliente = new Cliente("CTI-1", "Cliente", new Consultor("Ana"));
		Contrato contrato = cliente.contratar(new Servico("MPLS"), LocalDate.of(2025, 3, 2));
		assertThat(contrato.getAtivo()).isTrue();
		assertThat(contrato.getDataInicio()).isEqualTo(LocalDate.of(2025, 3, 2));

		contrato.cancelar();

		assertThat(contrato.getAtivo()).isFalse();
		assertThat(cliente.getContratos()).containsExactly(contrato);
	}

	@Test
	void categoriaDoServicoVemDoNome() {
		assertThat(new Servico("Internet Dedicada").getCategoria()).isEqualTo("Conectividade");
		assertThat(new Servico("  mpls ").getCategoria()).isEqualTo("Conectividade");
		assertThat(new Servico("Firewall").getCategoria()).isEqualTo("Segurança");
		assertThat(new Servico("Cloud").getCategoria()).isEqualTo("Nuvem");
		assertThat(new Servico("Telefonia IP").getCategoria()).isEqualTo("Outros");
	}

	@Test
	void insightTemTipoEDescricao() {
		Insight insight = new InsightSegmento("Industria", 3, new BigDecimal("900"));
		assertThat(insight.getTipo()).isEqualTo("SEGMENTO");
		insight.setDescricao("Texto");
		assertThat(insight.getDescricao()).isEqualTo("Texto");
	}

	@Test
	void matriculaEmBrancoViraNula() {
		Consultor consultor = new Consultor("Ana");
		consultor.setMatricula("  ");
		assertThat(consultor.getMatricula()).isNull();
		consultor.setMatricula(" ct-001 ");
		assertThat(consultor.getMatricula()).isEqualTo("CT-001");
	}

	@Test
	void subclassesDeInsightHerdamDaBase() {
		List<Insight> insights = List.of(new InsightSegmento("Industria", 3, new BigDecimal("900")),
				new InsightFaturamento(FaixaFaturamento.MICRO, 2, new BigDecimal("500")));

		assertThat(insights.get(0).gerarResumo()).startsWith("Segmento Industria");
		assertThat(insights.get(1).gerarResumo()).startsWith("Microempresa");
	}

}
