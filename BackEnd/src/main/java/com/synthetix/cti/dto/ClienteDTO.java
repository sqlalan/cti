package com.synthetix.cti.dto;

import com.synthetix.cti.model.Cliente;
import com.synthetix.cti.model.FaixaFaturamento;
import com.synthetix.cti.model.NivelCliente;
import java.math.BigDecimal;

public record ClienteDTO(Long id, String codigo, String nome, String consultor, String segmento, NivelCliente nivel,
		BigDecimal faturamentoAnual, FaixaFaturamento faixaFaturamento, String cidade, String uf) {

	public static ClienteDTO de(Cliente cliente) {
		return new ClienteDTO(cliente.getId(), cliente.getCodigo(), cliente.getNome(), cliente.getConsultor().getNome(),
				cliente.getSegmento(), cliente.getNivel(), cliente.getFaturamentoAnual(),
				cliente.calcularFaixaFaturamento(), cliente.getCidade(), cliente.getUf());
	}

}
