package br.com.cti.insights.repository;

import br.com.cti.insights.model.Contrato;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {

	List<Contrato> findByClienteId(Long clienteId);

}
