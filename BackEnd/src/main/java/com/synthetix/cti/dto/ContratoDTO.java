package com.synthetix.cti.dto;

import com.synthetix.cti.model.Contrato;
import java.time.LocalDate;

public record ContratoDTO(Long id, Long clienteId, String clienteCodigo, String servico, LocalDate dataContratacao) {

	public static ContratoDTO de(Contrato contrato) {
		return new ContratoDTO(contrato.getId(), contrato.getCliente().getId(), contrato.getCliente().getCodigo(),
				contrato.getServico().getNome(), contrato.getDataContratacao());
	}

}
