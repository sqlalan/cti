package br.com.cti.insights.repository;

import br.com.cti.insights.model.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

	Optional<Cliente> findByCodigo(String codigo);

	boolean existsByCodigo(String codigo);

}
