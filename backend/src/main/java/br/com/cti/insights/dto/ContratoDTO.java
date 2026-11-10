package br.com.cti.insights.dto;

import br.com.cti.insights.model.Contrato;
import java.time.LocalDate;

public record ContratoDTO(Long id, Long clienteId, String clienteCodigo, String servico, String categoria,
		LocalDate dataInicio, Boolean ativo) {

	public static ContratoDTO de(Contrato contrato) {
		return new ContratoDTO(contrato.getId(), contrato.getCliente().getId(), contrato.getCliente().getCodigo(),
				contrato.getServico().getNome(), contrato.getServico().getCategoria(), contrato.getDataInicio(),
				contrato.getAtivo());
	}

}
