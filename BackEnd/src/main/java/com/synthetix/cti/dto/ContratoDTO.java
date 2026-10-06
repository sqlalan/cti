package com.synthetix.cti.dto;

import com.synthetix.cti.model.CategoriaServico;
import com.synthetix.cti.model.Contrato;
import com.synthetix.cti.model.StatusContrato;
import java.time.LocalDate;

public record ContratoDTO(Long id, Long clienteId, String clienteCodigo, String servico, CategoriaServico categoria,
		LocalDate dataContratacao, StatusContrato status) {

	public static ContratoDTO de(Contrato contrato) {
		return new ContratoDTO(contrato.getId(), contrato.getCliente().getId(), contrato.getCliente().getCodigo(),
				contrato.getServico().getNome(), contrato.getServico().getCategoria(), contrato.getDataContratacao(),
				contrato.getStatus());
	}

}
