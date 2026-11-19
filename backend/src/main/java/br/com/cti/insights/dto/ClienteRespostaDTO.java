package br.com.cti.insights.dto;

import br.com.cti.insights.model.Cliente;
import br.com.cti.insights.model.FaixaFaturamento;
import br.com.cti.insights.model.NivelCliente;
import java.math.BigDecimal;

/**
 * O que a API devolve sobre um cliente (CRUD de /api/clientes): a entidade nunca sai direto.
 */
public record ClienteRespostaDTO(Long id, String codigo, String nome, String consultor, String segmento, NivelCliente nivel,
		BigDecimal faturamentoAnual, FaixaFaturamento faixaFaturamento, String cidade, String uf) {

	public static ClienteRespostaDTO de(Cliente cliente) {
		return new ClienteRespostaDTO(cliente.getId(), cliente.getCodigo(), cliente.getNome(), cliente.getConsultor().getNome(),
				cliente.getSegmento().getNome(), cliente.getNivel(), cliente.getFaturamentoAnual(),
				cliente.calcularFaixaFaturamento(), cliente.getCidade(), cliente.getUf());
	}

}
