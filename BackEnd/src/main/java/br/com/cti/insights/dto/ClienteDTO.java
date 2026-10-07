package br.com.cti.insights.dto;

import br.com.cti.insights.model.Cliente;
import br.com.cti.insights.model.FaixaFaturamento;
import br.com.cti.insights.model.NivelCliente;
import java.math.BigDecimal;

public record ClienteDTO(Long id, String codigo, String nome, String consultor, String segmento, NivelCliente nivel,
		BigDecimal faturamentoAnual, FaixaFaturamento faixaFaturamento, String cidade, String uf) {

	public static ClienteDTO de(Cliente cliente) {
		return new ClienteDTO(cliente.getId(), cliente.getCodigo(), cliente.getNome(), cliente.getConsultor().getNome(),
				cliente.getSegmento(), cliente.getNivel(), cliente.getFaturamentoAnual(),
				cliente.calcularFaixaFaturamento(), cliente.getCidade(), cliente.getUf());
	}

}
