package com.synthetix.cti.repository;

import com.synthetix.cti.model.Contrato;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {

	List<Contrato> findByClienteId(Long clienteId);

}
